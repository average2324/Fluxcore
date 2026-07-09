package com.luminadigitale.fluxcore.core.engine

import com.badlogic.gdx.math.MathUtils
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertTrue

class ThreatTransitionReadabilityTest {
    private data class Snapshot(
        val arrivalSeconds: Float,
        val gapCenterAtArrivalRad: Float,
        val halfGapRad: Float
    )

    @Test
    fun consecutiveThreatsLeaveReachableRotationWindow() {
        val simulation = GameSimulation(LevelCatalog.create(60), 55443322L)
        simulation.setLevel(57)
        simulation.startRun()

        var simulationTime = 0f
        var previousObstacleCount = simulation.obstacles.size
        var previous: Snapshot? = null
        var checkedPairs = 0

        for (step in 0 until 7200) {
            if (simulation.runPhase != RunPhase.RUNNING) {
                break
            }

            val input = GameSimulation.PlayerInput(
                holdDirection = if (step % 120 < 60) 1 else -1,
                stepDirection = if (step % 14 == 0) 1 else 0
            )
            simulation.step(GameSimulation.FIXED_TIMESTEP_SECONDS, input)
            simulationTime += GameSimulation.FIXED_TIMESTEP_SECONDS

            if (simulation.obstacles.size > previousObstacleCount) {
                val newest = simulation.obstacles.maxByOrNull { it.radius } ?: continue
                val travelSeconds = ((newest.radius - GameSimulation.PLAYER_COLLIDER.radius).coerceAtLeast(0f) /
                    newest.speed.coerceAtLeast(0.01f))
                val arrival = simulationTime + travelSeconds
                val sectorAngle = MathUtils.PI2 / newest.sectorCount.toFloat()
                val centerAtSpawn = normalize(
                    newest.rotationRad + (newest.gapStartSector + newest.gapSectorCount * 0.5f) * sectorAngle
                )
                val current = Snapshot(
                    arrivalSeconds = arrival,
                    gapCenterAtArrivalRad = normalize(centerAtSpawn + newest.spinRadPerSecond * travelSeconds),
                    halfGapRad = newest.gapSectorCount * sectorAngle * 0.5f
                )

                previous?.let { prior ->
                    val travelWindow = current.arrivalSeconds - prior.arrivalSeconds
                    val rotationalDelta = abs(shortestDelta(prior.gapCenterAtArrivalRad, current.gapCenterAtArrivalRad))
                    val forgivingGap = (prior.halfGapRad + current.halfGapRad) * 0.62f
                    val requiredTravel = (rotationalDelta - forgivingGap + GameSimulation.PLAYER_COLLIDER.halfArcWidthRad * 0.9f)
                        .coerceAtLeast(0f)
                    val effectiveSpeed = if (simulation.usesStepMovement) {
                        simulation.levelConfig.needleAngularSpeedRad * 2.8f
                    } else {
                        simulation.levelConfig.needleAngularSpeedRad * 1.45f
                    }.coerceAtLeast(0.01f)
                    val requiredWindow = 0.12f + requiredTravel / effectiveSpeed
                    assertTrue(
                        travelWindow + 0.02f >= requiredWindow,
                        "travelWindow=$travelWindow requiredWindow=$requiredWindow"
                    )
                    checkedPairs += 1
                }
                previous = current
                if (checkedPairs >= 8) {
                    break
                }
            }

            previousObstacleCount = simulation.obstacles.size
        }

        assertTrue(checkedPairs >= 1, "Expected at least 1 checked threat pair, found $checkedPairs")
    }

    private fun normalize(value: Float): Float {
        var angle = value % MathUtils.PI2
        if (angle < 0f) {
            angle += MathUtils.PI2
        }
        return angle
    }

    private fun shortestDelta(from: Float, to: Float): Float {
        var delta = normalize(to - from)
        if (delta > MathUtils.PI) {
            delta -= MathUtils.PI2
        }
        return delta
    }
}
