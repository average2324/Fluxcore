package com.orbitflux.core.profile

import com.badlogic.gdx.Preferences
import java.util.Locale

enum class AppLanguage {
    EN,
    TR
}

enum class GameDifficulty {
    RELAXED,
    STANDARD,
    EXPERT;

    fun next(): GameDifficulty {
        return when (this) {
            RELAXED -> STANDARD
            STANDARD -> EXPERT
            EXPERT -> RELAXED
        }
    }
}

data class SettingsState(
    val soundEnabled: Boolean,
    val musicVolume: Float,
    val effectsVolume: Float,
    val hapticsEnabled: Boolean,
    val language: AppLanguage,
    val difficulty: GameDifficulty
)

interface SettingsRepository {
    fun load(): SettingsState?
    fun save(state: SettingsState)
}

class PreferencesSettingsRepository(
    private val preferences: Preferences
) : SettingsRepository {
    companion object {
        private const val KEY_SOUND = "settings_sound"
        private const val KEY_MUSIC_VOLUME = "settings_music_volume"
        private const val KEY_EFFECTS_VOLUME = "settings_effects_volume"
        private const val KEY_HAPTICS = "settings_haptics"
        private const val KEY_LANGUAGE = "settings_language"
        private const val KEY_DIFFICULTY = "settings_difficulty"
        private const val DEFAULT_MUSIC_VOLUME = 0.42f
        private const val DEFAULT_EFFECTS_VOLUME = 0.85f
    }

    override fun load(): SettingsState? {
        if (!preferences.contains(KEY_SOUND) || !preferences.contains(KEY_HAPTICS) || !preferences.contains(KEY_LANGUAGE)) {
            return null
        }
        return SettingsState(
            soundEnabled = preferences.getBoolean(KEY_SOUND),
            musicVolume = preferences.getFloat(KEY_MUSIC_VOLUME, DEFAULT_MUSIC_VOLUME).coerceIn(0f, 1f),
            effectsVolume = preferences.getFloat(KEY_EFFECTS_VOLUME, DEFAULT_EFFECTS_VOLUME).coerceIn(0f, 1f),
            hapticsEnabled = preferences.getBoolean(KEY_HAPTICS),
            language = runCatching {
                AppLanguage.valueOf(preferences.getString(KEY_LANGUAGE, AppLanguage.EN.name))
            }.getOrDefault(AppLanguage.EN),
            difficulty = runCatching {
                GameDifficulty.valueOf(preferences.getString(KEY_DIFFICULTY, GameDifficulty.STANDARD.name))
            }.getOrDefault(GameDifficulty.STANDARD)
        )
    }

    override fun save(state: SettingsState) {
        preferences.putBoolean(KEY_SOUND, state.soundEnabled)
        preferences.putFloat(KEY_MUSIC_VOLUME, state.musicVolume.coerceIn(0f, 1f))
        preferences.putFloat(KEY_EFFECTS_VOLUME, state.effectsVolume.coerceIn(0f, 1f))
        preferences.putBoolean(KEY_HAPTICS, state.hapticsEnabled)
        preferences.putString(KEY_LANGUAGE, state.language.name)
        preferences.putString(KEY_DIFFICULTY, state.difficulty.name)
        preferences.flush()
    }
}

class SettingsManager(
    private val repository: SettingsRepository,
    private val localeProvider: () -> Locale = { Locale.getDefault() }
) {
    private var state: SettingsState = repository.load() ?: SettingsState(
        soundEnabled = true,
        musicVolume = 0.42f,
        effectsVolume = 0.85f,
        hapticsEnabled = true,
        language = if (localeProvider().language.equals("tr", ignoreCase = true)) {
            AppLanguage.TR
        } else {
            AppLanguage.EN
        },
        difficulty = GameDifficulty.STANDARD
    )

    fun snapshot(): SettingsState = state

    fun toggleSound(): SettingsState {
        state = state.copy(soundEnabled = !state.soundEnabled)
        repository.save(state)
        return state
    }

    fun cycleMusicVolume(): SettingsState {
        state = state.copy(musicVolume = nextVolumeStep(state.musicVolume))
        repository.save(state)
        return state
    }

    fun cycleEffectsVolume(): SettingsState {
        state = state.copy(effectsVolume = nextVolumeStep(state.effectsVolume))
        repository.save(state)
        return state
    }

    fun setMusicVolume(value: Float): SettingsState {
        val clamped = value.coerceIn(0f, 1f)
        if (kotlin.math.abs(clamped - state.musicVolume) < 0.001f) {
            return state
        }
        state = state.copy(musicVolume = clamped)
        repository.save(state)
        return state
    }

    fun setEffectsVolume(value: Float): SettingsState {
        val clamped = value.coerceIn(0f, 1f)
        if (kotlin.math.abs(clamped - state.effectsVolume) < 0.001f) {
            return state
        }
        state = state.copy(effectsVolume = clamped)
        repository.save(state)
        return state
    }

    fun toggleHaptics(): SettingsState {
        state = state.copy(hapticsEnabled = !state.hapticsEnabled)
        repository.save(state)
        return state
    }

    fun toggleLanguage(): SettingsState {
        state = state.copy(
            language = if (state.language == AppLanguage.EN) AppLanguage.TR else AppLanguage.EN
        )
        repository.save(state)
        return state
    }

    fun cycleDifficulty(): SettingsState {
        state = state.copy(difficulty = state.difficulty.next())
        repository.save(state)
        return state
    }

    private fun nextVolumeStep(current: Float): Float {
        val step = 0.1f
        val stepIndex = ((current.coerceIn(0f, 1f) / step) + 0.5f).toInt()
        val next = if (stepIndex >= 10) 0 else stepIndex + 1
        return (next * step).coerceIn(0f, 1f)
    }
}
