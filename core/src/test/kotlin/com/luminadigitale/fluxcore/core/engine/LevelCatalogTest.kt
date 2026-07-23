package com.luminadigitale.fluxcore.core.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LevelCatalogTest {
    @Test
    fun levelDifficultyIncreasesAcrossCampaign() {
        val levels = LevelCatalog.create(100)

        assertEquals(100, levels.size)
        assertTrue(levels.first().baseObstacleSpeed < levels.last().baseObstacleSpeed)
        assertTrue(levels.first().targetSurvivalSeconds < levels.last().targetSurvivalSeconds)
        assertTrue(levels.first().spawnIntervalMinSeconds > levels[9].spawnIntervalMinSeconds)
        assertTrue(levels.first().needleAngularSpeedRad < levels.last().needleAngularSpeedRad)
    }

    @Test
    fun arenaShapeProgression_tracksAllTenCampaignBlocks() {
        val levels = LevelCatalog.create(100)
        val expected = listOf(24, 20, 16, 12, 8, 6, 6, 6, 8, 10)

        expected.forEachIndexed { blockIndex, sectorCount ->
            val block = levels.subList(blockIndex * 10, blockIndex * 10 + 10)
            block.forEach {
                assertEquals(sectorCount, it.sectorCount)
                assertEquals(blockIndex + 1, it.featureTier)
            }
        }
    }

    @Test
    fun firstFiveLevels_areBeginnerFriendly() {
        val levels = LevelCatalog.create(100)
        val firstFive = levels.take(5)

        assertTrue(firstFive.all { it.maxConcurrentThreatDensity <= 4 })
        assertTrue(firstFive.all { it.laneShiftSeverity == 1 })
        assertTrue(firstFive.first().gapSectorCount > levels[9].gapSectorCount)
        assertTrue(firstFive.first().spawnIntervalMinSeconds > levels[9].spawnIntervalMinSeconds)
        assertTrue(firstFive.first().baseObstacleSpeed < levels[9].baseObstacleSpeed)
        assertTrue(firstFive.first().needleAngularSpeedRad < levels[9].needleAngularSpeedRad)
    }

    @Test
    fun difficultyCurve_remainsMonotonicAcrossLevels() {
        val levels = LevelCatalog.create(100)

        for (index in 1 until levels.size) {
            val previous = levels[index - 1]
            val current = levels[index]

            assertTrue(current.baseObstacleSpeed >= previous.baseObstacleSpeed, "speed regressed at level ${current.index}")
            assertTrue(current.spawnIntervalMinSeconds <= previous.spawnIntervalMinSeconds, "spawn min regressed at level ${current.index}")
            assertTrue(current.spawnIntervalMaxSeconds <= previous.spawnIntervalMaxSeconds, "spawn max regressed at level ${current.index}")
            assertTrue(current.targetSurvivalSeconds >= previous.targetSurvivalSeconds, "target duration regressed at level ${current.index}")
            assertTrue(current.needleAngularSpeedRad >= previous.needleAngularSpeedRad, "needle speed regressed at level ${current.index}")
            assertTrue(current.minRadialGapBetweenRings <= previous.minRadialGapBetweenRings, "min radial gap widened at level ${current.index}")
            assertTrue(current.targetRadialGapBetweenRings <= previous.targetRadialGapBetweenRings, "target radial gap widened at level ${current.index}")
            assertTrue(current.minReadableThreatGapSeconds <= previous.minReadableThreatGapSeconds, "readable gap widened at level ${current.index}")
            assertTrue(current.maxConcurrentThreatDensity >= previous.maxConcurrentThreatDensity, "density regressed at level ${current.index}")
            assertTrue(current.laneShiftSeverity >= previous.laneShiftSeverity, "lane shift regressed at level ${current.index}")
        }
    }
}
