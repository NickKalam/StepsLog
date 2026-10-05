package dev.nick.stepcounter.domain.model

data class DailyActivityStats (
    val steps: Int = 0,
    val distanceMeters: Double = 0.0,
    val distanceKm: Double = 0.0,
    val minutesOfWalking: Int = 0,
    val caloriesBurned: Double = 0.0,
    val userHeight: Int = 0,
    val userWeight: Double = 0.0,
    val userStepsGoal: Int = 7000
    )


