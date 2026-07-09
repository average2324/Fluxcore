package com.luminadigitale.fluxcore.core.lives

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LivesManagerTest {
    @Test
    fun consumeAndRefillLives_hourly() {
        val repository = InMemoryLivesRepository(LivesState(lives = 2, lastUpdatedEpochSeconds = 0L))
        val manager = LivesManager(repository, maxLives = 5, refillIntervalSeconds = 3600)

        assertTrue(manager.consumeLifeForAttempt(1000L))
        assertTrue(manager.consumeLifeForAttempt(1001L))
        assertFalse(manager.consumeLifeForAttempt(1002L))

        val snapshotAfterRefill = manager.snapshot(1001L + 3600L)
        assertEquals(1, snapshotAfterRefill.lives)
    }
}
