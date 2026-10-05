package dev.nick.stepcounter.data.repository
import dev.nick.stepcounter.data.datastore.UserMeasurements.UserProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class UserPreferencesRepository(private val userMeasurements: dev.nick.stepcounter.data.datastore.UserMeasurements) {
    val userProfileFlow:Flow<UserProfile> =userMeasurements.userProfileFlow

    suspend fun saveUserData(heightInput: Int?, weightInput: Double?,stepsGoalInput:Int?)
    {
        val height=heightInput?:userProfileFlow.first().height
        val weight=weightInput?:userProfileFlow.first().weight
        val stepsGoal=stepsGoalInput?:userProfileFlow.first().stepsGoal
        userMeasurements.saveUserData(height,weight,stepsGoal)
    }

    suspend fun updateUserCadence(cadence:Int)
    {
        userMeasurements.updateUserCadence(cadence)
    }

    suspend fun updateUserLastSteps(lastHardwareSteps:Int)
    {
        userMeasurements.updateUserLastSteps(lastHardwareSteps)
    }

    suspend fun updateUserActiveCalories(activeCalories:Double)
    {
        userMeasurements.updateUserActiveCaloriesToday(activeCalories)
    }
    suspend fun updateUserActiveMinutes(activeMinutes:Double)
    {
        userMeasurements.updateUserActiveMinutesToday(activeMinutes)
    }
    suspend fun updateLastTrackedDate(lastDate: Long) {
        userMeasurements.updateLastTrackedDate(lastDate)
    }
    suspend fun resetDailyStats() {
        userMeasurements.resetDailyActivityStats()
    }


}