package com.orbitflux.core.engine

import kotlin.test.Test
import kotlin.test.assertEquals

class PatternGeneratorDeterminismTest {
    @Test
    fun patternGenerator_isDeterministicForSameSeed() {
        val level = LevelCatalog.create(60)[10]
        val generatorA = PatternGenerator(level, 123456789L)
        val generatorB = PatternGenerator(level, 123456789L)

        repeat(20) {
            val obstacleA = generatorA.nextObstacle(1.2f, 1.15f)
            val obstacleB = generatorB.nextObstacle(1.2f, 1.15f)
            assertEquals(obstacleA, obstacleB)
            assertEquals(generatorA.nextSpawnInterval(1.15f), generatorB.nextSpawnInterval(1.15f))
        }
    }
}
