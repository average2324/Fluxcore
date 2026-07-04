package com.orbitflux.core.engine

import kotlin.test.Test
import kotlin.test.assertTrue

class SpawnReadabilityTest {
    @Test
    fun generatedThreatsMaintainReadableSpacing() {
        val simulation = GameSimulation(LevelCatalog.create(60), 99112233L)
        simulation.setLevel(24)
        simulation.startRun()

        var simulationTime = 0f
        var previousObstacleCount = simulation.obstacles.size
        val arrivals = ArrayList<Float>(8)
        val minGap = simulation.levelConfig.minRadialGapBetweenRings * 0.74f
        val minReadableSeconds = simulation.levelConfig.minReadableThreatGapSeconds * 0.68f

        for (step in 0 until 4200) {
            if (simulation.runPhase != RunPhase.RUNNING) {
                break
            }

            val input = GameSimulation.PlayerInput(
                holdDirection = if (step % 180 < 90) 1 else -1,
                stepDirection = if (step % 18 == 0) 1 else 0
            )
            simulation.step(GameSimulation.FIXED_TIMESTEP_SECONDS, input)
            simulationTime += GameSimulation.FIXED_TIMESTEP_SECONDS

            if (simulation.obstacles.size > previousObstacleCount) {
                val rings = simulation.obstacles.sortedByDescending { it.radius }
                if (rings.size >= 2) {
                    val radialGap = rings[0].radius - rings[1].radius
                    assertTrue(radialGap >= minGap, "radialGap=$radialGap minGap=$minGap")
                }

                val spawned = rings.first()
                val arrival = simulationTime + (
                    (spawned.radius - GameSimulation.PLAYER_COLLIDER.radius).coerceAtLeast(0f) /
                        spawned.speed.coerceAtLeast(0.01f)
                    )
                if (arrivals.isNotEmpty()) {
                    val readableGap = arrival - arrivals.last()
                    assertTrue(
                        readableGap >= minReadableSeconds,
                        "readableGap=$readableGap minReadableSeconds=$minReadableSeconds"
                    )
                }
                arrivals.add(arrival)
                if (arrivals.size >= 6) {
                    break
                }
            }

            previousObstacleCount = simulation.obstacles.size
        }

        assertTrue(arrivals.size >= 2, "Expected at least 2 recorded threats, found ${arrivals.size}")
    }
}
