package com.orbitflux.core.engine

import com.badlogic.gdx.math.MathUtils
import com.orbitflux.core.math.AngleMath
import kotlin.math.pow

class GameSimulation(
    val levels: List<LevelConfig>,
    private val campaignSeed: Long
) {
    enum class DeathCause {
        WALL,
        MISSILE,
        CORE
    }

    enum class MissileSource {
        WAR_VOLLEY,
        ENEMY_LASER,
        KITCHEN_KNIFE
    }

    data class PlayerInput(
        val holdDirection: Int = 0,
        val stepDirection: Int = 0
    )

    private data class SpawnReadabilityRules(
        val minRadialGapBetweenRings: Float,
        val targetRadialGapBetweenRings: Float,
        val minReadableThreatGapSeconds: Float
    )

    private data class ThreatSnapshot(
        val arrivalSeconds: Float,
        val predictedGapCenterAngleRad: Float,
        val halfGapAngleRad: Float,
        val occupancySeconds: Float
    )

    data class MissileHazard(
        val angleRad: Float,
        var radius: Float,
        val speed: Float,
        val halfArcWidthRad: Float,
        val radialTolerance: Float,
        val source: MissileSource = MissileSource.WAR_VOLLEY,
        val emitterVariant: Int = 0
    )

    companion object {
        const val FIXED_TIMESTEP_SECONDS: Float = 1f / 60f
        private const val SPAWN_RADIUS: Float = 2.95f
        const val BLACK_HOLE_VISUAL_RADIUS: Float = 0.165f
        const val BLACK_HOLE_ABSORB_RADIUS: Float = 0.205f
        private const val ABSOLUTE_MIN_RING_GAP: Float = 0.26f
        private const val ABSOLUTE_TARGET_RING_GAP: Float = 0.34f
        private const val ABSOLUTE_MIN_READABLE_SECONDS: Float = 0.4f
        private const val TIME_BUBBLE_SECONDS: Float = 1.5f
        private const val TIME_BUBBLE_TIME_SCALE: Float = 0.56f

        val PLAYER_COLLIDER = CollisionEngine.PlayerCollider(
            radius = 0.66f,
            halfArcWidthRad = 0.041f,
            radialTolerance = 0.027f
        )
    }

    private val mutableObstacles = ArrayList<Obstacle>(64)
    private val mutableMissiles = ArrayList<MissileHazard>(24)
    private var patternGenerator: PatternGenerator = PatternGenerator(levels.first(), campaignSeed)
    private var phaseRng: SeededRng = SeededRng(campaignSeed)
    private var spawnTimer: Float = 0f
    private var runClockSeconds: Float = 0f
    private var attemptNumberForLevel: Int = 0
    private var spawningEnabled: Boolean = false
    private var directionMultiplier: Int = 1
    private var phaseActiveRemainingSeconds: Float = 0f
    private var phaseCooldownRemainingSeconds: Float = 0f
    private var arenaSpinTargetRadPerSecond: Float = 0f
    private var arenaSpinShiftRemainingSeconds: Float = 0f
    private var discreteLaneIndex: Int = 0
    private var playerTargetAngleRad: Float = 0f
    private var pendingSpawnPlan: PatternGenerator.SpawnPlan? = null
    private var missileSpawnTimerSeconds: Float = Float.POSITIVE_INFINITY
    private var missileWaveCounter: Int = 0
    private var playerOrbitRadius: Float = PLAYER_COLLIDER.radius
    private var playerHitCircleRadius: Float = PLAYER_COLLIDER.radialTolerance
    private var runShieldHitsRemaining: Int = 0
    private var shieldBreakCount: Int = 0
    private var timeBubbleRemainingSeconds: Float = 0f
    private var manualSlowRemainingSeconds: Float = 0f
    private var reviveShieldRemainingSeconds: Float = 0f
    private var userDifficultyMultiplier: Float = 1f
    private var userReadabilityMultiplier: Float = 1f
    private var smoothedDifficultyMultiplier: Float = 1f
    private var adaptiveAssistEnabled: Boolean = true
    private var failureStreakForLevel: Int = 0

    var levelIndex: Int = 0
        private set

    var currentRunSeed: Long = 0L
        private set

    var runPhase: RunPhase = RunPhase.READY
        private set

    var playerAngleRad: Float = 0f
        private set

    var survivedSeconds: Float = 0f
        private set

    var arenaRotationRad: Float = 0f
        private set

    var arenaSpinRadPerSecond: Float = 0f
        private set

    var phaseGateStatus: PhaseGateStatus = PhaseGateStatus(
        active = false,
        directionMultiplier = 1,
        secondsToStateChange = 0f
    )
        private set

    val obstacles: List<Obstacle>
        get() = mutableObstacles

    val missiles: List<MissileHazard>
        get() = mutableMissiles

    val levelConfig: LevelConfig
        get() = levels[levelIndex]

    val runIntensity: Float
        get() = (survivedSeconds / levelConfig.targetSurvivalSeconds).coerceIn(0f, 1f)

    val elapsedRunSeconds: Float
        get() = runClockSeconds

    val usesStepMovement: Boolean
        get() = false

    val hasReversePhaseGate: Boolean
        get() = false

    val shieldActive: Boolean
        get() = runShieldHitsRemaining > 0

    val shieldBreakCounter: Int
        get() = shieldBreakCount

    val timeBubbleActive: Boolean
        get() = timeBubbleRemainingSeconds > 0f

    val manualSlowActive: Boolean
        get() = manualSlowRemainingSeconds > 0f

    var lastDeathCause: DeathCause = DeathCause.WALL
        private set

    val activeLethalThreatCount: Int
        get() {
            val collider = activePlayerCollider()
            var count = 0
            for (obstacle in mutableObstacles) {
                if (isObstacleStillLethal(obstacle, collider)) {
                    count += 1
                }
            }
            return count
        }

    val hasPendingThreats: Boolean
        get() = mutableObstacles.isNotEmpty() || mutableMissiles.isNotEmpty() || pendingSpawnPlan != null

    val playerOrbitRadiusNormalized: Float
        get() = playerOrbitRadius

    val adaptiveAssistIntensity: Float
        get() = if (adaptiveAssistEnabled) {
            val divisor = if (levelConfig.index in 61..100) 2.2f else 4f
            (failureStreakForLevel.toFloat() / divisor).coerceIn(0f, 1f)
        } else {
            0f
        }

    fun setDifficultyTuning(
        gameplayMultiplier: Float,
        readabilityMultiplier: Float,
        adaptiveAssist: Boolean = true
    ) {
        userDifficultyMultiplier = gameplayMultiplier.coerceIn(0.72f, 1.32f)
        userReadabilityMultiplier = readabilityMultiplier.coerceIn(0.78f, 1.28f)
        adaptiveAssistEnabled = adaptiveAssist
        smoothedDifficultyMultiplier = difficultyMultiplier()
    }

    fun registerRunOutcome(levelCleared: Boolean) {
        if (levelCleared) {
            failureStreakForLevel = 0
        } else if (adaptiveAssistEnabled) {
            val penaltyStep = if (levelConfig.index in 61..100) 3 else 1
            val maxStreak = if (levelConfig.index in 61..100) 9 else 6
            failureStreakForLevel = (failureStreakForLevel + penaltyStep).coerceAtMost(maxStreak)
        }
    }

    fun startRun() {
        currentRunSeed = computeRunSeed(levelIndex, attemptNumberForLevel)
        attemptNumberForLevel += 1

        patternGenerator = PatternGenerator(levelConfig, currentRunSeed)
        phaseRng = SeededRng(currentRunSeed xor 0x243F6A8885A308D3UL.toLong())
        mutableObstacles.clear()
        spawnTimer = 0.08f
        runClockSeconds = 0f
        spawningEnabled = true
        survivedSeconds = 0f
        arenaRotationRad = 0f
        arenaSpinTargetRadPerSecond = nextArenaSpinTargetRadPerSecond()
        arenaSpinRadPerSecond = arenaSpinTargetRadPerSecond
        arenaSpinShiftRemainingSeconds = nextArenaSpinShiftSeconds()
        discreteLaneIndex = 0
        playerOrbitRadius = PLAYER_COLLIDER.radius
        playerAngleRad = playerAngleForCurrentMode()
        playerTargetAngleRad = playerAngleRad
        directionMultiplier = 1
        phaseActiveRemainingSeconds = 0f
        phaseCooldownRemainingSeconds = nextPhaseCooldownSeconds()
        phaseGateStatus = PhaseGateStatus(
            active = false,
            directionMultiplier = directionMultiplier,
            secondsToStateChange = phaseCooldownRemainingSeconds
        )
        pendingSpawnPlan = null
        mutableMissiles.clear()
        missileWaveCounter = 0
        missileSpawnTimerSeconds = if (hasMissileHazards()) nextMissileWaveSeconds() else Float.POSITIVE_INFINITY
        runShieldHitsRemaining = 0
        timeBubbleRemainingSeconds = 0f
        manualSlowRemainingSeconds = 0f
        reviveShieldRemainingSeconds = 0f
        lastDeathCause = DeathCause.WALL
        smoothedDifficultyMultiplier = difficultyMultiplier()
        runPhase = RunPhase.RUNNING
    }

    fun step(deltaSeconds: Float, rotationInput: Int) {
        step(deltaSeconds, PlayerInput(holdDirection = rotationInput))
    }

    fun step(deltaSeconds: Float, playerInput: PlayerInput) {
        if (runPhase != RunPhase.RUNNING && runPhase != RunPhase.DRAINING) {
            return
        }

        updateFeatureTimers(deltaSeconds)
        val simulationDelta = deltaSeconds * if (timeBubbleActive) TIME_BUBBLE_TIME_SCALE else 1f
        val threatDelta = simulationDelta * if (manualSlowActive) 0.42f else 1f
        reviveShieldRemainingSeconds = (reviveShieldRemainingSeconds - simulationDelta).coerceAtLeast(0f)

        updatePhaseGate(simulationDelta)
        updateArenaMotion(threatDelta)
        updatePlayerMotion(simulationDelta, playerInput)
        relaxOrbitRadius(simulationDelta)
        val targetDifficultyMultiplier = difficultyMultiplier()
        val effectiveDifficultyMultiplier = smoothDifficultyMultiplier(
            target = targetDifficultyMultiplier,
            deltaSeconds = simulationDelta
        )
        val readabilityRules = spawnReadabilityRules(effectiveDifficultyMultiplier)
        var playerCollider = activePlayerCollider()
        runClockSeconds += simulationDelta

        val reachesTargetThisStep = runPhase == RunPhase.RUNNING &&
            survivedSeconds + simulationDelta >= levelConfig.targetSurvivalSeconds
        if (reachesTargetThisStep) {
            spawningEnabled = false
            pendingSpawnPlan = null
        }

        if (spawningEnabled && runPhase == RunPhase.RUNNING) {
            spawnTimer -= threatDelta
            var spawnSafetyCount = 0
            val minSpawnInterval = minimumSpawnIntervalSeconds()
            while (spawnTimer <= 0f && spawnSafetyCount < 2) {
                val spawn = pendingSpawnPlan ?: patternGenerator
                    .nextSpawn(SPAWN_RADIUS, effectiveDifficultyMultiplier)
                    .also { pendingSpawnPlan = it }
                val delay = spawnDelayForReadability(spawn.obstacle, readabilityRules, playerCollider)
                if (delay > 0f) {
                    spawnTimer += delay
                    break
                }

                mutableObstacles.add(spawn.obstacle)
                pendingSpawnPlan = null
                spawnTimer += maxOf(spawn.intervalSeconds, minSpawnInterval)
                spawnSafetyCount += 1
            }
            if (spawnTimer <= 0f) {
                spawnTimer = minSpawnInterval * 0.5f
            }
        }

        if (runPhase == RunPhase.RUNNING && hasMissileHazards()) {
            missileSpawnTimerSeconds -= threatDelta
            var missileWaveSafety = 0
            while (missileSpawnTimerSeconds <= 0f && missileWaveSafety < 2) {
                spawnMissileWave()
                missileSpawnTimerSeconds += nextMissileWaveSeconds()
                missileWaveSafety += 1
            }
            if (missileSpawnTimerSeconds <= 0f) {
                missileSpawnTimerSeconds = 0.32f
            }
        }

        for (index in mutableObstacles.lastIndex downTo 0) {
            val obstacle = mutableObstacles[index]
            obstacle.radius -= obstacle.speed * threatDelta
            obstacle.rotationRad = AngleMath.normalizeRadians(
                obstacle.rotationRad + obstacle.spinRadPerSecond * threatDelta
            )
            val absorbedByBlackHole = obstacle.radius - obstacle.thickness <= BLACK_HOLE_ABSORB_RADIUS
            if (absorbedByBlackHole) {
                mutableObstacles.removeAt(index)
            }
        }

        for (index in mutableMissiles.lastIndex downTo 0) {
            val missile = mutableMissiles[index]
            missile.radius -= missile.speed * threatDelta
            if (missile.radius <= BLACK_HOLE_ABSORB_RADIUS) {
                mutableMissiles.removeAt(index)
            }
        }

        val localPlayerAngle = AngleMath.normalizeRadians(playerAngleRad - arenaRotationRad)
        for (index in mutableObstacles.lastIndex downTo 0) {
            val obstacle = mutableObstacles[index]
            if (reviveShieldRemainingSeconds > 0f) {
                continue
            }
            if (!CollisionEngine.collides(localPlayerAngle, playerCollider, obstacle)) {
                continue
            }
            if (obstacle.patternId == "gravity_pull") {
                applyGravityWallImpact(obstacle)
                playerCollider = activePlayerCollider()
                continue
            }
            if (obstacle.patternId == "time_bubble") {
                applyTimeBubbleImpact()
                mutableObstacles.removeAt(index)
                continue
            }
            if (runShieldHitsRemaining > 0) {
                runShieldHitsRemaining = (runShieldHitsRemaining - 1).coerceAtLeast(0)
                shieldBreakCount += 1
                mutableObstacles.removeAt(index)
                continue
            }
            lastDeathCause = DeathCause.WALL
            runPhase = RunPhase.GAME_OVER
            return
        }

        for (index in mutableMissiles.lastIndex downTo 0) {
            val missile = mutableMissiles[index]
            val radialHit = kotlin.math.abs(missile.radius - playerOrbitRadius) <= (playerCollider.radialTolerance + missile.radialTolerance)
            if (!radialHit) {
                continue
            }
            val angularDelta = kotlin.math.abs(shortestAngleDelta(localPlayerAngle, missile.angleRad))
            val angularHit = angularDelta <= (playerCollider.halfArcWidthRad + missile.halfArcWidthRad)
            if (!angularHit) {
                continue
            }
            if (runShieldHitsRemaining > 0) {
                runShieldHitsRemaining = (runShieldHitsRemaining - 1).coerceAtLeast(0)
                shieldBreakCount += 1
                mutableMissiles.removeAt(index)
                continue
            }
            lastDeathCause = DeathCause.MISSILE
            runPhase = RunPhase.GAME_OVER
            return
        }

        val innerTouch = playerOrbitRadius - playerCollider.radialTolerance <= BLACK_HOLE_ABSORB_RADIUS
        if (innerTouch) {
            if (reviveShieldRemainingSeconds > 0f) {
                playerOrbitRadius = (BLACK_HOLE_ABSORB_RADIUS + playerCollider.radialTolerance + 0.01f)
                    .coerceAtMost(PLAYER_COLLIDER.radius)
                return
            }
            lastDeathCause = DeathCause.CORE
            runPhase = RunPhase.GAME_OVER
            return
        }

        if (runPhase == RunPhase.RUNNING) {
            survivedSeconds += simulationDelta
            if (survivedSeconds >= levelConfig.targetSurvivalSeconds) {
                survivedSeconds = levelConfig.targetSurvivalSeconds
                spawningEnabled = false
                pendingSpawnPlan = null
                runPhase = RunPhase.DRAINING
            }
        }

        if (runPhase == RunPhase.DRAINING && !hasPendingThreats) {
            runPhase = RunPhase.LEVEL_CLEARED
        }
    }

    fun resetLevelToReady() {
        mutableObstacles.clear()
        spawnTimer = 0f
        runClockSeconds = 0f
        spawningEnabled = false
        survivedSeconds = 0f
        arenaRotationRad = 0f
        arenaSpinTargetRadPerSecond = 0f
        arenaSpinRadPerSecond = 0f
        arenaSpinShiftRemainingSeconds = 0f
        discreteLaneIndex = 0
        playerOrbitRadius = PLAYER_COLLIDER.radius
        playerAngleRad = playerAngleForCurrentMode()
        playerTargetAngleRad = playerAngleRad
        directionMultiplier = 1
        phaseActiveRemainingSeconds = 0f
        phaseCooldownRemainingSeconds = 0f
        runShieldHitsRemaining = 0
        timeBubbleRemainingSeconds = 0f
        manualSlowRemainingSeconds = 0f
        reviveShieldRemainingSeconds = 0f
        lastDeathCause = DeathCause.WALL
        smoothedDifficultyMultiplier = difficultyMultiplier()
        phaseGateStatus = PhaseGateStatus(
            active = false,
            directionMultiplier = directionMultiplier,
            secondsToStateChange = 0f
        )
        pendingSpawnPlan = null
        mutableMissiles.clear()
        missileWaveCounter = 0
        missileSpawnTimerSeconds = if (hasMissileHazards()) nextMissileWaveSeconds() else Float.POSITIVE_INFINITY
        runPhase = RunPhase.READY
    }

    fun reviveAfterGameOver(): Boolean {
        if (runPhase != RunPhase.GAME_OVER) {
            return false
        }

        val collider = activePlayerCollider()
        val localPlayerAngle = AngleMath.normalizeRadians(playerAngleRad - arenaRotationRad)
        for (index in mutableObstacles.lastIndex downTo 0) {
            val obstacle = mutableObstacles[index]
            val threateningNow = CollisionEngine.collides(localPlayerAngle, collider, obstacle)
            val tooClose = obstacle.radius <= playerOrbitRadius + collider.radialTolerance * 1.8f
            if (threateningNow || tooClose) {
                mutableObstacles.removeAt(index)
            }
        }
        mutableMissiles.clear()

        playerOrbitRadius = PLAYER_COLLIDER.radius
        playerTargetAngleRad = playerAngleRad
        runShieldHitsRemaining = 0
        timeBubbleRemainingSeconds = 0f
        manualSlowRemainingSeconds = 0f
        reviveShieldRemainingSeconds = 1.1f
        lastDeathCause = DeathCause.WALL
        smoothedDifficultyMultiplier = difficultyMultiplier()
        runPhase = if (survivedSeconds >= levelConfig.targetSurvivalSeconds) {
            spawningEnabled = false
            RunPhase.DRAINING
        } else {
            spawningEnabled = true
            RunPhase.RUNNING
        }
        return true
    }

    private fun nextMissileWaveSeconds(): Float {
        if (!hasMissileHazards()) {
            return Float.POSITIVE_INFINITY
        }
        val assistEase = adaptiveAssistIntensity
        return if (levelConfig.index in 61..70) {
            val base = phaseRng.nextFloat(0.96f, 1.72f)
            val intensityTighten = runIntensity * (0.22f - assistEase * 0.08f).coerceAtLeast(0.08f)
            (base - intensityTighten).coerceIn(0.72f, 1.86f)
        } else if (levelConfig.index in 91..100) {
            val base = phaseRng.nextFloat(1.34f, 2.42f)
            val tighten = runIntensity * (0.18f - assistEase * 0.05f).coerceAtLeast(0.07f)
            (base - tighten).coerceIn(1.02f, 2.48f)
        } else {
            val base = phaseRng.nextFloat(1.08f, 2.04f)
            val tighten = runIntensity * (0.2f - assistEase * 0.06f).coerceAtLeast(0.08f)
            (base - tighten).coerceIn(0.76f, 2.12f)
        }
    }

    private fun spawnMissileWave() {
        if (!hasMissileHazards()) {
            return
        }
        if (levelConfig.index in 91..100) {
            spawnKitchenKnifeWave()
            missileWaveCounter += 1
            return
        }
        if (levelConfig.index in 81..90) {
            spawnAmbushLaserWave()
            missileWaveCounter += 1
            return
        }
        spawnWarMissileWave()
        missileWaveCounter += 1
    }

    private fun spawnWarMissileWave() {
        val assistEase = adaptiveAssistIntensity
        val highVolleyWindow = levelConfig.index >= 66
        val baseCount = when {
            highVolleyWindow && runIntensity > 0.74f -> 8
            highVolleyWindow && runIntensity > 0.4f -> 6
            highVolleyWindow -> 4
            runIntensity > 0.74f -> 4
            runIntensity > 0.4f -> 3
            else -> 2
        }
        val assistReduction = if (highVolleyWindow) {
            (assistEase * 1.2f).toInt()
        } else {
            (assistEase * 2f).toInt()
        }
        val minCount = if (highVolleyWindow) 4 else 2
        val maxCount = if (highVolleyWindow) 8 else 4
        val easedCount = (baseCount - assistReduction).coerceIn(minCount, maxCount)
        val count = if (easedCount % 2 == 0) easedCount else (easedCount + 1).coerceAtMost(maxCount)
        val spawnRadius = (SPAWN_RADIUS - 0.02f).coerceAtLeast(1.36f)
        val baseSpeed = (levelConfig.baseObstacleSpeed * 1.9f * (1f - assistEase * 0.16f)).coerceAtLeast(0.42f)
        val missilesPerSide = (count / 2).coerceAtLeast(1)
        val spreadStep = when (missilesPerSide) {
            1 -> 0f
            else -> 0.16f
        }
        val anchorAngles = floatArrayOf(MathUtils.PI * 0.5f, MathUtils.PI * 1.5f)
        for (anchor in anchorAngles) {
            for (index in 0 until missilesPerSide) {
                val spread = if (missilesPerSide == 1) {
                    0f
                } else {
                    (index - (missilesPerSide - 1) * 0.5f) * spreadStep
                }
                val angle = AngleMath.normalizeRadians(anchor + spread)
                mutableMissiles.add(
                    MissileHazard(
                        angleRad = angle,
                        radius = spawnRadius,
                        speed = baseSpeed * phaseRng.nextFloat(0.94f, 1.08f),
                        halfArcWidthRad = phaseRng.nextFloat(0.01f, 0.016f),
                        radialTolerance = phaseRng.nextFloat(0.012f, 0.019f),
                        source = MissileSource.WAR_VOLLEY,
                        emitterVariant = phaseRng.nextInt(11)
                    )
                )
            }
        }
        if (mutableMissiles.size > 32) {
            mutableMissiles.subList(0, mutableMissiles.size - 32).clear()
        }
    }

    private fun spawnAmbushLaserWave() {
        val assistEase = adaptiveAssistIntensity
        val blockProgress = ((levelConfig.index - 81).coerceIn(0, 9)).toFloat() / 9f
        val baseShips = when {
            blockProgress > 0.66f && runIntensity > 0.62f -> 3
            runIntensity > 0.42f -> 3
            else -> 2
        }
        val shipReduction = (assistEase * if (blockProgress > 0.66f) 1.2f else 1f).toInt()
        val shipCount = (baseShips - shipReduction).coerceIn(2, 3)
        val anchors = ambushAnchorPattern(shipCount, blockProgress)
        if (anchors.isEmpty()) {
            return
        }
        val shotsPerShip = if (blockProgress > 0.46f && runIntensity > 0.5f) 2 else 1
        val spawnRadius = (SPAWN_RADIUS - 0.01f).coerceAtLeast(1.4f)
        val baseSpeed = (levelConfig.baseObstacleSpeed * (1.76f + blockProgress * 0.28f) * (1f - assistEase * 0.12f))
            .coerceAtLeast(0.44f)
        val maxNewMissiles = if (blockProgress > 0.72f) 6 else 4
        var spawned = 0
        for (anchor in anchors) {
            val emitterVariant = phaseRng.nextInt(11)
            for (shot in 0 until shotsPerShip) {
                if (spawned >= maxNewMissiles) {
                    break
                }
                val spread = when (shotsPerShip) {
                    1 -> 0f
                    else -> (shot - (shotsPerShip - 1) * 0.5f) * 0.096f
                }
                val jitter = if (blockProgress > 0.72f) phaseRng.nextFloat(-0.048f, 0.048f) else 0f
                val angle = AngleMath.normalizeRadians(anchor + spread + jitter)
                mutableMissiles.add(
                    MissileHazard(
                        angleRad = angle,
                        radius = spawnRadius,
                        speed = baseSpeed * phaseRng.nextFloat(0.94f, 1.1f),
                        halfArcWidthRad = phaseRng.nextFloat(0.008f, 0.014f),
                        radialTolerance = phaseRng.nextFloat(0.01f, 0.017f),
                        source = MissileSource.ENEMY_LASER,
                        emitterVariant = emitterVariant
                    )
                )
                spawned += 1
            }
        }
        if (mutableMissiles.size > 34) {
            mutableMissiles.subList(0, mutableMissiles.size - 34).clear()
        }
    }

    private fun spawnKitchenKnifeWave() {
        val assistEase = adaptiveAssistIntensity
        val blockProgress = ((levelConfig.index - 91).coerceIn(0, 9)).toFloat() / 9f
        val earlyLevel = levelConfig.index <= 94
        val baseCount = when {
            blockProgress > 0.72f && runIntensity > 0.68f -> 4
            runIntensity > 0.48f -> 3
            else -> 2
        }
        val reducedCount = (baseCount - (assistEase * 1.1f).toInt()).coerceIn(2, 4)
        val spawnRadius = (SPAWN_RADIUS - 0.02f).coerceAtLeast(1.42f)
        val baseSpeed = (levelConfig.baseObstacleSpeed * (1.28f + blockProgress * 0.1f) * (1f - assistEase * 0.1f))
            .coerceAtLeast(0.34f)
        val anchors = if (earlyLevel) {
            floatArrayOf(MathUtils.PI * 0.5f, MathUtils.PI * 1.5f)
        } else {
            val patterns = arrayOf(
                floatArrayOf(MathUtils.PI * 0.5f, MathUtils.PI * 1.5f),
                floatArrayOf(0f, MathUtils.PI),
                floatArrayOf(MathUtils.PI * 0.5f, 0f, MathUtils.PI),
                floatArrayOf(MathUtils.PI * 1.5f, 0f, MathUtils.PI)
            )
            patterns[(missileWaveCounter + levelConfig.index).mod(patterns.size)]
        }
        val finalAnchors = if (reducedCount <= anchors.size) anchors.copyOfRange(0, reducedCount) else anchors
        for (anchor in finalAnchors) {
            val jitter = if (levelConfig.index >= 98) phaseRng.nextFloat(-0.034f, 0.034f) else 0f
            val angle = AngleMath.normalizeRadians(anchor + jitter)
            mutableMissiles.add(
                MissileHazard(
                    angleRad = angle,
                    radius = spawnRadius,
                    speed = baseSpeed * phaseRng.nextFloat(0.9f, 1.05f),
                    halfArcWidthRad = phaseRng.nextFloat(0.02f, 0.034f),
                    radialTolerance = phaseRng.nextFloat(0.022f, 0.036f),
                    source = MissileSource.KITCHEN_KNIFE,
                    emitterVariant = phaseRng.nextInt(6)
                )
            )
        }
        if (mutableMissiles.size > 36) {
            mutableMissiles.subList(0, mutableMissiles.size - 36).clear()
        }
    }

    private fun ambushAnchorPattern(shipCount: Int, blockProgress: Float): FloatArray {
        val vertical = floatArrayOf(MathUtils.PI * 0.5f, MathUtils.PI * 1.5f)
        val horizontal = floatArrayOf(0f, MathUtils.PI)
        val mixed = arrayOf(
            floatArrayOf(MathUtils.PI * 0.5f, 0f),
            floatArrayOf(MathUtils.PI * 1.5f, MathUtils.PI),
            floatArrayOf(MathUtils.PI * 0.5f, MathUtils.PI),
            floatArrayOf(MathUtils.PI * 1.5f, 0f),
            floatArrayOf(MathUtils.PI * 0.5f, MathUtils.PI * 1.5f, 0f),
            floatArrayOf(MathUtils.PI * 0.5f, MathUtils.PI * 1.5f, MathUtils.PI)
        )
        return when {
            levelConfig.index in 81..83 -> vertical
            levelConfig.index in 84..86 -> if (missileWaveCounter % 2 == 0) horizontal else vertical
            else -> {
                val pick = mixed[(missileWaveCounter + (blockProgress * 10f).toInt()).mod(mixed.size)]
                if (shipCount <= 2) {
                    pick.copyOfRange(0, 2)
                } else if (shipCount == 3 && pick.size >= 3) {
                    pick.copyOfRange(0, 3)
                } else {
                    floatArrayOf(MathUtils.PI * 0.5f, MathUtils.PI * 1.5f, 0f, MathUtils.PI)
                }
            }
        }
    }

    private fun hasMissileHazards(): Boolean = levelConfig.index in 61..70 || levelConfig.index in 81..100

    fun setPlayerHitCircleRadius(normalizedRadius: Float) {
        playerHitCircleRadius = normalizedRadius.coerceIn(0.014f, 0.036f)
    }

    fun advanceLevel(): Boolean {
        if (levelIndex >= levels.lastIndex) {
            return false
        }

        levelIndex += 1
        attemptNumberForLevel = 0
        failureStreakForLevel = 0
        resetLevelToReady()
        return true
    }

    fun setLevel(levelIndex: Int) {
        this.levelIndex = levelIndex.coerceIn(0, levels.lastIndex)
        attemptNumberForLevel = 0
        failureStreakForLevel = 0
        resetLevelToReady()
    }

    fun activateShield(hitCount: Int = 1) {
        runShieldHitsRemaining = (runShieldHitsRemaining + hitCount.coerceAtLeast(0)).coerceAtMost(1)
    }

    fun activateManualSlow(durationSeconds: Float = 2.6f) {
        manualSlowRemainingSeconds = maxOf(manualSlowRemainingSeconds, durationSeconds.coerceIn(0.4f, 4f))
    }

    private fun computeRunSeed(levelIndex: Int, attempt: Int): Long {
        val a = (levelIndex + 1).toLong() * 0xD1B54A32D192ED03UL.toLong()
        val b = (attempt + 1).toLong() * 0x94D049BB133111EBUL.toLong()
        return campaignSeed xor a xor b
    }

    private fun updatePhaseGate(deltaSeconds: Float) {
        if (!hasReversePhaseGate) {
            directionMultiplier = 1
            phaseActiveRemainingSeconds = 0f
            phaseCooldownRemainingSeconds = 0f
            phaseGateStatus = PhaseGateStatus(
                active = false,
                directionMultiplier = 1,
                secondsToStateChange = 0f
            )
            return
        }

        if (phaseActiveRemainingSeconds > 0f) {
            phaseActiveRemainingSeconds -= deltaSeconds
            if (phaseActiveRemainingSeconds <= 0f) {
                phaseActiveRemainingSeconds = 0f
                phaseCooldownRemainingSeconds = nextPhaseCooldownSeconds()
                phaseGateStatus = PhaseGateStatus(
                    active = false,
                    directionMultiplier = directionMultiplier,
                    secondsToStateChange = phaseCooldownRemainingSeconds
                )
            } else {
                phaseGateStatus = PhaseGateStatus(
                    active = true,
                    directionMultiplier = directionMultiplier,
                    secondsToStateChange = phaseActiveRemainingSeconds
                )
            }
            return
        }

        phaseCooldownRemainingSeconds -= deltaSeconds
        if (phaseCooldownRemainingSeconds <= 0f) {
            directionMultiplier *= -1
            phaseActiveRemainingSeconds = nextPhaseDurationSeconds()
            phaseGateStatus = PhaseGateStatus(
                active = true,
                directionMultiplier = directionMultiplier,
                secondsToStateChange = phaseActiveRemainingSeconds
            )
            return
        }

        phaseGateStatus = PhaseGateStatus(
            active = false,
            directionMultiplier = directionMultiplier,
            secondsToStateChange = phaseCooldownRemainingSeconds
        )
    }

    private fun updateArenaMotion(deltaSeconds: Float) {
        arenaSpinShiftRemainingSeconds -= deltaSeconds
        if (arenaSpinShiftRemainingSeconds <= 0f) {
            arenaSpinShiftRemainingSeconds = nextArenaSpinShiftSeconds()
            arenaSpinTargetRadPerSecond = nextArenaSpinTargetRadPerSecond()
        }

        val desiredSpin = arenaSpinTargetRadPerSecond * if (phaseGateStatus.active) 1.45f else 1f
        val blend = (deltaSeconds * 3.6f).coerceAtMost(1f)
        arenaSpinRadPerSecond += (desiredSpin - arenaSpinRadPerSecond) * blend
        arenaRotationRad = AngleMath.normalizeRadians(
            arenaRotationRad + arenaSpinRadPerSecond * deltaSeconds
        )
    }

    private fun updatePlayerMotion(deltaSeconds: Float, playerInput: PlayerInput) {
        val turnAssist = dynamicTurnAssistMultiplier()
        if (usesStepMovement) {
            val sectorCount = levelConfig.sectorCount.coerceIn(3, 16)
            val step = (playerInput.stepDirection.coerceIn(-1, 1) * directionMultiplier)
            if (step != 0) {
                discreteLaneIndex = wrapLane(discreteLaneIndex + step, sectorCount)
            }
            playerTargetAngleRad = AngleMath.normalizeRadians(
                arenaRotationRad + laneCenterAngle(sectorCount, discreteLaneIndex)
            )
            val snapSpeed = levelConfig.needleAngularSpeedRad * 3.2f * turnAssist
            playerAngleRad = moveAngleTowards(playerAngleRad, playerTargetAngleRad, snapSpeed * deltaSeconds)
            return
        }

        val input = (playerInput.holdDirection.coerceIn(-1, 1) * directionMultiplier).toFloat()
        val assistCap = when (levelConfig.index) {
            in 61..100 -> 2.2f
            in 51..60 -> 2.02f
            in 41..50 -> 2.1f
            else -> 2.65f
        }
        val levelDamping = when (levelConfig.index) {
            in 61..100 -> 0.92f
            in 51..60 -> 0.82f
            in 41..50 -> 0.84f
            else -> 1f
        }
        val effectiveAssist = turnAssist.coerceAtMost(assistCap)
        val baselineObstacleSpeed = levels.firstOrNull()?.baseObstacleSpeed?.coerceAtLeast(0.01f)
            ?: levelConfig.baseObstacleSpeed.coerceAtLeast(0.01f)
        val speedRatio = (levelConfig.baseObstacleSpeed / baselineObstacleSpeed).coerceIn(1f, 2.2f)
        val speedProportionalBoost = (1f + (speedRatio - 1f) * 0.34f).coerceIn(1f, 1.42f)
        val targetAngularSpeed = levelConfig.needleAngularSpeedRad * effectiveAssist * levelDamping * speedProportionalBoost
        playerTargetAngleRad = AngleMath.normalizeRadians(
            playerTargetAngleRad + targetAngularSpeed * input * deltaSeconds
        )
        if (input == 0f) {
            playerTargetAngleRad = moveAngleTowards(
                playerTargetAngleRad,
                playerAngleRad,
                targetAngularSpeed * 0.38f * deltaSeconds
            )
        }
        val responseMultiplier = when {
            levelConfig.index in 61..100 && kotlin.math.abs(input) > 0f -> 1.7f
            levelConfig.index in 61..100 -> 1.48f
            levelConfig.index in 51..60 && kotlin.math.abs(input) > 0f -> 1.62f
            levelConfig.index in 51..60 -> 1.4f
            levelConfig.index in 41..50 && kotlin.math.abs(input) > 0f -> 1.62f
            levelConfig.index in 41..50 -> 1.38f
            kotlin.math.abs(input) > 0f -> 1.9f
            else -> 1.52f
        }
        playerAngleRad = moveAngleTowards(
            playerAngleRad,
            playerTargetAngleRad,
            targetAngularSpeed * responseMultiplier * deltaSeconds
        )
    }

    private fun dynamicTurnAssistMultiplier(): Float {
        if (mutableObstacles.isEmpty()) {
            return 1f
        }
        val playerLocalAngle = AngleMath.normalizeRadians(playerAngleRad - arenaRotationRad)
        val baseObstacleSpeed = levelConfig.baseObstacleSpeed.coerceAtLeast(0.01f)
        var strongestPressure = 0f

        for (obstacle in mutableObstacles) {
            val distanceToRing = (obstacle.radius - playerOrbitRadius).coerceAtLeast(0f)
            if (distanceToRing > 0.92f) {
                continue
            }

            val arrivalSeconds = timeToPlayerRingSeconds(obstacle.radius, obstacle.speed)
            val imminence = (1f - arrivalSeconds / 2.25f).coerceIn(0f, 1f)
            if (imminence <= 0f) {
                continue
            }

            val speedRatio = obstacle.speed / baseObstacleSpeed
            val speedPressure = ((speedRatio - 0.96f) / 0.92f).coerceIn(0f, 1.35f)
            if (speedPressure <= 0f && arrivalSeconds > 1.1f) {
                continue
            }

            val sectorAngle = MathUtils.PI2 / obstacle.sectorCount.toFloat()
            val gapCenterAtSpawn = AngleMath.normalizeRadians(
                obstacle.rotationRad + (obstacle.gapStartSector + obstacle.gapSectorCount * 0.5f) * sectorAngle
            )
            val gapCenterAtPlayer = AngleMath.normalizeRadians(
                gapCenterAtSpawn + obstacle.spinRadPerSecond * arrivalSeconds
            )
            val halfGap = obstacle.gapSectorCount * sectorAngle * 0.5f
            val requiredTurn = (
                kotlin.math.abs(shortestAngleDelta(playerLocalAngle, gapCenterAtPlayer)) - halfGap * 0.56f
                ).coerceAtLeast(0f)
            val turnPressure = (requiredTurn / MathUtils.PI).coerceIn(0f, 1f)
            val obstaclePressure = (0.18f + speedPressure * 0.66f) *
                (0.28f + imminence * 0.72f) *
                (0.36f + turnPressure * 0.94f)
            if (obstaclePressure > strongestPressure) {
                strongestPressure = obstaclePressure
            }
        }

        val tierPressure = ((levelConfig.featureTier - 1).toFloat() / 5f) * 0.22f
        val runPressure = runIntensity * 0.18f
        val combinedPressure = (strongestPressure + tierPressure + runPressure).coerceIn(0f, 1.35f)
        val assistBoost = if (levelConfig.index in 61..100) {
            1f + adaptiveAssistIntensity * 0.44f
        } else {
            1f + adaptiveAssistIntensity * 0.28f
        }
        return ((1f + combinedPressure * 1.55f) * assistBoost).coerceIn(1f, 3.35f)
    }

    private fun difficultyMultiplier(): Float {
        val rampStrength = (0.34f + (levelConfig.featureTier - 1) * 0.095f).coerceAtMost(0.9f)
        val curvedIntensity = runIntensity.coerceIn(0f, 1f).toDouble().pow(1.35).toFloat()
        val stormBoost = if (hasReversePhaseGate) {
            0.08f + if (phaseGateStatus.active) 0.12f else 0f
        } else {
            0f
        }
        val adaptiveEase = adaptiveAssistIntensity
        val adaptiveEaseStrength = if (levelConfig.index in 61..100) 0.44f else 0.2f
        val adaptiveMultiplier = (1f - adaptiveEase * adaptiveEaseStrength).coerceIn(0.74f, 1f)
        val progressiveMultiplier = 1f + curvedIntensity * rampStrength + stormBoost
        return progressiveMultiplier * userDifficultyMultiplier * adaptiveMultiplier
    }

    private fun smoothDifficultyMultiplier(target: Float, deltaSeconds: Float): Float {
        val current = smoothedDifficultyMultiplier
        val risePerSecond = if (levelConfig.index in 61..100) 0.2f else 0.26f
        val fallPerSecond = 0.44f
        val maxStep = if (target >= current) risePerSecond * deltaSeconds else fallPerSecond * deltaSeconds
        smoothedDifficultyMultiplier = current + (target - current).coerceIn(-maxStep, maxStep)
        return smoothedDifficultyMultiplier
    }

    private fun minimumSpawnIntervalSeconds(): Float {
        return when (levelConfig.index) {
            in 61..100 -> 0.28f
            in 51..60 -> 0.15f
            in 41..50 -> 0.13f
            else -> 0.12f
        }
    }

    private fun spawnReadabilityRules(difficultyMultiplier: Float): SpawnReadabilityRules {
        val pressure = (difficultyMultiplier - 1f).coerceIn(0f, 0.8f)
        val compression = (1f - pressure * 0.24f).coerceIn(0.78f, 1f)
        val adaptiveBoost = if (levelConfig.index in 61..100) {
            1f + adaptiveAssistIntensity * 0.34f
        } else {
            1f + adaptiveAssistIntensity * 0.22f
        }
        val readabilityBoost = userReadabilityMultiplier * adaptiveBoost
        val highTierSafetyBoost = if (levelConfig.index in 61..100) 1.12f else 1f
        val minGap = (levelConfig.minRadialGapBetweenRings * compression * readabilityBoost)
            .times(highTierSafetyBoost)
            .coerceAtLeast(ABSOLUTE_MIN_RING_GAP)
        val targetGap = (levelConfig.targetRadialGapBetweenRings * compression)
            .times(readabilityBoost)
            .times(highTierSafetyBoost)
            .coerceAtLeast(minGap)
            .coerceAtLeast(ABSOLUTE_TARGET_RING_GAP)
        val readableGap = (
            levelConfig.minReadableThreatGapSeconds *
                (0.84f + compression * 0.16f) *
                readabilityBoost
            )
            .times(if (levelConfig.index in 61..100) 1.08f else 1f)
            .coerceAtLeast(ABSOLUTE_MIN_READABLE_SECONDS)
        return SpawnReadabilityRules(
            minRadialGapBetweenRings = minGap,
            targetRadialGapBetweenRings = targetGap,
            minReadableThreatGapSeconds = readableGap
        )
    }

    private fun spawnDelayForReadability(
        candidate: Obstacle,
        rules: SpawnReadabilityRules,
        collider: CollisionEngine.PlayerCollider
    ): Float {
        var requiredDelay = 0f
        val newestObstacle = mutableObstacles.maxByOrNull { it.radius }
        if (newestObstacle != null) {
            val radialGap = (SPAWN_RADIUS - newestObstacle.radius).coerceAtLeast(0f)
            val speed = newestObstacle.speed.coerceAtLeast(0.01f)
            val missingTargetGap = rules.targetRadialGapBetweenRings - radialGap
            if (missingTargetGap > 0f) {
                requiredDelay = maxOf(requiredDelay, missingTargetGap / speed)
            }
            val missingMinGap = rules.minRadialGapBetweenRings - radialGap
            if (missingMinGap > 0f) {
                requiredDelay = maxOf(requiredDelay, missingMinGap / speed)
            }
        }

        val candidateArrival = runClockSeconds + timeToPlayerRingSeconds(
            radialPosition = candidate.radius,
            speed = candidate.speed
        )
        val candidateThreat = threatSnapshotFor(candidate, candidateArrival, collider)
        var latestExistingArrival: Float? = null
        val existingThreats = ArrayList<ThreatSnapshot>(mutableObstacles.size.coerceAtMost(16))
        for (obstacle in mutableObstacles) {
            if (!isObstacleStillLethal(obstacle, collider)) {
                continue
            }
            val arrival = runClockSeconds + timeToPlayerRingSeconds(obstacle.radius, obstacle.speed)
            if (latestExistingArrival == null || arrival > latestExistingArrival) {
                latestExistingArrival = arrival
            }
            if (arrival <= candidateArrival + 0.0001f) {
                existingThreats.add(threatSnapshotFor(obstacle, arrival, collider))
            }
        }
        if (latestExistingArrival != null) {
            val extraSequentialGap = when (levelConfig.index) {
                in 61..100 -> 0.34f
                in 51..60 -> 0.1f
                else -> 0f
            }
            val requiredSequentialArrival = latestExistingArrival + 0.05f + extraSequentialGap
            val missingSequentialWindow = requiredSequentialArrival - candidateArrival
            if (missingSequentialWindow > 0f) {
                requiredDelay = maxOf(requiredDelay, missingSequentialWindow)
            }
        }
        if (existingThreats.isNotEmpty()) {
            for (previousThreat in existingThreats) {
                val previousClearSeconds = previousThreat.arrivalSeconds + previousThreat.occupancySeconds
                val readableGapAfterClear = candidateArrival - previousClearSeconds
                val minStableGap = (
                    rules.minReadableThreatGapSeconds + previousThreat.occupancySeconds * 0.48f
                    ).coerceAtLeast(if (levelConfig.index in 61..100) 0.45f else 0.3f)
                val missingReadableTime = minStableGap - readableGapAfterClear
                if (missingReadableTime > 0f) {
                    requiredDelay = maxOf(requiredDelay, missingReadableTime)
                }

                val rotationalDelta = kotlin.math.abs(
                    shortestAngleDelta(
                        previousThreat.predictedGapCenterAngleRad,
                        candidateThreat.predictedGapCenterAngleRad
                    )
                )
                val sharedGapForgiveness = (
                    previousThreat.halfGapAngleRad + candidateThreat.halfGapAngleRad
                    ) * 0.56f
                val requiredTravelRad = (
                    rotationalDelta - sharedGapForgiveness + collider.halfArcWidthRad * 1.16f
                    ).coerceAtLeast(0f)
                val effectivePlayerAngularSpeed = if (usesStepMovement) {
                    (levelConfig.needleAngularSpeedRad * 2.35f).coerceAtLeast(0.01f)
                } else {
                    (levelConfig.needleAngularSpeedRad * 1.26f).coerceAtLeast(0.01f)
                }
                val phaseBufferSeconds = if (hasReversePhaseGate) 0.26f else 0.2f
                val minTravelWindow = phaseBufferSeconds + (requiredTravelRad / effectivePlayerAngularSpeed)
                val travelWindow = candidateArrival - previousClearSeconds
                val missingTravelWindow = minTravelWindow - travelWindow
                if (missingTravelWindow > 0f) {
                    requiredDelay = maxOf(requiredDelay, missingTravelWindow)
                }

                val overlapFactor = 1f - (rotationalDelta / MathUtils.PI).coerceIn(0f, 1f)
                val temporalCompression = 1f - (readableGapAfterClear / minStableGap).coerceIn(0f, 1f)
                val collapseRisk = (overlapFactor * temporalCompression).coerceIn(0f, 1f)
                if (collapseRisk > 0.56f) {
                    val antiCollapseDelay = 0.06f + collapseRisk * 0.16f
                    requiredDelay = maxOf(requiredDelay, antiCollapseDelay)
                }
            }
        }

        val currentLocalPlayerAngle = AngleMath.normalizeRadians(playerAngleRad - arenaRotationRad)
        val immediateTravelDelta = kotlin.math.abs(
            shortestAngleDelta(currentLocalPlayerAngle, candidateThreat.predictedGapCenterAngleRad)
        )
        val immediateTravelRequired = (
            immediateTravelDelta - candidateThreat.halfGapAngleRad + collider.halfArcWidthRad
            ).coerceAtLeast(0f)
        val immediateSpeed = if (usesStepMovement) {
            (levelConfig.needleAngularSpeedRad * 2.5f).coerceAtLeast(0.01f)
        } else {
            (levelConfig.needleAngularSpeedRad * 1.22f).coerceAtLeast(0.01f)
        }
        val immediateWindow = (candidateArrival - runClockSeconds).coerceAtLeast(0f)
        val immediateRequiredWindow = 0.16f + immediateTravelRequired / immediateSpeed
        val missingImmediateWindow = immediateRequiredWindow - immediateWindow
        if (missingImmediateWindow > 0f) {
            requiredDelay = maxOf(requiredDelay, missingImmediateWindow)
        }

        var activeThreatDensity = 0
        for (obstacle in mutableObstacles) {
            if (obstacle.radius - obstacle.thickness > collider.radius) {
                activeThreatDensity += 1
            }
        }
        val effectiveMaxDensity = when (levelConfig.index) {
            in 61..100 -> (levelConfig.maxConcurrentThreatDensity - 2).coerceAtLeast(2)
            else -> levelConfig.maxConcurrentThreatDensity
        }
        if (activeThreatDensity >= effectiveMaxDensity) {
            var minReleaseSeconds: Float? = null
            for (obstacle in mutableObstacles) {
                val releaseSeconds = timeToPlayerRingSeconds(obstacle.radius - obstacle.thickness, obstacle.speed)
                if (releaseSeconds <= 0f) {
                    continue
                }
                if (minReleaseSeconds == null || releaseSeconds < minReleaseSeconds) {
                    minReleaseSeconds = releaseSeconds
                }
            }
            val releaseDelay = minReleaseSeconds?.plus(0.02f) ?: 0f
            requiredDelay = maxOf(requiredDelay, releaseDelay)
        }

        return requiredDelay
    }

    private fun timeToPlayerRingSeconds(radialPosition: Float, speed: Float): Float {
        val distanceToPlayer = (radialPosition - playerOrbitRadius).coerceAtLeast(0f)
        return distanceToPlayer / speed.coerceAtLeast(0.01f)
    }

    private fun threatSnapshotFor(
        obstacle: Obstacle,
        arrivalSeconds: Float,
        collider: CollisionEngine.PlayerCollider
    ): ThreatSnapshot {
        val sectorAngle = MathUtils.PI2 / obstacle.sectorCount.toFloat()
        val gapCenterAtSpawn = AngleMath.normalizeRadians(
            obstacle.rotationRad + (obstacle.gapStartSector + obstacle.gapSectorCount * 0.5f) * sectorAngle
        )
        val travelSeconds = timeToPlayerRingSeconds(obstacle.radius, obstacle.speed)
        val gapCenterAtPlayerRing = AngleMath.normalizeRadians(
            gapCenterAtSpawn + obstacle.spinRadPerSecond * travelSeconds
        )
        val halfGap = obstacle.gapSectorCount * sectorAngle * 0.5f
        val speed = obstacle.speed.coerceAtLeast(0.01f)
        val occupancySeconds = (
            obstacle.thickness + collider.radialTolerance * 2.2f
            ) / speed
        return ThreatSnapshot(
            arrivalSeconds = arrivalSeconds,
            predictedGapCenterAngleRad = gapCenterAtPlayerRing,
            halfGapAngleRad = halfGap,
            occupancySeconds = occupancySeconds.coerceIn(0.06f, 1.4f)
        )
    }

    private fun isObstacleStillLethal(
        obstacle: Obstacle,
        playerCollider: CollisionEngine.PlayerCollider
    ): Boolean {
        if (obstacle.patternId == "gravity_pull" || obstacle.patternId == "time_bubble") {
            return false
        }
        val playerInner = playerCollider.radius - playerCollider.radialTolerance
        return obstacle.radius >= playerInner
    }

    private fun activePlayerCollider(): CollisionEngine.PlayerCollider {
        val miniModeMultiplier = if (levelConfig.index in 31..40) 0.78f else 1f
        val hitRadius = (playerHitCircleRadius * miniModeMultiplier * 0.9f).coerceAtMost(playerOrbitRadius * 0.82f)
        val tangentialArc = kotlin.math.asin((hitRadius / playerOrbitRadius).coerceIn(0f, 0.92f))
        return CollisionEngine.PlayerCollider(
            radius = playerOrbitRadius,
            halfArcWidthRad = tangentialArc,
            radialTolerance = hitRadius
        )
    }

    private fun relaxOrbitRadius(deltaSeconds: Float) {
        if (playerOrbitRadius >= PLAYER_COLLIDER.radius) {
            playerOrbitRadius = PLAYER_COLLIDER.radius
            return
        }
        playerOrbitRadius = (playerOrbitRadius + deltaSeconds * 0.017f).coerceAtMost(PLAYER_COLLIDER.radius)
    }

    private fun applyGravityWallImpact(obstacle: Obstacle) {
        val speedPressure = (
            obstacle.speed /
                levelConfig.baseObstacleSpeed.coerceAtLeast(0.01f)
            ).coerceIn(0.9f, 1.85f)
        val pullByThickness = (
            0.022f +
                obstacle.thickness * 0.18f +
                speedPressure * 0.008f
            ).coerceIn(0.022f, 0.056f)
        playerOrbitRadius = (playerOrbitRadius - pullByThickness).coerceAtLeast(BLACK_HOLE_VISUAL_RADIUS * 0.82f)
        playerTargetAngleRad = playerAngleRad
    }

    private fun applyTimeBubbleImpact() {
        if (levelConfig.index !in 31..40) {
            return
        }
        timeBubbleRemainingSeconds = maxOf(timeBubbleRemainingSeconds, TIME_BUBBLE_SECONDS)
    }

    private fun updateFeatureTimers(deltaSeconds: Float) {
        timeBubbleRemainingSeconds = (timeBubbleRemainingSeconds - deltaSeconds).coerceAtLeast(0f)
        manualSlowRemainingSeconds = (manualSlowRemainingSeconds - deltaSeconds).coerceAtLeast(0f)
    }

    private fun nextPhaseCooldownSeconds(): Float {
        val tierProgress = (levelConfig.featureTier - 1).toFloat() / 6f
        val levelProgress = (levelConfig.index - 1).toFloat() / (levels.size - 1).coerceAtLeast(1).toFloat()
        val baseMin = (2.24f - tierProgress * 0.92f - levelProgress * 0.18f).coerceAtLeast(1.04f)
        val baseMax = (3.02f - tierProgress * 1.14f - levelProgress * 0.24f).coerceAtLeast(baseMin + 0.36f)
        return phaseRng.nextFloat(baseMin, baseMax)
    }

    private fun nextPhaseDurationSeconds(): Float {
        val tierProgress = (levelConfig.featureTier - 1).toFloat() / 6f
        val levelProgress = (levelConfig.index - 1).toFloat() / (levels.size - 1).coerceAtLeast(1).toFloat()
        val baseMin = 1.08f + tierProgress * 0.46f + levelProgress * 0.08f
        val baseMax = 1.62f + tierProgress * 0.64f + levelProgress * 0.12f
        return phaseRng.nextFloat(baseMin, baseMax)
    }

    private fun nextArenaSpinTargetRadPerSecond(): Float {
        val progress = (levelConfig.index - 1).toFloat() / (levels.size - 1).coerceAtLeast(1).toFloat()
        val degrees = phaseRng.nextFloat(22f + progress * 18f, 54f + progress * 54f)
        val sign = if (phaseRng.nextInt(2) == 0) -1f else 1f
        return degrees * MathUtils.degreesToRadians * sign
    }

    private fun nextArenaSpinShiftSeconds(): Float {
        val progress = (levelConfig.index - 1).toFloat() / (levels.size - 1).coerceAtLeast(1).toFloat()
        return phaseRng.nextFloat(1.15f, 2.2f - progress * 0.55f)
    }

    private fun playerAngleForCurrentMode(): Float {
        return if (usesStepMovement) {
            laneCenterAngle(levelConfig.sectorCount.coerceIn(3, 16), discreteLaneIndex)
        } else {
            0f
        }
    }

    private fun laneCenterAngle(sectorCount: Int, laneIndex: Int): Float {
        return MathUtils.PI2 * (wrapLane(laneIndex, sectorCount) + 0.5f) / sectorCount.toFloat()
    }

    private fun wrapLane(value: Int, sectorCount: Int): Int {
        return ((value % sectorCount) + sectorCount) % sectorCount
    }

    private fun moveAngleTowards(current: Float, target: Float, maxDelta: Float): Float {
        val delta = shortestAngleDelta(current, target)
        if (kotlin.math.abs(delta) <= maxDelta) {
            return AngleMath.normalizeRadians(target)
        }
        return AngleMath.normalizeRadians(current + kotlin.math.sign(delta) * maxDelta)
    }

    private fun shortestAngleDelta(from: Float, to: Float): Float {
        var delta = AngleMath.normalizeRadians(to - from)
        if (delta > MathUtils.PI) {
            delta -= MathUtils.PI2
        }
        return delta
    }
}
