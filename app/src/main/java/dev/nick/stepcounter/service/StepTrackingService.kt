package dev.nick.stepcounter.service


import android.Manifest
import android.app.Service
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.hardware.Sensor
import android.hardware.SensorManager
import android.os.Build
import android.os.IBinder
import androidx.annotation.RequiresPermission
import androidx.core.app.ServiceCompat
import dagger.hilt.android.AndroidEntryPoint
import dev.nick.stepcounter.data.repository.StepsRepository
import dev.nick.stepcounter.data.repository.UserPreferencesRepository
import dev.nick.stepcounter.data.sensor.StepsSensorListener
import dev.nick.stepcounter.domain.calculator.calculateActiveCaloriesDelta
import dev.nick.stepcounter.domain.usecase.ObserveDailyStatsUseCase
import dev.nick.stepcounter.receiver.DateChangeReceiver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.sample
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

@AndroidEntryPoint
class StepTrackingService : Service() {

    @Inject lateinit var repository: StepsRepository
    @Inject lateinit var observeDailyStatsUseCase: ObserveDailyStatsUseCase
    @Inject lateinit var userPreferencesRepository: UserPreferencesRepository
    @Inject lateinit var timeProvider: dev.nick.stepcounter.domain.util.TimeProvider
    @Inject lateinit var stepNotificationManager: StepNotificationManager
    private var hasNotified=false
    private var currentStepsGoal=0
    private lateinit var sensorManager: SensorManager
    private var stepSensor: Sensor? = null
    private lateinit var listener: StepsSensorListener
    private var dailyStepJob: Job? = null
    private var pendingCalorieDelta: Double = 0.0
    private var pendingMinutesDelta: Double = 0.0
    private var lastDataStoreWriteTime: Long = 0L
    private var lastHardwareTotal:Int=-1
    private var currentProfile: dev.nick.stepcounter.data.datastore.UserMeasurements.UserProfile? = null



    companion object  {
        private const val DATASTORE_WRITE_INTERVAL_MS=30_000L
    }
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())


    private val dateChangeReceiver= DateChangeReceiver{
        serviceScope.launch {
            userPreferencesRepository.resetDailyStats()
            userPreferencesRepository.updateUserCadence(90)
            repository.deleteOldSteps()
            hasNotified = false
        }
        startObservingSteps()
    }

    override fun onCreate() {
        super.onCreate()
        lastDataStoreWriteTime = timeProvider.currentTimeMillis()
        serviceScope.launch {
            val profile = userPreferencesRepository.userProfileFlow.first()
            val today = LocalDate.now().toEpochDay()

            //When the data changes
            if (profile.lastTrackedDate < today) {
                //Reset Datastore daily stats data and delete old steps in the database
                userPreferencesRepository.resetDailyStats()
                userPreferencesRepository.updateUserCadence(90)
                repository.deleteOldSteps()
                hasNotified = false
            }
        }

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_DATE_CHANGED)
            addAction(Intent.ACTION_TIME_CHANGED)
            addAction(Intent.ACTION_TIMEZONE_CHANGED)
        }
        registerReceiver(dateChangeReceiver, filter)

        val serviceType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH
        } else {
            0
        }
        val initialData=
            StepNotificationManager.NotificationData(0,0,0.0,0.0,0)
        ServiceCompat.startForeground(
            this,
            StepNotificationManager.NOTIFICATION_ID,
            stepNotificationManager.getNotification(initialData),
            serviceType
        )
        startObservingSteps()


        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)

        listener = StepsSensorListener(timeProvider) { newSteps,timestamp ->
            serviceScope.launch {
                repository.saveSteps(newSteps,timestamp)

                val profile = currentProfile ?: userPreferencesRepository.userProfileFlow.first()

                val safeCadence = maxOf(1, profile.cadence) // Prevent divide by zero
                val batchMinutes = newSteps.toDouble() / safeCadence


                val deltaKcal = calculateActiveCaloriesDelta(profile.cadence,profile.weight,batchMinutes)

                pendingCalorieDelta += deltaKcal
                pendingMinutesDelta+=batchMinutes

                val currentTime = timeProvider.currentTimeMillis()
                if (currentTime - lastDataStoreWriteTime >= DATASTORE_WRITE_INTERVAL_MS) {

                    if(profile.stepsGoal>currentStepsGoal && hasNotified)
                        hasNotified=false
                    currentStepsGoal=profile.stepsGoal

                    userPreferencesRepository.updateUserActiveCalories(profile.activeCaloriesToday + pendingCalorieDelta)
                    userPreferencesRepository.updateUserActiveMinutes(profile.activeMinutesToday + pendingMinutesDelta)
                    if(lastHardwareTotal>0)
                        userPreferencesRepository.updateUserLastSteps(lastHardwareTotal)
                    userPreferencesRepository.updateLastTrackedDate(LocalDate.now().toEpochDay())

                    pendingCalorieDelta = 0.0
                    pendingMinutesDelta = 0.0
                    lastDataStoreWriteTime = currentTime
                }

            }
        }




        listener.onCadenceUpdated = { newCadence ->
            serviceScope.launch {
                userPreferencesRepository.updateUserCadence(newCadence)
            }
        }

        serviceScope.launch {
            userPreferencesRepository.userProfileFlow.collect { profile ->
                listener.currentCadence = profile.cadence
                currentProfile=profile
            }
        }
        serviceScope.launch {
            val profile = userPreferencesRepository.userProfileFlow.first()
            val savedBaseline = profile.lastHardwareSteps

            if (savedBaseline > 0) {
                listener.setLastReportedSteps(savedBaseline, timeProvider.currentTimeMillis())
            }
            listener.onHardwareTotalUpdated = { newHardwareTotal ->
                    lastHardwareTotal=newHardwareTotal
            }

            stepSensor?.let {
                sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_NORMAL)
            }
        }
    }
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    @RequiresPermission(Manifest.permission.ACTIVITY_RECOGNITION)
    override fun onDestroy() {
        super.onDestroy()
        // Prevent memory leaks
        if (pendingCalorieDelta > 0.0 || lastHardwareTotal>0) {
            CoroutineScope(Dispatchers.IO).launch {
                val profile = userPreferencesRepository.userProfileFlow.first()
                userPreferencesRepository.updateUserActiveCalories(profile.activeCaloriesToday + pendingCalorieDelta)
                userPreferencesRepository.updateUserActiveMinutes(profile.activeMinutesToday+pendingMinutesDelta)
                if(lastHardwareTotal>0)
                    userPreferencesRepository.updateUserLastSteps(lastHardwareTotal)
            }
        }
        sensorManager.unregisterListener(listener)
        serviceScope.cancel()
        dailyStepJob?.cancel()
        unregisterReceiver(dateChangeReceiver)

    }
    override fun onBind(intent: Intent?): IBinder? = null

    @OptIn(FlowPreview::class)
    private fun startObservingSteps(){
        dailyStepJob?.cancel()

        dailyStepJob=serviceScope.launch {
            val zoneId = ZoneId.systemDefault()
            val startOfDay = LocalDate.now().atStartOfDay(zoneId).toInstant().toEpochMilli()
            val endOfDay = LocalDate.now().plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli()

            observeDailyStatsUseCase(startOfDay, endOfDay)
                .sample(2000L) // Prevent Notification Manager spam
                .map { stats ->
                    // Map Domain Model -> Notification State
                    StepNotificationManager.NotificationData(
                        currentSteps = stats.steps,
                        currentMinutes = stats.minutesOfWalking,
                        currentKcal = stats.caloriesBurned,
                        currentKm = stats.distanceKm,
                        stepsGoal = stats.userStepsGoal
                    )
                }
                .collect{data->
                    stepNotificationManager.updateNotification(data)
                    if (data.currentSteps >= data.stepsGoal && !hasNotified) {
                        stepNotificationManager.showOneOffNotification(data.stepsGoal)
                        hasNotified = true
                    }
            }

        }
    }
}
