package com.orbitflux.core.profile

import kotlin.test.Test
import kotlin.test.assertEquals

class BestScoreManagerTest {
    private class InMemoryBestScoreRepository(initial: BestScoreState? = null) : BestScoreRepository {
        private var state: BestScoreState? = initial

        override fun load(): BestScoreState? = state

        override fun save(state: BestScoreState) {
            this.state = state
        }
    }

    @Test
    fun registersBestScoreAndHighestLevel() {
        val repository = InMemoryBestScoreRepository()
        val manager = BestScoreManager(repository)

        manager.registerRunResult(levelIndex = 3, survivedSeconds = 18.2f, levelCleared = false)
        manager.registerRunResult(levelIndex = 4, survivedSeconds = 16.1f, levelCleared = true)

        val snapshot = manager.snapshot()
        assertEquals(18.2f, snapshot.bestSurvivalSeconds)
        assertEquals(5, snapshot.highestLevelCleared)
    }
}
