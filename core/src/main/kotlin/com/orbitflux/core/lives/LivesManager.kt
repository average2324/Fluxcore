package com.orbitflux.core.lives

class LivesManager(
    private val repository: LivesRepository,
    private val maxLives: Int = 5,
    private val refillIntervalSeconds: Long = 3_600L
) {
    private var state: LivesState = repository.load() ?: LivesState(
        lives = maxLives,
        lastUpdatedEpochSeconds = 0L
    )

    fun snapshot(nowEpochSeconds: Long): LivesState {
        refresh(nowEpochSeconds)
        return state
    }

    fun consumeLifeForAttempt(nowEpochSeconds: Long): Boolean {
        refresh(nowEpochSeconds)
        if (state.lives <= 0) {
            return false
        }

        state = state.copy(
            lives = state.lives - 1,
            lastUpdatedEpochSeconds = nowEpochSeconds
        )
        repository.save(state)
        return true
    }

    fun grantLife(nowEpochSeconds: Long, amount: Int = 1) {
        refresh(nowEpochSeconds)
        val updatedLives = (state.lives + amount).coerceAtMost(maxLives)
        state = state.copy(
            lives = updatedLives,
            lastUpdatedEpochSeconds = nowEpochSeconds
        )
        repository.save(state)
    }

    fun secondsUntilNextLife(nowEpochSeconds: Long): Long {
        refresh(nowEpochSeconds)
        if (state.lives >= maxLives) {
            return 0L
        }

        val elapsed = (nowEpochSeconds - state.lastUpdatedEpochSeconds).coerceAtLeast(0L)
        val remaining = refillIntervalSeconds - (elapsed % refillIntervalSeconds)
        return remaining.coerceAtLeast(1L)
    }

    private fun refresh(nowEpochSeconds: Long) {
        val safeNow = nowEpochSeconds.coerceAtLeast(0L)
        if (state.lastUpdatedEpochSeconds == 0L) {
            state = state.copy(lastUpdatedEpochSeconds = safeNow)
            repository.save(state)
            return
        }

        if (state.lives >= maxLives) {
            return
        }

        val elapsed = (safeNow - state.lastUpdatedEpochSeconds).coerceAtLeast(0L)
        if (elapsed < refillIntervalSeconds) {
            return
        }

        val livesToAdd = (elapsed / refillIntervalSeconds).toInt()
        val remainder = elapsed % refillIntervalSeconds

        state = state.copy(
            lives = (state.lives + livesToAdd).coerceAtMost(maxLives),
            lastUpdatedEpochSeconds = safeNow - remainder
        )
        repository.save(state)
    }
}
