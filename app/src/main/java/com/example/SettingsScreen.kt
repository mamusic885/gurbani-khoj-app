package com.example

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.theme.*

@Composable
fun SettingsScreen(
    settingsManager: SettingsManager,
    onBack: () -> Unit,
    onNavigateToAbout: () -> Unit = {}
) {
    val settingsState by settingsManager.settingsState.collectAsStateWithLifecycle()

    val previewFontSize = when (settingsState.fontSize) {
        "Small" -> 15.sp
        "Large" -> 23.sp
        "Extra Large" -> 28.sp
        else -> 19.sp
    }

    val previewFontFamily = when (settingsState.fontFamily) {
        "Sant Lipi" -> santLipiFontFamily
        "Unicode Gurmukhi" -> FontFamily.SansSerif
        "Noto Serif Gurmukhi" -> FontFamily.Serif
        else -> FontFamily.Default
    }

    val previewLineHeight = previewFontSize * settingsState.lineSpacing

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .testTag("settings_screen_scaffold"),
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp)
        ) {
            TopAppBar(
                title = "ਸੈਟਿੰਗਾਂ (Settings)",
                onBack = onBack,
                modifier = Modifier.padding(top = 8.dp)
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
                    .testTag("settings_list"),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 16.dp)
            ) {
                // 1. Live Preview Section
                item {
                    Text(
                        text = "ਪੂਰਵਦਰਸ਼ਨ (Live Preview)",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = SaffronPrimary
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSystemInDarkTheme()) Color(0xFF1E1E1E) else Slate50
                        ),
                        border = BorderStroke(1.dp, SaffronBorder),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth().testTag("settings_preview_card")
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp).fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = buildGurmukhiLine("ੴ ਸਤਿ ਨਾਮੁ ਕਰਤਾ ਪੁਰਖੁ ਨਿਰਭਉ ਨਿਰਵੈਰੁ ॥", settingsState.vishramColor),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = previewFontSize,
                                    fontFamily = previewFontFamily,
                                    color = if (isSystemInDarkTheme()) Color.White else TextMedium,
                                    textAlign = TextAlign.Center,
                                    lineHeight = previewLineHeight
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                            if (settingsState.showTranslation) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "One Universal Creator God. Truth is the Name. Creator Being Personified.",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontSize = 11.sp,
                                        color = TextGray,
                                        textAlign = TextAlign.Center
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            if (settingsState.showPunjabiTranslation) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "ਵਾਹਿਗੁਰੂ ਕੇਵਲ ਇਕ ਹੈ। ਸੱਚਾ ਹੈ ਉਸ ਦਾ ਨਾਮ, ਰਚਨਹਾਰ ਉਸ ਦੀ ਵਿਅਕਤੀ ਅਤੇ ਅਮਰ ਉਸ ਦਾ ਸਰੂਪ।",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontSize = 11.sp,
                                        color = TextGray,
                                        textAlign = TextAlign.Center
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }

                // 2. Reading Experience Section
                item {
                    Text(
                        text = "ਪਾਠ ਅਨੁਭਵ (Reading Experience)",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = SaffronPrimary
                        )
                    )
                }

                // Show English Translation Switch Option
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Slate50),
                        border = BorderStroke(1.dp, Slate200),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth().testTag("show_translation_setting_card")
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Show English Translation",
                                    fontWeight = FontWeight.Bold,
                                    color = TextMedium,
                                    fontSize = 15.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Show or hide the English translation below Gurbani.",
                                    color = TextGray,
                                    fontSize = 12.sp
                                )
                            }
                            Switch(
                                checked = settingsState.showTranslation,
                                onCheckedChange = { settingsManager.updateShowTranslation(it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = SaffronPrimary,
                                    uncheckedThumbColor = Slate200,
                                    uncheckedTrackColor = Color.White
                                ),
                                modifier = Modifier.testTag("show_translation_switch")
                            )
                        }
                    }
                }

                // Show Punjabi Translation Switch Option
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Slate50),
                        border = BorderStroke(1.dp, Slate200),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth().testTag("show_punjabi_translation_setting_card")
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Show Punjabi Translation",
                                    fontWeight = FontWeight.Bold,
                                    color = TextMedium,
                                    fontSize = 15.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Show or hide the Punjabi translation (ਟੀਕਾ/ਅਰਥ) below Gurbani.",
                                    color = TextGray,
                                    fontSize = 12.sp
                                )
                            }
                            Switch(
                                checked = settingsState.showPunjabiTranslation,
                                onCheckedChange = { settingsManager.updateShowPunjabiTranslation(it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = SaffronPrimary,
                                    uncheckedThumbColor = Slate200,
                                    uncheckedTrackColor = Color.White
                                ),
                                modifier = Modifier.testTag("show_punjabi_translation_switch")
                            )
                        }
                    }
                }

                // Font Size Options
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Slate50),
                        border = BorderStroke(1.dp, Slate200),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth().testTag("font_size_setting_card")
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "ਅੱਖਰਾਂ ਦਾ ਅਕਾਰ (Font Size)",
                                fontWeight = FontWeight.Bold,
                                color = TextMedium,
                                fontSize = 15.sp
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf("Small", "Medium", "Large", "Extra Large").forEach { size ->
                                    val isSelected = settingsState.fontSize == size
                                    Card(
                                        onClick = { settingsManager.updateFontSize(size) },
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (isSelected) SaffronPrimary else Color.White
                                        ),
                                        border = BorderStroke(1.dp, if (isSelected) SaffronPrimary else Slate200),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.weight(1f).height(40.dp).testTag("font_size_btn_$size")
                                    ) {
                                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                            Text(
                                                text = when (size) {
                                                    "Small" -> "ਛੋਟਾ"
                                                    "Medium" -> "ਦਰਮਿਆਨਾ"
                                                    "Large" -> "ਵੱਡਾ"
                                                    else -> "ਬਹੁਤ ਵੱਡਾ"
                                                },
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isSelected) Color.White else TextMedium,
                                                    fontSize = 12.sp
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Font Family Options
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Slate50),
                        border = BorderStroke(1.dp, Slate200),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth().testTag("font_family_setting_card")
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "ਫੋਂਟ ਸ਼ੈਲੀ (Font Family)",
                                fontWeight = FontWeight.Bold,
                                color = TextMedium,
                                fontSize = 15.sp
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Column(
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf("Noto Serif Gurmukhi", "Sant Lipi", "Unicode Gurmukhi").forEach { family ->
                                    val isSelected = settingsState.fontFamily == family
                                    Card(
                                        onClick = { settingsManager.updateFontFamily(family) },
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (isSelected) SaffronLight else Color.White
                                        ),
                                        border = BorderStroke(1.dp, if (isSelected) SaffronPrimary else Slate200),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("font_family_btn_$family")
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = when (family) {
                                                    "Noto Serif Gurmukhi" -> "ਨੋਟੋ ਸੇਰਿਫ (Noto Serif Gurmukhi)"
                                                    "Sant Lipi" -> "ਪੁਰਾਤਨ ਲਿਪੀ (Sant Lipi)"
                                                    else -> "ਯੂਨੀਕੋਡ (Unicode Gurmukhi)"
                                                },
                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    color = TextMedium,
                                                    fontFamily = when (family) {
                                                        "Sant Lipi" -> santLipiFontFamily
                                                        "Unicode Gurmukhi" -> FontFamily.SansSerif
                                                        else -> FontFamily.Serif
                                                    }
                                                )
                                            )
                                            if (isSelected) {
                                                Text(text = "✓", color = SaffronPrimary, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Line Spacing Options
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Slate50),
                        border = BorderStroke(1.dp, Slate200),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth().testTag("line_spacing_setting_card")
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "ਲਾਈਨਾਂ ਵਿਚਲੀ ਵਿੱਥ (Line Spacing)",
                                    fontWeight = FontWeight.Bold,
                                    color = TextMedium,
                                    fontSize = 15.sp
                                )
                                Text(
                                    text = String.format("%.1fx", settingsState.lineSpacing),
                                    fontWeight = FontWeight.Bold,
                                    color = SaffronPrimary,
                                    fontSize = 14.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Slider(
                                value = settingsState.lineSpacing,
                                onValueChange = { settingsManager.updateLineSpacing(it) },
                                valueRange = 1.0f..2.5f,
                                steps = 14,
                                colors = SliderDefaults.colors(
                                    thumbColor = SaffronPrimary,
                                    activeTrackColor = SaffronPrimary,
                                    inactiveTrackColor = Slate200
                                ),
                                modifier = Modifier.testTag("line_spacing_slider")
                            )
                        }
                    }
                }

                // 3. Theme & Screen On Section
                item {
                    Text(
                        text = "ਦਿੱਖ ਅਤੇ ਥੀਮ (Appearance & Screen)",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = SaffronPrimary
                        )
                    )
                }

                // Theme Mode Selector
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Slate50),
                        border = BorderStroke(1.dp, Slate200),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth().testTag("theme_setting_card")
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "ਐਪ ਥੀਮ (Theme)",
                                fontWeight = FontWeight.Bold,
                                color = TextMedium,
                                fontSize = 15.sp
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf("System", "Light", "Dark").forEach { mode ->
                                    val isSelected = settingsState.themeMode == mode
                                    Card(
                                        onClick = { settingsManager.updateThemeMode(mode) },
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (isSelected) SaffronPrimary else Color.White
                                        ),
                                        border = BorderStroke(1.dp, if (isSelected) SaffronPrimary else Slate200),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.weight(1f).height(40.dp).testTag("theme_btn_$mode")
                                    ) {
                                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                            Text(
                                                text = when (mode) {
                                                    "System" -> "ਸਿਸਟਮ"
                                                    "Light" -> "ਲਾਈਟ"
                                                    else -> "ਡਾਰਕ"
                                                },
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isSelected) Color.White else TextMedium,
                                                    fontSize = 12.sp
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Keep Screen On Toggle
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Slate50),
                        border = BorderStroke(1.dp, Slate200),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth().testTag("keep_screen_on_setting_card")
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "ਸਕ੍ਰੀਨ ਚਾਲੂ ਰੱਖੋ (Keep Screen On)",
                                    fontWeight = FontWeight.Bold,
                                    color = TextMedium,
                                    fontSize = 15.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "ਪਾਠ ਕਰਦੇ ਸਮੇਂ ਸਕ੍ਰੀਨ ਬੰਦ ਨਹੀਂ ਹੋਵੇਗੀ",
                                    color = TextGray,
                                    fontSize = 12.sp
                                )
                            }
                            Switch(
                                checked = settingsState.keepScreenOn,
                                onCheckedChange = { settingsManager.updateKeepScreenOn(it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = SaffronPrimary,
                                    uncheckedThumbColor = Slate200,
                                    uncheckedTrackColor = Color.White
                                ),
                                modifier = Modifier.testTag("keep_screen_on_switch")
                            )
                        }
                    }
                }

                // 4. Vishram Highlight Section
                item {
                    Text(
                        text = "ਵਿਸ਼ਰਾਮ ਹਾਈਲਾਈਟ (Vishram Color)",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = SaffronPrimary
                        )
                    )
                }

                // Vishram Color Options
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Slate50),
                        border = BorderStroke(1.dp, Slate200),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth().testTag("vishram_color_setting_card")
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "ਹਾਈਲਾਈਟ ਰੰਗ (Highlight Color)",
                                fontWeight = FontWeight.Bold,
                                color = TextMedium,
                                fontSize = 15.sp
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val colorOptions = listOf(
                                    "#FF9933" to "Saffron",
                                    "#2E7D32" to "Green",
                                    "#1565C0" to "Blue",
                                    "#C62828" to "Red",
                                    "#6A1B9A" to "Purple",
                                    "None" to "None"
                                )

                                colorOptions.forEach { (colorHex, name) ->
                                    val isSelected = settingsState.vishramColor == colorHex

                                    val parsedBgColor = if (colorHex == "None") {
                                        Color.Transparent
                                    } else {
                                        try {
                                            Color(AndroidColor.parseColor(colorHex))
                                        } catch (_: Exception) {
                                            SaffronPrimary
                                        }
                                    }

                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(
                                                color = parsedBgColor,
                                                shape = CircleShape
                                            )
                                            .border(
                                                width = if (isSelected) 3.dp else 1.dp,
                                                color = if (isSelected) SaffronPrimary else if (colorHex == "None") TextGray else Color.Transparent,
                                                shape = CircleShape
                                            )
                                            .clickable { settingsManager.updateVishramColor(colorHex) }
                                            .testTag("vishram_color_btn_$name")
                                    ) {
                                        if (colorHex == "None") {
                                            Text(text = "✕", color = TextGray, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                        } else if (isSelected) {
                                            Text(text = "✓", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 6. About Section
                item {
                    Text(
                        text = "ਐਪ ਬਾਰੇ (About App)",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = SaffronPrimary
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Slate50),
                        border = BorderStroke(1.dp, Slate200),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigateToAbout() }
                            .testTag("about_setting_card")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(text = "ℹ️", fontSize = 22.sp)
                                Column {
                                    Text(
                                        text = "About Gurbani Khoj / ਐਪ ਬਾਰੇ",
                                        fontWeight = FontWeight.Bold,
                                        color = TextMedium,
                                        fontSize = 15.sp
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Version info, features & credits",
                                        color = TextGray,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                            Text(
                                text = "→",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = SaffronPrimary
                            )
                        }
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .width(128.dp)
                        .height(6.dp)
                        .background(
                            color = Slate200,
                            shape = RoundedCornerShape(3.dp)
                        )
                )
            }
        }
    }
}
