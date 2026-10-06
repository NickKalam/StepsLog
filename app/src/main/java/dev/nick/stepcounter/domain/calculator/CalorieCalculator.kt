package dev.nick.stepcounter.domain.calculator

/**
 * Calculates the active calories burned based on cadence, weight, and duration.
 *
 * Uses Metabolic Equivalent of Task (MET) estimations:
 * - < 100 steps/min: Light pace (~2.2 METs)
 * - 100-119 steps/min: Moderate pace (up to 4.0 METs)
 * - 120+ steps/min: Vigorous pace (up to 5.0+ METs)
 */
fun calculateActiveCaloriesDelta(cadence:Int,weight:Double,minutes:Double):Double
{
    val grossMet = when {
        cadence <= 0 -> 1.0
        cadence < 100 -> 0.038 * cadence - 0.796
        cadence < 120 -> 3.0 + (cadence - 100) * 0.05
        else -> 4.0 + (cadence - 120) * 0.10
    }.coerceAtLeast(1.0).coerceAtMost(6.0)

    val netMet = (grossMet - 1.0).coerceAtLeast(0.5)
    val batchHours=minutes/60.0
    return netMet * weight * batchHours
}