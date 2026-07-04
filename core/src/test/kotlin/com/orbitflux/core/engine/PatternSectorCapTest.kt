package com.orbitflux.core.engine

import kotlin.test.Test
import kotlin.test.assertTrue

class PatternSectorCapTest {
    @Test
    fun hardModeNeverExceedsHexagon() {
        val level = LevelCatalog.create(60)[59]
        val generator = PatternGenerator(level, 778899L)

        repeat(120) {
            val obstacle = generator.nextObstacle(1.2f, 1.4f)
            assertTrue(obstacle.sectorCount in 3..8)
        }
    }
}
