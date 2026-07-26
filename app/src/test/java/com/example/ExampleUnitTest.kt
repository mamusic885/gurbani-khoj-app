package com.example

import com.example.util.convertGurbaniAkharToUnicode
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun testGurbaniVerseNumberPreservation() {
    val line1 = "hukim rjweI clxw; nwnk. iliKAw nwil ]1]"
    val converted1 = convertGurbaniAkharToUnicode(line1)
    assertTrue("Should end with verse number 1", converted1.endsWith("॥੧॥"))

    val line2 = "nwnk. hukmY jy buJY; q haumY. khY n koie ]2]"
    val converted2 = convertGurbaniAkharToUnicode(line2)
    assertTrue("Should end with verse number 2", converted2.endsWith("॥੨॥"))

    val line16 = "suxIAY idK kI rhY inswin ]16]1]"
    val converted16 = convertGurbaniAkharToUnicode(line16)
    assertTrue("Should end with verse numbers 16 and 1", converted16.endsWith("॥੧੬॥੧॥"))
  }
}

