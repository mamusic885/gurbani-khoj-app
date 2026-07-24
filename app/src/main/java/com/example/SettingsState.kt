package com.example

data class SettingsState(
    val fontSize: String = "Medium",
    val fontFamily: String = "Noto Serif Gurmukhi",
    val themeMode: String = "System",
    val lineSpacing: Float = 1.5f,
    val vishramColor: String = "#FF9933",
    val keepScreenOn: Boolean = false,
    val isFirstLaunchDone: Boolean = false,
    val showTranslation: Boolean = true
)
