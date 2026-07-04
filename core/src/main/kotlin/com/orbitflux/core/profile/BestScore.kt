package com.orbitflux.core.profile

import com.badlogic.gdx.Preferences

data class BestScoreState(
    val bestSurvivalSeconds: Float,
    val highestLevelCleared: Int
)

interface BestScoreRepository {
    fun load(): BestScoreState?
    fun save(state: BestScoreState)
}

class PreferencesBestScoreRepository(
    private val preferences: Preferences
) : BestScoreRepository {
    companion object {
        private const val KEY_BEST_SECONDS = "best_survival_seconds"
        private const val KEY_BEST_LEVEL = "highest_level_cleared"
    }

    override fun load(): BestScoreState? {
        if (!preferences.contains(KEY_BEST_SECONDS) || !preferences.contains(KEY_BEST_LEVEL)) {
            return null
        }
        return BestScoreState(
            bestSurvivalSeconds = preferences.getFloat(KEY_BEST_SECONDS),
            highestLevelCleared = preferences.getInteger(KEY_BEST_LEVEL)
        )
    }

    override fun save(state: BestScoreState) {
        preferences.putFloat(KEY_BEST_SECONDS, state.bestSurvivalSeconds)
        preferences.putInteger(KEY_BEST_LEVEL, state.highestLevelCleared)
        preferences.flush()
    }
}

class BestScoreManager(
    private val repository: BestScoreRepository
) {
    private var state: BestScoreState = repository.load() ?: BestScoreState(
        bestSurvivalSeconds = 0f,
        highestLevelCleared = 0
    )

    fun snapshot(): BestScoreState = state

    fun registerRunResult(
        levelIndex: Int,
        survivedSeconds: Float,
        levelCleared: Boolean
    ): BestScoreState {
        val clearedLevel = if (levelCleared) levelIndex + 1 else levelIndex
        val next = BestScoreState(
            bestSurvivalSeconds = maxOf(state.bestSurvivalSeconds, survivedSeconds),
            highestLevelCleared = maxOf(state.highestLevelCleared, clearedLevel)
        )

        if (next != state) {
            state = next
            repository.save(state)
        }

        return state
    }
}
