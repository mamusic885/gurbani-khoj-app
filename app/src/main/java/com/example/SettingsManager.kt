package com.example

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.IOException

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "gurbani_settings_datastore_v3")

class SettingsManager(private val context: Context) {

    companion object {
        val FONT_SIZE_KEY = stringPreferencesKey("font_size")
        val FONT_FAMILY_KEY = stringPreferencesKey("font_family")
        val THEME_MODE_KEY = stringPreferencesKey("theme_mode")
        val LINE_SPACING_KEY = floatPreferencesKey("line_spacing")
        val VISHRAM_COLOR_KEY = stringPreferencesKey("vishram_color")
        val KEEP_SCREEN_ON_KEY = booleanPreferencesKey("keep_screen_on")
        val FIRST_LAUNCH_KEY = booleanPreferencesKey("is_first_launch_done")
        val SHOW_TRANSLATION_KEY = booleanPreferencesKey("show_translation")
        val SHOW_PUNJABI_TRANSLATION_KEY = booleanPreferencesKey("show_punjabi_translation")
    }

    private val scope = CoroutineScope(Dispatchers.IO)

    val settingsState: StateFlow<SettingsState> = context.settingsDataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { prefs ->
            SettingsState(
                fontSize = prefs[FONT_SIZE_KEY] ?: "Medium",
                fontFamily = prefs[FONT_FAMILY_KEY] ?: "Noto Serif Gurmukhi",
                themeMode = prefs[THEME_MODE_KEY] ?: "System",
                lineSpacing = prefs[LINE_SPACING_KEY] ?: 1.5f,
                vishramColor = prefs[VISHRAM_COLOR_KEY] ?: "#FF9933",
                keepScreenOn = prefs[KEEP_SCREEN_ON_KEY] ?: false,
                isFirstLaunchDone = prefs[FIRST_LAUNCH_KEY] ?: false,
                showTranslation = prefs[SHOW_TRANSLATION_KEY] ?: true,
                showPunjabiTranslation = prefs[SHOW_PUNJABI_TRANSLATION_KEY] ?: false
            )
        }
        .stateIn(
            scope = scope,
            started = SharingStarted.Eagerly,
            initialValue = SettingsState()
        )

    // Backward compatibility alias
    val settings: StateFlow<SettingsState> get() = settingsState

    fun updateFontSize(size: String?) {
        val safeSize = size ?: "Medium"
        scope.launch {
            try {
                context.settingsDataStore.edit { prefs ->
                    prefs[FONT_SIZE_KEY] = safeSize
                }
            } catch (_: Exception) {}
        }
    }

    fun updateFontFamily(family: String?) {
        val safeFamily = family ?: "Noto Serif Gurmukhi"
        scope.launch {
            try {
                context.settingsDataStore.edit { prefs ->
                    prefs[FONT_FAMILY_KEY] = safeFamily
                }
            } catch (_: Exception) {}
        }
    }

    fun updateThemeMode(mode: String?) {
        val safeMode = mode ?: "System"
        scope.launch {
            try {
                context.settingsDataStore.edit { prefs ->
                    prefs[THEME_MODE_KEY] = safeMode
                }
            } catch (_: Exception) {}
        }
    }

    fun updateLineSpacing(spacing: Float) {
        val safeSpacing = if (spacing in 1.0f..3.0f) spacing else 1.5f
        scope.launch {
            try {
                context.settingsDataStore.edit { prefs ->
                    prefs[LINE_SPACING_KEY] = safeSpacing
                }
            } catch (_: Exception) {}
        }
    }

    fun updateVishramColor(color: String?) {
        val safeColor = color ?: "#FF9933"
        scope.launch {
            try {
                context.settingsDataStore.edit { prefs ->
                    prefs[VISHRAM_COLOR_KEY] = safeColor
                }
            } catch (_: Exception) {}
        }
    }

    fun updateKeepScreenOn(enabled: Boolean) {
        scope.launch {
            try {
                context.settingsDataStore.edit { prefs ->
                    prefs[KEEP_SCREEN_ON_KEY] = enabled
                }
            } catch (_: Exception) {}
        }
    }

    fun updateShowTranslation(show: Boolean) {
        scope.launch {
            try {
                context.settingsDataStore.edit { prefs ->
                    prefs[SHOW_TRANSLATION_KEY] = show
                }
            } catch (_: Exception) {}
        }
    }

    fun updateShowPunjabiTranslation(show: Boolean) {
        scope.launch {
            try {
                context.settingsDataStore.edit { prefs ->
                    prefs[SHOW_PUNJABI_TRANSLATION_KEY] = show
                }
            } catch (_: Exception) {}
        }
    }

    fun setFirstLaunchDone(done: Boolean = true) {
        scope.launch {
            try {
                context.settingsDataStore.edit { prefs ->
                    prefs[FIRST_LAUNCH_KEY] = done
                }
            } catch (_: Exception) {}
        }
    }

    fun saveLastReadPosition(fileName: String?, lineIndex: Int) {
        if (fileName.isNullOrEmpty()) return
        scope.launch {
            try {
                context.settingsDataStore.edit { prefs ->
                    prefs[intPreferencesKey("last_read_$fileName")] = lineIndex
                }
            } catch (_: Exception) {}
        }
    }

    fun getLastReadPosition(fileName: String?): Flow<Int> {
        if (fileName.isNullOrEmpty()) return flowOf(0)
        return context.settingsDataStore.data
            .catch { emit(emptyPreferences()) }
            .map { prefs ->
                prefs[intPreferencesKey("last_read_$fileName")] ?: 0
            }
    }
}
