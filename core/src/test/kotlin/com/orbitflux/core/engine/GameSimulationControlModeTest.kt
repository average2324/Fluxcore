package com.orbitflux.core.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GameSimulationControlModeTest {
    @Test
    fun roundLevelsUseFreeMovement() {
        val simulation = GameSimulation(LevelCatalog.create(60), 4242L)
        simulation.setLevel(0)
        simulation.startRun()

        assertTrue(!simulation.usesStepMovement)
        assertTrue(!simulation.hasReversePhaseGate)
    }

    @Test
    fun lowSectorLevelsUseFreeMovementAndIgnoreStepInput() {
        val neutralSimulation = GameSimulation(LevelCatalog.create(60), 4242L)
        neutralSimulation.setLevel(52)
        neutralSimulation.startRun()

        val stepSimulation = GameSimulation(LevelCatalog.create(60), 4242L)
        stepSimulation.setLevel(52)
        stepSimulation.startRun()

        assertTrue(!neutralSimulation.usesStepMovement)

        neutralSimulation.step(
            GameSimulation.FIXED_TIMESTEP_SECONDS,
            GameSimulation.PlayerInput()
        )
        stepSimulation.step(
            GameSimulation.FIXED_TIMESTEP_SECONDS,
            GameSimulation.PlayerInput(stepDirection = 1)
        )

        // Step input should not affect free-movement levels.
        assertEquals(neutralSimulation.playerAngleRad, stepSimulation.playerAngleRad, 0.0001f)
        assertEquals(neutralSimulation.arenaRotationRad, stepSimulation.arenaRotationRad, 0.0001f)

        val beforeHoldLocalAngle = neutralSimulation.playerAngleRad - neutralSimulation.arenaRotationRad
        neutralSimulation.step(
            GameSimulation.FIXED_TIMESTEP_SECONDS,
            GameSimulation.PlayerInput(holdDirection = 1)
        )
        val afterHoldLocalAngle = neutralSimulation.playerAngleRad - neutralSimulation.arenaRotationRad

        assertTrue(afterHoldLocalAngle > beforeHoldLocalAngle)
    }

    @Test
    fun firstBlockKeepsDirectInputWithoutReversePhase() {
        val simulation = GameSimulation(LevelCatalog.create(60), 4242L)
        simulation.setLevel(5)
        simulation.startRun()

        repeat(720) {
            simulation.step(GameSimulation.FIXED_TIMESTEP_SECONDS, GameSimulation.PlayerInput(holdDirection = 1))
            assertEquals(1, simulation.phaseGateStatus.directionMultiplier)
            assertTrue(!simulation.phaseGateStatus.active)
        }
    }
}
