package com.example

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SettingsViewModel(private val settingsManager: SettingsManager) : ViewModel() {

    val uiState: StateFlow<SettingsState> = settingsManager.settingsState

    fun setFontSize(size: String) {
        viewModelScope.launch {
            settingsManager.updateFontSize(size)
        }
    }

    fun setFontFamily(family: String) {
        viewModelScope.launch {
            settingsManager.updateFontFamily(family)
        }
    }

    fun setThemeMode(mode: String) {
        viewModelScope.launch {
            settingsManager.updateThemeMode(mode)
        }
    }

    fun setLineSpacing(spacing: Float) {
        viewModelScope.launch {
            settingsManager.updateLineSpacing(spacing)
        }
    }

    fun setVishramColor(color: String) {
        viewModelScope.launch {
            settingsManager.updateVishramColor(color)
        }
    }

    fun setKeepScreenOn(enabled: Boolean) {
        viewModelScope.launch {
            settingsManager.updateKeepScreenOn(enabled)
        }
    }

    fun setShowTranslation(show: Boolean) {
        viewModelScope.launch {
            settingsManager.updateShowTranslation(show)
        }
    }
}
