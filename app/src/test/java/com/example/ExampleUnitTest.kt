package com.example

import org.junit.Assert.*
import org.junit.Test
import java.util.regex.Pattern

class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun testMathEvaluation() {
    val input = "what is 25 * 4"
    val normalized = input.replace("times", "*").replace("x", "*")
    val pattern = Pattern.compile("(\\d+(\\.\\d+)?)\\s*([+\\-*/])\\s*(\\d+(\\.\\d+)?)")
    val matcher = pattern.matcher(normalized)
    assertTrue(matcher.find())
    val num1 = matcher.group(1)?.toDoubleOrNull() ?: 0.0
    val num2 = matcher.group(4)?.toDoubleOrNull() ?: 0.0
    assertEquals(100.0, num1 * num2, 0.001)
  }

  @Test
  fun testTimerRegexExtraction() {
    val input = "set timer for 5 minutes"
    val regex = Pattern.compile("(\\d+)\\s*(min|minute|minutes|મિનિટ)")
    val matcher = regex.matcher(input)
    assertTrue(matcher.find())
    val minutes = matcher.group(1)?.toIntOrNull() ?: 0
    assertEquals(5, minutes)
  }
}
