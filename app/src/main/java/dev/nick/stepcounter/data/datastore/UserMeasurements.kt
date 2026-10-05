package dev.nick.stepcounter.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_profile")
class UserMeasurements(private val context: Context) {

    companion object {
        val HEIGHT = intPreferencesKey(name = "userHeight")
        val WEIGHT = doublePreferencesKey(name = "userWeight")
        val CADENCE = intPreferencesKey(name="userCadence")
        val LAST_HARDWARE_STEPS= intPreferencesKey(name="userLastSteps")
        val LAST_TRACKED_DATE=longPreferencesKey(name="userLastTrackedDate")
        val ACTIVE_CALORIES_TODAY= doublePreferencesKey(name="userActiveCaloriesToday")
        val ACTIVE_MINUTES_TODAY= doublePreferencesKey(name="userActiveMinutesToday")
        val STEPS_GOAL= intPreferencesKey(name="userStepsGoal")
    }

    data class UserProfile(
        val height: Int ,
        val weight: Double ,
        val cadence:Int=90,
        val lastHardwareSteps:Int=0,
        val lastTrackedDate:Long=java.time.LocalDate.now().toEpochDay(),
        val activeCaloriesToday:Double=0.0,
        val activeMinutesToday:Double=0.0,
        val stepsGoal:Int=7000
    )
    val userProfileFlow :Flow<UserProfile> = context.dataStore.data.map{ preferences->
        UserProfile(
            height = preferences[HEIGHT]
                ?: 174,
            weight = preferences[WEIGHT]
                ?: 82.0,
            cadence = preferences[CADENCE]
                ?: 90,
            lastHardwareSteps = preferences[LAST_HARDWARE_STEPS]
                ?: 0,
            lastTrackedDate = preferences[LAST_TRACKED_DATE]
                ?: java.time.LocalDate.now().toEpochDay(),
            activeCaloriesToday = preferences[ACTIVE_CALORIES_TODAY]
                ?: 0.0,
            activeMinutesToday = preferences[ACTIVE_MINUTES_TODAY]
                ?: 0.0,
            stepsGoal = preferences[STEPS_GOAL]
                ?: 7000
        )
    }

    suspend fun saveUserData(heightInput: Int, weightInput: Double,stepsGoalInput:Int){
        context.dataStore.edit { preferences ->
            preferences[HEIGHT] = heightInput
            preferences[WEIGHT] = weightInput
            preferences[STEPS_GOAL]=stepsGoalInput
        }
    }

    suspend fun updateUserCadence(cadence:Int)
    {
        context.dataStore.edit{preferences->
            preferences[CADENCE]=cadence
        }
    }

    suspend fun updateUserLastSteps(lastHardwareSteps: Int)
    {
        context.dataStore.edit{ preferences->
            preferences[LAST_HARDWARE_STEPS]=lastHardwareSteps
        }
    }

    suspend fun updateUserActiveCaloriesToday(calories:Double)
    {
        context.dataStore.edit{preferences->
            preferences[ACTIVE_CALORIES_TODAY]=calories
        }
    }
    suspend fun updateUserActiveMinutesToday(minutes:Double)
    {
        context.dataStore.edit{preferences->
            preferences[ACTIVE_MINUTES_TODAY]=minutes
        }
    }

    suspend fun updateLastTrackedDate(lastDate:Long)
    {
        context.dataStore.edit{preferences->
            preferences[LAST_TRACKED_DATE]=lastDate
        }
    }

    suspend fun resetDailyActivityStats()
    {
        context.dataStore.edit{preferences->
            preferences[ACTIVE_MINUTES_TODAY]=0.0
            preferences[ACTIVE_CALORIES_TODAY] = 0.0
            preferences[LAST_TRACKED_DATE] = java.time.LocalDate.now().toEpochDay()
        }
    }

}