package dev.nick.stepcounter.domain.calculator

import org.junit.Assert.assertEquals
import org.junit.Test

class CalorieCalculatorTest {

    @Test
    fun `calculateActiveCaloriesDelta at rest returns base METs`() {
        val cadence = 0
        val weight = 80.0
        val minutes = 60.0
        // At rest: METs = 1.0. Formula: 1.0 * 80.0 * (60.0/60.0) = 80.0
        val result = calculateActiveCaloriesDelta(cadence, weight, minutes)

        assertEquals(80.0, result, 0.1)
    }

    @Test
    fun `calculateActiveCaloriesDelta at moderate pace returns correct calories`() {
        val cadence = 110
        val weight = 82.0
        val minutes = 30.0
        // METs = 3.0 + (110 - 100) * 0.05 = 3.5
        // Calories = 3.5 * 82.0 * (30.0/60.0) = 143.5
        val result = calculateActiveCaloriesDelta(cadence, weight, minutes)

        assertEquals(143.5, result, 0.1)
    }
}