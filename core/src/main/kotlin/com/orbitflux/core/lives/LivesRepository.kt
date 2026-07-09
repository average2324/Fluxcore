package com.luminadigitale.fluxcore.core.lives

import com.badlogic.gdx.Preferences

data class LivesState(
    val lives: Int,
    val lastUpdatedEpochSeconds: Long
)

interface LivesRepository {
    fun load(): LivesState?
    fun save(state: LivesState)
}

class InMemoryLivesRepository(
    initialState: LivesState? = null
) : LivesRepository {
    private var state: LivesState? = initialState

    override fun load(): LivesState? = state

    override fun save(state: LivesState) {
        this.state = state
    }
}

class PreferencesLivesRepository(
    private val preferences: Preferences
) : LivesRepository {
    companion object {
        private const val KEY_LIVES = "lives"
        private const val KEY_UPDATED_AT = "lives_updated_at"
    }

    override fun load(): LivesState? {
        if (!preferences.contains(KEY_LIVES) || !preferences.contains(KEY_UPDATED_AT)) {
            return null
        }
        return LivesState(
            lives = preferences.getInteger(KEY_LIVES),
            lastUpdatedEpochSeconds = preferences.getLong(KEY_UPDATED_AT)
        )
    }

    override fun save(state: LivesState) {
        preferences.putInteger(KEY_LIVES, state.lives)
        preferences.putLong(KEY_UPDATED_AT, state.lastUpdatedEpochSeconds)
        preferences.flush()
    }
}
