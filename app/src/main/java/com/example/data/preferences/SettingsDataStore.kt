package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class AppThemeMode {
    DARK,
    LIGHT,
    SYSTEM,
    OBSIDIAN_VIOLET, // Premium exclusive
    ROYAL_GOLD       // Premium exclusive
}

enum class AppLanguage(val code: String, val nativeLabel: String) {
    ENGLISH("en", "English"),
    HINDI("hi", "हिन्दी (Hindi)"),
    MARATHI("mr", "मराठी (Marathi)")
}

data class UserSettings(
    val themeMode: AppThemeMode = AppThemeMode.DARK,
    val language: AppLanguage = AppLanguage.ENGLISH,
    val decimalPrecision: Int = 6,
    val thousandsSeparator: Boolean = true,
    val soundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val hasCompletedOnboarding: Boolean = false,
    val memoryValue: String = "0",
    val hasMemory: Boolean = false
)

private val Context.calcProDataStore: DataStore<Preferences> by preferencesDataStore(name = "calcpro_settings")

class SettingsDataStore(private val context: Context) {

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val LANGUAGE_CODE = stringPreferencesKey("language_code")
        val DECIMAL_PRECISION = intPreferencesKey("decimal_precision")
        val THOUSANDS_SEPARATOR = booleanPreferencesKey("thousands_separator")
        val SOUND_ENABLED = booleanPreferencesKey("sound_enabled")
        val VIBRATION_ENABLED = booleanPreferencesKey("vibration_enabled")
        val ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")
        val MEMORY_VALUE = stringPreferencesKey("memory_value")
        val HAS_MEMORY = booleanPreferencesKey("has_memory")
    }

    val settingsFlow: Flow<UserSettings> = context.calcProDataStore.data.map { prefs ->
        val themeName = prefs[Keys.THEME_MODE] ?: AppThemeMode.DARK.name
        val theme = runCatching { AppThemeMode.valueOf(themeName) }.getOrDefault(AppThemeMode.DARK)

        val langCode = prefs[Keys.LANGUAGE_CODE] ?: AppLanguage.ENGLISH.code
        val lang = AppLanguage.entries.firstOrNull { it.code == langCode } ?: AppLanguage.ENGLISH

        UserSettings(
            themeMode = theme,
            language = lang,
            decimalPrecision = (prefs[Keys.DECIMAL_PRECISION] ?: 6).coerceIn(0, 10),
            thousandsSeparator = prefs[Keys.THOUSANDS_SEPARATOR] ?: true,
            soundEnabled = prefs[Keys.SOUND_ENABLED] ?: true,
            vibrationEnabled = prefs[Keys.VIBRATION_ENABLED] ?: true,
            hasCompletedOnboarding = prefs[Keys.ONBOARDING_DONE] ?: false,
            memoryValue = prefs[Keys.MEMORY_VALUE] ?: "0",
            hasMemory = prefs[Keys.HAS_MEMORY] ?: false
        )
    }

    suspend fun setThemeMode(mode: AppThemeMode) {
        context.calcProDataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    suspend fun setLanguage(language: AppLanguage) {
        context.calcProDataStore.edit { it[Keys.LANGUAGE_CODE] = language.code }
    }

    suspend fun setDecimalPrecision(precision: Int) {
        context.calcProDataStore.edit { it[Keys.DECIMAL_PRECISION] = precision.coerceIn(0, 10) }
    }

    suspend fun setThousandsSeparator(enabled: Boolean) {
        context.calcProDataStore.edit { it[Keys.THOUSANDS_SEPARATOR] = enabled }
    }

    suspend fun setSoundEnabled(enabled: Boolean) {
        context.calcProDataStore.edit { it[Keys.SOUND_ENABLED] = enabled }
    }

    suspend fun setVibrationEnabled(enabled: Boolean) {
        context.calcProDataStore.edit { it[Keys.VIBRATION_ENABLED] = enabled }
    }

    suspend fun setOnboardingCompleted(done: Boolean) {
        context.calcProDataStore.edit { it[Keys.ONBOARDING_DONE] = done }
    }

    suspend fun setMemory(value: String, hasMemory: Boolean) {
        context.calcProDataStore.edit {
            it[Keys.MEMORY_VALUE] = value
            it[Keys.HAS_MEMORY] = hasMemory
        }
    }
}
