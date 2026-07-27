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
  fun generateLauncherIcons() {
    val size = 512
    
    // 1. Generate Rich Golden Radial Background with Soft Glow
    val bgBitmap = android.graphics.Bitmap.createBitmap(size, size, android.graphics.Bitmap.Config.ARGB_8888)
    val bgCanvas = android.graphics.Canvas(bgBitmap)
    
    val bgShader = android.graphics.RadialGradient(
      size / 2f, size / 2f, size * 0.70f,
      intArrayOf(
        android.graphics.Color.rgb(255, 252, 220),
        android.graphics.Color.rgb(255, 205, 50),
        android.graphics.Color.rgb(215, 140, 10),
        android.graphics.Color.rgb(140, 80, 0)
      ),
      floatArrayOf(0.0f, 0.35f, 0.70f, 1.0f),
      android.graphics.Shader.TileMode.CLAMP
    )
    val bgPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
      shader = bgShader
    }
    bgCanvas.drawRect(0f, 0f, size.toFloat(), size.toFloat(), bgPaint)

    val glowShader = android.graphics.RadialGradient(
      size / 2f, size / 2f, size * 0.45f,
      intArrayOf(
        android.graphics.Color.argb(180, 255, 255, 245),
        android.graphics.Color.argb(90, 255, 220, 100),
        android.graphics.Color.argb(0, 255, 190, 40)
      ),
      floatArrayOf(0.0f, 0.5f, 1.0f),
      android.graphics.Shader.TileMode.CLAMP
    )
    val glowPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
      shader = glowShader
    }
    bgCanvas.drawRect(0f, 0f, size.toFloat(), size.toFloat(), glowPaint)

    val bgFile = java.io.File("src/main/res/drawable/ic_launcher_background_img.png")
    bgBitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, java.io.FileOutputStream(bgFile))

    // 2. Generate White/Silver ੴ Foreground Symbol
    val fgBitmap = android.graphics.Bitmap.createBitmap(size, size, android.graphics.Bitmap.Config.ARGB_8888)
    val fgCanvas = android.graphics.Canvas(fgBitmap)

    val fontFile = java.io.File("src/main/res/font/noto_serif_gurmukhi.ttf")
    val typeface = android.graphics.Typeface.createFromFile(fontFile)

    val textPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
      this.typeface = typeface
      textSize = 210f
    }

    val symbol = "ੴ"
    val bounds = android.graphics.Rect()
    textPaint.getTextBounds(symbol, 0, symbol.length, bounds)

    val x = size / 2f - bounds.exactCenterX()
    val y = size / 2f - bounds.exactCenterY()

    val shadowPaint = android.graphics.Paint(textPaint).apply {
      color = android.graphics.Color.argb(70, 80, 40, 0)
    }
    fgCanvas.drawText(symbol, x + 2f, y + 4f, shadowPaint)

    val textShader = android.graphics.LinearGradient(
      0f, y - bounds.height(), 0f, y + bounds.height() / 2f,
      intArrayOf(
        android.graphics.Color.rgb(255, 255, 255),
        android.graphics.Color.rgb(245, 247, 250),
        android.graphics.Color.rgb(220, 225, 235)
      ),
      null,
      android.graphics.Shader.TileMode.CLAMP
    )
    textPaint.shader = textShader
    fgCanvas.drawText(symbol, x, y, textPaint)

    val fgFile = java.io.File("src/main/res/drawable/ic_launcher_foreground_img.png")
    fgBitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, java.io.FileOutputStream(fgFile))

    // 3. Generate legacy and adaptive mipmap icons for all densities
    data class DensityConfig(val dirName: String, val legacySize: Int, val adaptiveSize: Int)
    val densities = listOf(
      DensityConfig("mipmap-mdpi", 48, 108),
      DensityConfig("mipmap-hdpi", 72, 162),
      DensityConfig("mipmap-xhdpi", 96, 216),
      DensityConfig("mipmap-xxhdpi", 144, 324),
      DensityConfig("mipmap-xxxhdpi", 192, 432)
    )

    for (config in densities) {
      val dir = java.io.File("src/main/res/${config.dirName}")
      dir.mkdirs()

      // Adaptive Background
      val scaledBg = android.graphics.Bitmap.createScaledBitmap(bgBitmap, config.adaptiveSize, config.adaptiveSize, true)
      scaledBg.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, java.io.FileOutputStream(java.io.File(dir, "ic_launcher_background.png")))

      // Adaptive Foreground
      val scaledFg = android.graphics.Bitmap.createScaledBitmap(fgBitmap, config.adaptiveSize, config.adaptiveSize, true)
      scaledFg.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, java.io.FileOutputStream(java.io.File(dir, "ic_launcher_foreground.png")))

      // Legacy Composite Square/Squircle Full Icon
      val legacyFull = android.graphics.Bitmap.createBitmap(config.legacySize, config.legacySize, android.graphics.Bitmap.Config.ARGB_8888)
      val legacyCanvas = android.graphics.Canvas(legacyFull)
      legacyCanvas.drawBitmap(bgBitmap, null, android.graphics.RectF(0f, 0f, config.legacySize.toFloat(), config.legacySize.toFloat()), null)
      
      // Draw foreground scaled into safe center zone (66/108 of legacy size)
      val fgInset = config.legacySize * (21f / 108f)
      val fgRect = android.graphics.RectF(fgInset, fgInset, config.legacySize - fgInset, config.legacySize - fgInset)
      legacyCanvas.drawBitmap(fgBitmap, null, fgRect, null)

      legacyFull.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, java.io.FileOutputStream(java.io.File(dir, "ic_launcher.png")))

      // Legacy Composite Round Icon
      val roundFull = android.graphics.Bitmap.createBitmap(config.legacySize, config.legacySize, android.graphics.Bitmap.Config.ARGB_8888)
      val roundCanvas = android.graphics.Canvas(roundFull)
      val clipPath = android.graphics.Path().apply {
        addCircle(config.legacySize / 2f, config.legacySize / 2f, config.legacySize / 2f, android.graphics.Path.Direction.CW)
      }
      roundCanvas.clipPath(clipPath)
      roundCanvas.drawBitmap(legacyFull, 0f, 0f, null)

      roundFull.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, java.io.FileOutputStream(java.io.File(dir, "ic_launcher_round.png")))
    }
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

