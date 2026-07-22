package com.example

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "gurbani_settings_datastore")

class SettingsManager(private val context: Context) {

    data class AppSettings(
        val fontSize: String = "Medium",
        val fontFamily: String = "Default Gurmukhi",
        val lineSpacing: Float = 1.5f,
        val themeMode: String = "System",
        val keepScreenOn: Boolean = false,
        val vishramColor: String = "#FF9933",
        val isFirstLaunchDone: Boolean = false
    )

    companion object {
        val FONT_SIZE_KEY = stringPreferencesKey("font_size")
        val FONT_FAMILY_KEY = stringPreferencesKey("font_family")
        val LINE_SPACING_KEY = floatPreferencesKey("line_spacing")
        val THEME_MODE_KEY = stringPreferencesKey("theme_mode")
        val KEEP_SCREEN_ON_KEY = booleanPreferencesKey("keep_screen_on")
        val VISHRAM_COLOR_KEY = stringPreferencesKey("vishram_color")
        val IS_FIRST_LAUNCH_DONE_KEY = booleanPreferencesKey("is_first_launch_done")
    }

    private val scope = CoroutineScope(Dispatchers.IO)

    val settings: StateFlow<AppSettings> = context.dataStore.data
        .map { prefs ->
            AppSettings(
                fontSize = prefs[FONT_SIZE_KEY] ?: "Medium",
                fontFamily = prefs[FONT_FAMILY_KEY] ?: "Default Gurmukhi",
                lineSpacing = prefs[LINE_SPACING_KEY] ?: 1.5f,
                themeMode = prefs[THEME_MODE_KEY] ?: "System",
                keepScreenOn = prefs[KEEP_SCREEN_ON_KEY] ?: false,
                vishramColor = prefs[VISHRAM_COLOR_KEY] ?: "#FF9933",
                isFirstLaunchDone = prefs[IS_FIRST_LAUNCH_DONE_KEY] ?: false
            )
        }
        .stateIn(
            scope = scope,
            started = SharingStarted.Eagerly,
            initialValue = AppSettings()
        )

    fun updateFontSize(size: String) {
        scope.launch {
            context.dataStore.edit { prefs ->
                prefs[FONT_SIZE_KEY] = size
            }
        }
    }

    fun updateFontFamily(family: String) {
        scope.launch {
            context.dataStore.edit { prefs ->
                prefs[FONT_FAMILY_KEY] = family
            }
        }
    }

    fun updateLineSpacing(spacing: Float) {
        scope.launch {
            context.dataStore.edit { prefs ->
                prefs[LINE_SPACING_KEY] = spacing
            }
        }
    }

    fun updateThemeMode(mode: String) {
        scope.launch {
            context.dataStore.edit { prefs ->
                prefs[THEME_MODE_KEY] = mode
            }
        }
    }

    fun updateKeepScreenOn(enabled: Boolean) {
        scope.launch {
            context.dataStore.edit { prefs ->
                prefs[KEEP_SCREEN_ON_KEY] = enabled
            }
        }
    }

    fun updateVishramColor(colorHex: String) {
        scope.launch {
            context.dataStore.edit { prefs ->
                prefs[VISHRAM_COLOR_KEY] = colorHex
            }
        }
    }

    fun setFirstLaunchDone(done: Boolean = true) {
        scope.launch {
            context.dataStore.edit { prefs ->
                prefs[IS_FIRST_LAUNCH_DONE_KEY] = done
            }
        }
    }

    fun saveLastReadPosition(fileName: String, lineIndex: Int) {
        if (fileName.isEmpty()) return
        scope.launch {
            context.dataStore.edit { prefs ->
                prefs[androidx.datastore.preferences.core.intPreferencesKey("last_read_$fileName")] = lineIndex
            }
        }
    }

    fun getLastReadPosition(fileName: String): kotlinx.coroutines.flow.Flow<Int> {
        return context.dataStore.data.map { prefs ->
            prefs[androidx.datastore.preferences.core.intPreferencesKey("last_read_$fileName")] ?: 0
        }
    }
}
