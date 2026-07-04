package com.orbitflux.core.engine

import kotlin.math.PI
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DrainPhaseCompletionTest {
    @Test
    fun levelDoesNotClearImmediatelyAfterTargetWhenThreatsRemain() {
        val level = LevelConfig(
            index = 1,
            sectorCount = 24,
            gapSectorCount = 7,
            featureTier = 1,
            baseObstacleSpeed = 0.08f,
            obstacleThickness = 0.11f,
            spawnIntervalMinSeconds = 1.4f,
            spawnIntervalMaxSeconds = 1.4f,
            targetSurvivalSeconds = 0.5f,
            needleAngularSpeedRad = (150.0 * (PI / 180.0)).toFloat(),
            minRadialGapBetweenRings = 0.22f,
            targetRadialGapBetweenRings = 0.28f,
            minReadableThreatGapSeconds = 0.4f,
            maxConcurrentThreatDensity = 4,
            laneShiftSeverity = 1
        )
        val simulation = GameSimulation(listOf(level), 44556677L)
        simulation.startRun()

        for (step in 0 until 40) {
            simulation.step(GameSimulation.FIXED_TIMESTEP_SECONDS, GameSimulation.PlayerInput())
            if (simulation.runPhase == RunPhase.DRAINING) {
                break
            }
        }

        assertEquals(RunPhase.DRAINING, simulation.runPhase)
        assertTrue(simulation.activeLethalThreatCount > 0)
        assertTrue(simulation.runPhase != RunPhase.LEVEL_CLEARED)
    }
}
