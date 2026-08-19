package com.luminadigitale.fluxcore.core.engine

import com.badlogic.gdx.math.MathUtils

class PatternGenerator(
    private val levelConfig: LevelConfig,
    seed: Long
) {
    data class SpawnPlan(
        val obstacle: Obstacle,
        val intervalSeconds: Float
    )

    private data class BurstState(
        val template: PatternTemplate,
        val sectorCount: Int,
        val gapSectorCount: Int,
        var gapStartSector: Int,
        val laneStep: Int,
        var remaining: Int,
        val baseRotationRad: Float,
        val spinRadPerSecond: Float
    )

    private val rng = SeededRng(seed xor (levelConfig.index.toLong() * 0x9E3779B97F4A7C15UL.toLong()))
    private val templates = PatternCatalog.templates
    private val levelSlot = (levelConfig.index - 1) % 10
    private val levelBlockProgress = levelSlot.toFloat() / 9f
    private val signatureFamily = PatternFamily.entries[(levelConfig.index - 1) % PatternFamily.entries.size]
    private val unlockedTemplates = templates.filter {
        (
            it.unlockTier <= levelConfig.featureTier ||
                (levelSlot >= 8 && it.unlockTier == levelConfig.featureTier + 1)
            ) && isTemplateAllowedForLevel(it)
    }
    private val unlockedTemplatesByFamily = unlockedTemplates.groupBy { it.family }
    private var activeBurst: BurstState? = null
    private var pendingSpawnPlan: SpawnPlan? = null
    private var pendingObstacleRead = false
    private var pendingIntervalRead = false
    private var activePhraseFamily: PatternFamily? = null
    private var burstsRemainingInPhrase = 0
    private var previousTemplateId: String? = null
    private var previousObstacleSpeed: Float? = null

    fun nextSpawn(spawnRadius: Float, difficultyMultiplier: Float): SpawnPlan {
        val plan = ensurePending(spawnRadius, difficultyMultiplier)
        clearPending()
        return plan
    }

    fun nextSpawnInterval(difficultyMultiplier: Float): Float {
        val plan = ensurePending(spawnRadius = 1.22f, difficultyMultiplier = difficultyMultiplier)
        pendingIntervalRead = true
        clearPendingIfConsumed()
        return plan.intervalSeconds
    }

    fun nextObstacle(spawnRadius: Float, difficultyMultiplier: Float): Obstacle {
        val plan = ensurePending(spawnRadius, difficultyMultiplier)
        pendingObstacleRead = true
        clearPendingIfConsumed()
        return plan.obstacle
    }

    private fun ensurePending(spawnRadius: Float, difficultyMultiplier: Float): SpawnPlan {
        pendingSpawnPlan?.let { return it }

        val burst = activeBurst ?: startBurst().also { activeBurst = it }
        val template = burst.template
        val speedMultiplier = rng.nextFloat(
            template.speedMultiplierMin,
            template.speedMultiplierMax
        )
        val thicknessMultiplier = rng.nextFloat(
            template.thicknessMultiplierMin,
            template.thicknessMultiplierMax
        )
        val intervalBase = rng.nextFloat(
            levelConfig.spawnIntervalMinSeconds,
            levelConfig.spawnIntervalMaxSeconds
        )
        val intervalTemplateMultiplier = rng.nextFloat(
            template.spawnIntervalMultiplierMin,
            template.spawnIntervalMultiplierMax
        )
        val interval = (
            intervalBase *
                intervalTemplateMultiplier *
                cadenceMultiplierFor(template.id) /
                difficultyMultiplier.coerceAtLeast(1f)
            ).coerceAtLeast(if (levelConfig.index in 61..100) 0.16f else 0.11f)

        val rawSpeed = levelConfig.baseObstacleSpeed * speedMultiplier * difficultyMultiplier.coerceAtLeast(1f)
        val smoothedSpeed = previousObstacleSpeed?.let { previous ->
            rawSpeed.coerceIn(previous * 0.82f, previous * 1.18f)
        } ?: rawSpeed
        previousObstacleSpeed = smoothedSpeed

        val obstacle = Obstacle(
            sectorCount = burst.sectorCount,
            radius = spawnRadius,
            speed = smoothedSpeed,
            thickness = levelConfig.obstacleThickness * thicknessMultiplier,
            gapStartSector = burst.gapStartSector,
            gapSectorCount = burst.gapSectorCount,
            patternId = template.id,
            rotationRad = burst.baseRotationRad,
            spinRadPerSecond = burst.spinRadPerSecond
        )

        burst.remaining -= 1
        burst.gapStartSector = wrapSector(burst.gapStartSector + burst.laneStep, burst.sectorCount)
        if (burst.remaining <= 0) {
            activeBurst = null
        }

        pendingObstacleRead = false
        pendingIntervalRead = false
        return SpawnPlan(
            obstacle = obstacle,
            intervalSeconds = interval
        ).also { pendingSpawnPlan = it }
    }

    private fun clearPendingIfConsumed() {
        if (pendingObstacleRead && pendingIntervalRead) {
            clearPending()
        }
    }

    private fun clearPending() {
        pendingSpawnPlan = null
        pendingObstacleRead = false
        pendingIntervalRead = false
    }

    private fun startBurst(): BurstState {
        val template = nextTemplate()
        val sectorCount = sectorCountFor(template)
        val gapSectorCount = gapSectorCountFor(template, sectorCount)
        val sectorAngle = MathUtils.PI2 / sectorCount.toFloat()
        val rotationJitter = when (template.id) {
            "drift_gap", "split_lane" -> sectorAngle * 0.5f
            "pulse_ring" -> sectorAngle * 0.25f
            else -> 0f
        }
        return BurstState(
            template = template,
            sectorCount = sectorCount,
            gapSectorCount = gapSectorCount,
            gapStartSector = rng.nextInt(sectorCount),
            laneStep = laneStepFor(template.id, sectorCount),
            remaining = repeatCountFor(template.id),
            baseRotationRad = rng.nextFloat(0f, rotationJitter),
            spinRadPerSecond = spinFor(template.id)
        )
    }

    private fun sectorCountFor(template: PatternTemplate): Int {
        val slot = levelSlot
        val dynamicOffset = when {
            levelConfig.sectorCount >= 20 && slot >= 6 -> if (rng.nextInt(2) == 0) 1 else 0
            levelConfig.sectorCount > 8 && slot >= 7 -> if (rng.nextInt(3) == 0) -1 else 0
            else -> 0
        }
        return if (levelConfig.sectorCount >= 20) {
            (levelConfig.sectorCount + template.sectorOffset + dynamicOffset).coerceIn(20, 24)
        } else if (levelConfig.sectorCount > 8) {
            (levelConfig.sectorCount + template.sectorOffset + dynamicOffset).coerceIn(
                (levelConfig.sectorCount - 2).coerceAtLeast(9),
                (levelConfig.sectorCount + 2).coerceAtMost(18)
            )
        } else {
            levelConfig.sectorCount.coerceIn(3, 8)
        }
    }

    private fun gapSectorCountFor(template: PatternTemplate, sectorCount: Int): Int {
        val rawGapSectorCount = when (template.id) {
            "wide_ring", "pulse_ring" -> (template.gapOverride ?: levelConfig.gapSectorCount) + 1
            "gravity_pull" -> (template.gapOverride ?: levelConfig.gapSectorCount) + 2
            "time_bubble" -> (template.gapOverride ?: levelConfig.gapSectorCount) + 2
            "missile_volley" -> (template.gapOverride ?: levelConfig.gapSectorCount) + 1
            "flux_mirror" -> (template.gapOverride ?: levelConfig.gapSectorCount) + 1
            "dense_blades", "needle_window", "final_crush" -> 2
            "core_surge" -> 2
            else -> template.gapOverride ?: levelConfig.gapSectorCount
        }
        val safeGapSectorCount = minimumSafeGapSectors(sectorCount)
        val readabilityFloor = when {
            sectorCount >= 20 -> 7
            sectorCount >= 16 -> 6
            sectorCount >= 12 -> 4
            sectorCount >= 8 -> 3
            else -> 3
        }
        return rawGapSectorCount
            .coerceAtLeast(safeGapSectorCount)
            .coerceAtLeast(readabilityFloor)
            .coerceIn(1, (sectorCount - 1).coerceAtLeast(1))
    }

    private fun repeatCountFor(_templateId: String): Int {
        return 1
    }

    private fun laneStepFor(templateId: String, sectorCount: Int): Int {
        if (sectorCount <= 1) {
            return 0
        }
        if (levelConfig.index <= 5) {
            return 0
        }

        val baseShift = if (sectorCount > 12) 2 else 1
        val maxLaneShift = (baseShift + (levelConfig.laneShiftSeverity - 1)).coerceAtMost(sectorCount / 3)
        val safeLaneShift = maxLaneShift.coerceAtMost(1)
        val direction = if (rng.nextInt(2) == 0) -1 else 1
        return when (templateId) {
            "clean_arc" -> if (levelConfig.index <= 10 && rng.nextInt(3) == 0) direction else 0
            "wide_ring" -> if (levelConfig.index <= 10 && levelBlockProgress > 0.45f) direction else 0
            "pulse_ring" -> if (levelConfig.index <= 10) direction else 0
            "time_bubble" -> 0
            "narrow_gate", "needle_window" -> direction
            "gravity_pull" -> 0
            "flux_mirror" -> -direction * maxOf(1, safeLaneShift)
            "missile_volley" -> direction * maxOf(1, safeLaneShift)
            "tight_teeth", "split_lane" -> direction * safeLaneShift
            "drift_gap" -> direction
            "dense_blades", "core_surge", "final_crush" -> direction * safeLaneShift
            else -> 0
        }
    }

    private fun cadenceMultiplierFor(templateId: String): Float {
        val base = when (templateId) {
            "wide_ring" -> 1.08f
            "pulse_ring" -> 0.92f
            "gravity_pull" -> 1.24f
            "time_bubble" -> 1.28f
            "flux_mirror" -> 0.96f
            "missile_volley" -> 0.9f
            "core_surge", "dense_blades", "tight_teeth" -> 0.84f
            "needle_window" -> 0.9f
            "final_crush" -> 0.74f
            else -> 1f
        }
        val slotWave = when (levelSlot % 3) {
            0 -> 1.04f
            1 -> 0.95f
            else -> 1.01f
        }
        val latePush = 1f - levelBlockProgress * 0.06f
        return (base * slotWave * latePush).coerceIn(0.68f, 1.16f)
    }

    private fun spinFor(templateId: String): Float {
        val maxProgressIndex = when {
            levelConfig.index <= 60 -> 59f
            levelConfig.index <= 80 -> 79f
            else -> 99f
        }
        val levelProgress = ((levelConfig.index - 1).toFloat() / maxProgressIndex).coerceIn(0f, 1f)
        val blockSpinBoost = 1f + levelBlockProgress * 0.16f
        val postTenSpinBoost = if (levelConfig.index <= 10) {
            1f
        } else {
            val postTenRange = when {
                levelConfig.index <= 60 -> 50f
                levelConfig.index <= 80 -> 70f
                else -> 90f
            }
            1f + ((levelConfig.index - 10).toFloat() / postTenRange) * 0.45f
        }
        val tierSpinBoost = when (levelConfig.index) {
            in 11..20 -> 1.18f
            in 21..30 -> 1.14f
            in 31..40 -> 1.1f
            in 41..50 -> 1.2f
            in 51..60 -> 1.24f
            in 61..70 -> 1.28f
            in 71..80 -> 1.3f
            in 81..90 -> 1.34f
            in 91..100 -> 1.26f
            else -> 1f
        }
        val degrees = when (templateId) {
            "drift_gap" -> rng.nextFloat(10f, (18f + levelProgress * 14f) * blockSpinBoost)
            "pulse_ring" -> rng.nextFloat(14f, (26f + levelProgress * 18f) * blockSpinBoost)
            "gravity_pull" -> rng.nextFloat(4f, (9f + levelProgress * 6f) * blockSpinBoost)
            "time_bubble" -> rng.nextFloat(3f, (8f + levelProgress * 5f) * blockSpinBoost)
            "flux_mirror" -> rng.nextFloat(8f, (20f + levelProgress * 14f) * blockSpinBoost)
            "missile_volley" -> rng.nextFloat(16f, (28f + levelProgress * 18f) * blockSpinBoost)
            "split_lane" -> rng.nextFloat(12f, (22f + levelProgress * 16f) * blockSpinBoost)
            "core_surge" -> rng.nextFloat(18f, (34f + levelProgress * 22f) * blockSpinBoost)
            "final_crush" -> rng.nextFloat(18f, (32f + levelProgress * 18f) * blockSpinBoost)
            else -> if (levelConfig.index > 10) {
                rng.nextFloat(5f, (11f + levelProgress * 9f) * blockSpinBoost)
            } else {
                0f
            }
        }
        if (degrees == 0f) {
            return 0f
        }
        val boostedDegrees = (degrees * postTenSpinBoost * tierSpinBoost).coerceAtMost(96f)
        val sign = if (rng.nextInt(2) == 0) -1f else 1f
        return boostedDegrees * MathUtils.degreesToRadians * sign
    }

    private fun nextTemplate(): PatternTemplate {
        if (unlockedTemplates.isEmpty()) {
            return templates.first()
        }

        if (burstsRemainingInPhrase <= 0 || activePhraseFamily == null) {
            activePhraseFamily = choosePhraseFamily()
            burstsRemainingInPhrase = phraseLengthFor(activePhraseFamily!!)
        }

        val family = activePhraseFamily!!
        val familyTemplates = unlockedTemplatesByFamily[family].orEmpty()
        val pool = if (familyTemplates.isNotEmpty()) familyTemplates else unlockedTemplates
        val chosen = pickWeightedTemplate(pool)
        previousTemplateId = chosen.id

        burstsRemainingInPhrase -= 1
        if (burstsRemainingInPhrase <= 0) {
            activePhraseFamily = null
        }
        return chosen
    }

    private fun phraseLengthFor(family: PatternFamily): Int {
        return when (family) {
            PatternFamily.SINGLE_THREAT -> 2 + rng.nextInt(2)
            PatternFamily.DOUBLE_PHRASE -> 2 + rng.nextInt(3)
            PatternFamily.DELAYED_FOLLOW_UP -> 2 + rng.nextInt(2)
            PatternFamily.ALTERNATING_PRESSURE -> 3 + rng.nextInt(2)
            PatternFamily.SPIRAL_PRESSURE -> 3 + rng.nextInt(3)
            PatternFamily.FAKE_OUT_SHIFT -> 2 + rng.nextInt(2)
        }
    }

    private fun choosePhraseFamily(): PatternFamily {
        val families = unlockedTemplatesByFamily.keys.toList().ifEmpty {
            unlockedTemplates.map { it.family }.distinct()
        }
        if (families.isEmpty()) {
            return PatternFamily.SINGLE_THREAT
        }
        val preferredWeights = familyWeights()
        val totalWeight = families.sumOf { family -> (preferredWeights[family] ?: 1f).toDouble() }
            .toFloat()
            .coerceAtLeast(0.0001f)
        var pick = rng.nextFloat(0f, totalWeight)
        for (family in families) {
            pick -= preferredWeights[family] ?: 1f
            if (pick <= 0f) {
                return family
            }
        }
        return families.last()
    }

    private fun familyWeights(): Map<PatternFamily, Float> {
        val tier = levelConfig.featureTier.coerceIn(1, 10)
        val phase = levelBlockProgress
        val early = (1f - phase * 1.35f).coerceIn(0f, 1f)
        val mid = (1f - kotlin.math.abs(phase - 0.5f) * 2f).coerceIn(0f, 1f)
        val late = ((phase - 0.52f) / 0.48f).coerceIn(0f, 1f)
        val weights = mutableMapOf(
            PatternFamily.SINGLE_THREAT to (3.3f - tier * 0.34f + early * 1.3f - late * 0.55f).coerceAtLeast(0.7f),
            PatternFamily.DOUBLE_PHRASE to (2.5f - tier * 0.18f + mid * 0.9f).coerceAtLeast(0.9f),
            PatternFamily.DELAYED_FOLLOW_UP to (2.1f + tier * 0.1f + mid * 0.35f),
            PatternFamily.ALTERNATING_PRESSURE to (1.0f + tier * 0.25f + late * 0.9f),
            PatternFamily.SPIRAL_PRESSURE to (0.8f + tier * 0.3f + late * 1.05f),
            PatternFamily.FAKE_OUT_SHIFT to (
                if (tier >= 3) {
                    0.75f + tier * 0.22f + late * 1.15f
                } else {
                    0.14f + late * 0.2f
                }
            )
        )
        weights[signatureFamily] = (weights[signatureFamily] ?: 1f) * 1.34f
        if (levelConfig.index <= 5) {
            weights[PatternFamily.SINGLE_THREAT] = (weights[PatternFamily.SINGLE_THREAT] ?: 1f) * 1.45f
            weights[PatternFamily.DOUBLE_PHRASE] = (weights[PatternFamily.DOUBLE_PHRASE] ?: 1f) * 1.2f
            weights[PatternFamily.SPIRAL_PRESSURE] = (weights[PatternFamily.SPIRAL_PRESSURE] ?: 1f) * 0.55f
            weights[PatternFamily.FAKE_OUT_SHIFT] = (weights[PatternFamily.FAKE_OUT_SHIFT] ?: 1f) * 0.45f
        }
        return weights
    }

    private fun pickWeightedTemplate(candidates: List<PatternTemplate>): PatternTemplate {
        val basePool = candidates.ifEmpty { unlockedTemplates.ifEmpty { templates } }
        val pool = if (previousTemplateId != null && basePool.size > 1) {
            basePool.filter { it.id != previousTemplateId }
        } else {
            basePool
        }
        var totalWeight = 0f
        val weights = FloatArray(pool.size)
        for (i in pool.indices) {
            val template = pool[i]
            val weight = templateWeightForLevel(template).coerceAtLeast(0.05f)
            weights[i] = weight
            totalWeight += weight
        }
        if (totalWeight <= 0f) {
            return pool[rng.nextInt(pool.size)]
        }
        var pick = rng.nextFloat(0f, totalWeight)
        for (i in pool.indices) {
            pick -= weights[i]
            if (pick <= 0f) {
                return pool[i]
            }
        }
        return pool.last()
    }

    private fun templateWeightForLevel(template: PatternTemplate): Float {
        // Use Kotlin stdlib Int.mod (compiled into bytecode) instead of java.lang.Math.floorMod,
        // which RoboVM's iOS runtime does not implement (NoSuchMethodError → crash on run start).
        val moduloBand = (template.id.hashCode() xor (levelConfig.index * 110351524)).mod(7)
        val levelBand = (levelConfig.index + levelSlot) % 7
        val bandAffinity = when ((moduloBand - levelBand).mod(7)) {
            0 -> 1.8f
            1, 6 -> 1.35f
            2, 5 -> 1.08f
            else -> 0.82f
        }
        val warBoost = when (template.id) {
            "missile_volley" -> if (levelConfig.index in 61..70) 2.05f else 0.01f
            "core_surge" -> if (levelConfig.index in 41..60) 1.32f else 0.95f
            "final_crush" -> if (levelConfig.index in 61..70) 0.52f else 1f
            "dense_blades" -> if (levelConfig.index in 61..70) 0.74f else 1f
            "split_lane" -> if (levelConfig.index in 61..70) 0.82f else 1f
            else -> 1f
        }
        val earlyEase = when (template.id) {
            "clean_arc", "narrow_gate", "wide_ring", "drift_gap" -> if (levelConfig.index <= 5) 1.6f else 1f
            "tight_teeth", "dense_blades", "core_surge", "final_crush", "split_lane" -> if (levelConfig.index <= 5) 0.28f else 1f
            "pulse_ring", "gravity_pull", "needle_window", "flux_mirror" -> if (levelConfig.index <= 5) 0.55f else 1f
            else -> 1f
        }
        val repeatPenalty = if (template.id == previousTemplateId) 0.42f else 1f
        return bandAffinity * earlyEase * repeatPenalty * warBoost
    }

    private fun wrapSector(value: Int, sectorCount: Int): Int {
        return ((value % sectorCount) + sectorCount) % sectorCount
    }

    private fun minimumSafeGapSectors(sectorCount: Int): Int {
        val sectorAngle = MathUtils.PI2 / sectorCount.toFloat()
        val requiredGapAngle =
            GameSimulation.PLAYER_COLLIDER.halfArcWidthRad * 6.6f +
                levelConfig.needleAngularSpeedRad * 0.3f
        return kotlin.math.ceil(requiredGapAngle / sectorAngle).toInt().coerceAtLeast(1)
    }

    private fun isTemplateAllowedForLevel(template: PatternTemplate): Boolean {
        return when (template.id) {
            "time_bubble" -> levelConfig.index in 31..40
            "final_crush" -> levelConfig.index >= 41
            "missile_volley" -> levelConfig.index in 61..70
            "core_surge" -> levelConfig.index >= 41
            "dense_blades" -> levelConfig.index >= 21
            "flux_mirror" -> levelConfig.index >= 11
            "split_lane" -> levelConfig.index >= 16
            "needle_window" -> levelConfig.index >= 11
            "tight_teeth" -> levelConfig.index >= 8
            "gravity_pull", "pulse_ring" -> levelConfig.index >= 6
            "clean_arc", "narrow_gate", "wide_ring", "drift_gap" -> true
            else -> true
        }
    }
}
