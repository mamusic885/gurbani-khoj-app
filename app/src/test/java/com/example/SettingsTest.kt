package com.example

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import com.example.ui.theme.MyApplicationTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SettingsTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun testSettingsScreenRendering() {
    val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    val settingsManager = SettingsManager(context)
    composeTestRule.setContent {
      MyApplicationTheme {
        SettingsScreen(
          settingsManager = settingsManager,
          onBack = {}
        )
      }
    }
  }
}
