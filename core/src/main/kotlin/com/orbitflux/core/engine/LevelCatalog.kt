package com.orbitflux.core.engine

import kotlin.math.PI
import kotlin.math.floor

object LevelCatalog {
    fun create(totalLevels: Int = 60): List<LevelConfig> {
        require(totalLevels >= 1) { "totalLevels must be >= 1" }

        val configs = ArrayList<LevelConfig>(totalLevels)
        val blockSize = 10
        val sectorPlan = listOf(24, 20, 16, 12, 8, 6, 6, 6, 8, 10)

        for (level in 1..totalLevels) {
            val progress = if (totalLevels == 1) {
                0f
            } else {
                (level - 1).toFloat() / (totalLevels - 1).toFloat()
            }
            val firstFiveEase = if (level <= 5) {
                (6 - level).toFloat() / 5f
            } else {
                0f
            }
            val blockIndex = ((level - 1) / blockSize).coerceAtMost(sectorPlan.lastIndex)
            val inBlockProgress = ((level - 1) % blockSize) / (blockSize - 1f)
            val featureTier = blockIndex + 1
            val sectors = sectorPlan[blockIndex]
            val kitchenTier = featureTier >= 10

            val baseGapSectors = when (sectors) {
                24 -> 7
                20 -> 6
                16 -> 5
                12 -> 4
                else -> 3
            }

            val gapPressure = floor(progress * 2.4f).toInt()
            val inBlockPressure = if (inBlockProgress > 0.62f) 1 else 0
            val earlyGapPressure = if (level <= 10 && inBlockProgress > 0.36f) 1 else 0
            val firstFiveGapBonus = when (level) {
                1, 2 -> 3
                3, 4 -> 2
                5 -> 1
                else -> 0
            }

            val cadenceByTier = when (featureTier) {
                1 -> 1.06f
                2 -> 0.9f
                3 -> 0.84f
                4 -> 0.86f
                5 -> 0.74f
                6 -> 0.72f
                7 -> 0.69f
                8 -> 0.66f
                10 -> 0.75f
                else -> 0.64f
            }
            val earlySpawnSlowdown = 1f + firstFiveEase * 0.24f
            val kitchenSpawnEase = if (kitchenTier) 1.14f else 1f
            val spawnMin = lerp(1.78f, 0.74f, progress) * cadenceByTier * earlySpawnSlowdown * kitchenSpawnEase
            val spawnMax = (spawnMin + lerp(0.52f, 0.28f, progress)).coerceAtLeast(spawnMin + 0.12f)
            val maxDensity = (
                3 +
                    floor(progress * 4.4f).toInt() +
                    inBlockPressure +
                    if (featureTier >= 5) 1 else 0
                ).coerceAtMost(7)
            val laneShift = if (level <= 5) {
                1
            } else {
                (1 + floor(progress * 3f).toInt()).coerceAtMost(4)
            }
            val tierSpeedBoost = when (featureTier) {
                1 -> 0.98f
                2 -> 1.18f
                3 -> 1.26f
                4 -> 1.22f
                5 -> 1.3f
                6 -> 1.34f
                7 -> 1.38f
                8 -> 1.42f
                10 -> 1.34f
                else -> 1.46f
            }
            val tierGapMultiplier = when (featureTier) {
                1 -> 1.08f
                2 -> 1.02f
                3 -> 0.98f
                4 -> 0.94f
                5 -> 0.9f
                6 -> 0.88f
                7 -> 0.9f
                8 -> 0.86f
                10 -> 0.92f
                else -> 0.84f
            }
            val thicknessMultiplier = when (featureTier) {
                3 -> 0.94f
                4 -> 0.76f // Mini mode block.
                5 -> 0.88f
                6 -> 0.86f
                7 -> 0.84f
                8 -> 0.82f
                9 -> 0.8f
                10 -> 0.85f
                else -> 0.8f
            }
            val earlySpeedMultiplier = 1f - firstFiveEase * 0.2f
            val earlyNeedleMultiplier = 1f - firstFiveEase * 0.18f
            val earlyThicknessMultiplier = 1f - firstFiveEase * 0.12f
            val earlyReadableGapBoost = 1f + firstFiveEase * 0.2f
            val earlyRadialGapBoost = 1f + firstFiveEase * 0.18f
            val earlyTargetDurationTrim = 1f - firstFiveEase * 0.16f
            val adjustedMaxDensity = when {
                level <= 5 -> maxDensity.coerceAtMost(2 + ((level - 1) / 2))
                kitchenTier -> maxDensity.coerceAtMost(4)
                featureTier >= 7 -> maxDensity.coerceAtMost(5)
                featureTier >= 6 -> maxDensity.coerceAtMost(6)
                else -> maxDensity
            }
            val readableTierFactor = when (featureTier) {
                5 -> 0.92f
                6 -> 0.98f
                7 -> 1.05f
                8 -> 1.08f
                10 -> 1.0f
                else -> 1.12f
            }
            val kitchenGapBonus = if (kitchenTier) 1 else 0
            val kitchenSpeedEase = if (kitchenTier) 0.93f else 1f

            val candidate = LevelConfig(
                index = level,
                sectorCount = sectors,
                gapSectorCount = (
                    baseGapSectors +
                        kitchenGapBonus +
                        firstFiveGapBonus -
                        gapPressure -
                        inBlockPressure -
                        earlyGapPressure
                    ).coerceAtLeast(1),
                featureTier = featureTier,
                baseObstacleSpeed = lerp(0.24f, 0.48f, progress) * tierSpeedBoost * earlySpeedMultiplier * kitchenSpeedEase,
                obstacleThickness = lerp(0.124f, 0.09f, progress) * thicknessMultiplier * earlyThicknessMultiplier,
                spawnIntervalMinSeconds = spawnMin,
                spawnIntervalMaxSeconds = spawnMax,
                targetSurvivalSeconds = lerp(12f, 30f, progress) * earlyTargetDurationTrim,
                needleAngularSpeedRad = (
                    lerp(132f, 252f, progress) *
                        earlyNeedleMultiplier *
                        (PI / 180.0)
                    ).toFloat(),
                minRadialGapBetweenRings = (
                    lerp(0.34f, 0.24f, progress) *
                        tierGapMultiplier *
                        earlyRadialGapBoost
                    ),
                targetRadialGapBetweenRings = (
                    lerp(0.46f, 0.31f, progress) *
                        tierGapMultiplier *
                        earlyRadialGapBoost
                    ),
                minReadableThreatGapSeconds = (
                    lerp(0.82f, 0.48f, progress) *
                        readableTierFactor *
                        earlyReadableGapBoost
                    ),
                maxConcurrentThreatDensity = adjustedMaxDensity,
                laneShiftSeverity = laneShift
            )
            val previous = configs.lastOrNull()
            configs.add(if (previous == null) candidate else enforceLinearProgression(previous, candidate))
        }

        return configs
    }

    private fun enforceLinearProgression(previous: LevelConfig, candidate: LevelConfig): LevelConfig {
        val minSpeedStep = 0.0008f
        val minNeedleStep = (0.8f * (PI / 180.0)).toFloat()
        val minTargetDurationStep = 0.08f
        val maxSpawnStep = 0.018f
        val maxGapStep = 0.0035f
        val maxReadableStep = 0.006f

        val linearGapSectorCount = if (candidate.index <= 5) {
            candidate.gapSectorCount
        } else {
            candidate.gapSectorCount.coerceAtMost(previous.gapSectorCount)
        }
        val linearMinGap = candidate.minRadialGapBetweenRings.coerceAtMost(previous.minRadialGapBetweenRings - maxGapStep)
        val linearTargetGap = candidate.targetRadialGapBetweenRings.coerceAtMost(previous.targetRadialGapBetweenRings - maxGapStep)
        val linearReadableGap = candidate.minReadableThreatGapSeconds.coerceAtMost(previous.minReadableThreatGapSeconds - maxReadableStep)
        val linearSpawnMin = candidate.spawnIntervalMinSeconds.coerceAtMost(previous.spawnIntervalMinSeconds - maxSpawnStep)
        val linearSpawnMax = candidate.spawnIntervalMaxSeconds.coerceAtMost(previous.spawnIntervalMaxSeconds - maxSpawnStep)

        val resolvedMinGap = linearMinGap.coerceAtLeast(0.2f)
        val resolvedTargetGap = linearTargetGap.coerceAtLeast(resolvedMinGap + 0.04f)
        val resolvedSpawnMin = linearSpawnMin.coerceAtLeast(0.52f)
        val resolvedSpawnMax = linearSpawnMax.coerceAtLeast(resolvedSpawnMin + 0.12f)

        return candidate.copy(
            gapSectorCount = linearGapSectorCount.coerceAtLeast(1),
            baseObstacleSpeed = candidate.baseObstacleSpeed.coerceAtLeast(previous.baseObstacleSpeed + minSpeedStep),
            spawnIntervalMinSeconds = resolvedSpawnMin,
            spawnIntervalMaxSeconds = resolvedSpawnMax,
            targetSurvivalSeconds = candidate.targetSurvivalSeconds.coerceAtLeast(previous.targetSurvivalSeconds + minTargetDurationStep),
            needleAngularSpeedRad = candidate.needleAngularSpeedRad.coerceAtLeast(previous.needleAngularSpeedRad + minNeedleStep),
            minRadialGapBetweenRings = resolvedMinGap,
            targetRadialGapBetweenRings = resolvedTargetGap,
            minReadableThreatGapSeconds = linearReadableGap.coerceAtLeast(0.34f),
            maxConcurrentThreatDensity = candidate.maxConcurrentThreatDensity.coerceAtLeast(previous.maxConcurrentThreatDensity),
            laneShiftSeverity = candidate.laneShiftSeverity.coerceAtLeast(previous.laneShiftSeverity)
        )
    }

    private fun lerp(from: Float, to: Float, t: Float): Float {
        val clamped = t.coerceIn(0f, 1f)
        return from + (to - from) * clamped
    }
}
