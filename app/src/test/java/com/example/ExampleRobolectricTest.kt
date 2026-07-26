package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Gurbani Khoj", appName)
  }

  @Test
  fun `verify all banis parse correctly and are complete`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val baniFiles = listOf(
      "japji_sahib.json",
      "jaap_sahib.json",
      "tav_prasad_savaiye.json",
      "chaupai_sahib.json",
      "anand_sahib.json",
      "rehras_sahib.json",
      "ardas.json",
      "kirtan_sohila.json",
      "aarti.json",
      "asa_di_vaar.json",
      "sri_sukhmani_sahib.json"
    )

    for (fileName in baniFiles) {
      val bani = loadBaniFromAsset(context, fileName)
      assertNotEquals("Failed for $fileName", "", bani.title)
      assertTrue("Bani $fileName is empty!", bani.verses.isNotEmpty())

      // Verify that large banis are indeed complete and contain many verses (not just a few pauris)
      when (fileName) {
        "japji_sahib.json" -> {
          assertTrue("Japji Sahib is too short! (${bani.verses.size} lines)", bani.verses.size > 100)
        }
        "jaap_sahib.json" -> {
          assertTrue("Jaap Sahib is too short! (${bani.verses.size} lines)", bani.verses.size > 150)
        }
        "sri_sukhmani_sahib.json" -> {
          assertTrue("Sukhmani Sahib is too short! (${bani.verses.size} lines)", bani.verses.size > 1000)
        }
        "asa_di_vaar.json" -> {
          assertTrue("Asa Di Vaar is too short! (${bani.verses.size} lines)", bani.verses.size > 200)
        }
        "rehras_sahib.json" -> {
          assertTrue("Rehras Sahib is too short! (${bani.verses.size} lines)", bani.verses.size > 100)
        }
      }

      // Check schema fields are fully parsed
      val firstVerse = bani.verses.first()
      assertNotEquals("", firstVerse.line)
    }
  }
}

