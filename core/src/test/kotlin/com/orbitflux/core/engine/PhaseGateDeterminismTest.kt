package com.orbitflux.core.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PhaseGateDeterminismTest {
    @Test
    fun phaseGateStateIsDeterministicForSameSeed() {
        val levels = LevelCatalog.create(60)
        val simulationA = GameSimulation(levels, 424242L)
        val simulationB = GameSimulation(levels, 424242L)

        simulationA.setLevel(24)
        simulationB.setLevel(24)
        simulationA.startRun()
        simulationB.startRun()

        repeat(900) { step ->
            val input = if (step % 90 < 45) 1 else -1
            simulationA.step(GameSimulation.FIXED_TIMESTEP_SECONDS, input)
            simulationB.step(GameSimulation.FIXED_TIMESTEP_SECONDS, input)

            assertEquals(simulationA.phaseGateStatus, simulationB.phaseGateStatus)
            assertTrue(simulationA.phaseGateStatus.directionMultiplier == 1 || simulationA.phaseGateStatus.directionMultiplier == -1)

            if (simulationA.runPhase != RunPhase.RUNNING || simulationB.runPhase != RunPhase.RUNNING) {
                assertEquals(simulationA.runPhase, simulationB.runPhase)
                return
            }
        }
    }
}
