package com.orbitflux.core.profile

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import java.util.Locale

class SettingsManagerTest {
    private class InMemorySettingsRepository(initial: SettingsState? = null) : SettingsRepository {
        private var state: SettingsState? = initial

        override fun load(): SettingsState? = state

        override fun save(state: SettingsState) {
            this.state = state
        }
    }

    @Test
    fun togglesSettingsPersisted() {
        val repository = InMemorySettingsRepository(
            SettingsState(
                soundEnabled = true,
                musicVolume = 0.4f,
                effectsVolume = 0.8f,
                hapticsEnabled = true,
                language = AppLanguage.EN,
                difficulty = GameDifficulty.STANDARD
            )
        )
        val manager = SettingsManager(repository)

        val afterSound = manager.toggleSound()
        val afterHaptics = manager.toggleHaptics()
        val afterLanguage = manager.toggleLanguage()
        val afterDifficulty = manager.cycleDifficulty()

        assertFalse(afterSound.soundEnabled)
        assertFalse(afterHaptics.hapticsEnabled)
        assertTrue(afterLanguage.language == AppLanguage.TR)
        assertTrue(afterDifficulty.difficulty == GameDifficulty.EXPERT)
        assertFalse(manager.snapshot().soundEnabled)
        assertFalse(manager.snapshot().hapticsEnabled)
        assertTrue(manager.snapshot().language == AppLanguage.TR)
        assertTrue(manager.snapshot().difficulty == GameDifficulty.EXPERT)
    }

    @Test
    fun defaultsToTurkishWhenLocaleIsTurkish() {
        val repository = InMemorySettingsRepository(initial = null)
        val manager = SettingsManager(
            repository = repository,
            localeProvider = { Locale("tr", "TR") }
        )

        assertTrue(manager.snapshot().language == AppLanguage.TR)
    }

    @Test
    fun defaultsToEnglishWhenLocaleIsNotTurkish() {
        val repository = InMemorySettingsRepository(initial = null)
        val manager = SettingsManager(
            repository = repository,
            localeProvider = { Locale.US }
        )

        assertTrue(manager.snapshot().language == AppLanguage.EN)
    }
}
