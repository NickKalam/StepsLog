package dev.nick.stepcounter.data.sensor

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import kotlin.math.abs

class StepsSensorListener(
    private val timeProvider: dev.nick.stepcounter.domain.util.TimeProvider,
    private val onStepsDetected: (steps:Int,timestamp:Long) -> Unit
) : SensorEventListener {

    var onCadenceUpdated: ((Int) -> Unit)? = null
    var currentCadence: Int = 90
        set(value) {
            field = value
            internalCadence = value
        }

    private var internalCadence:Int=90
    private var minuteAccumulatorStart: Long = 0L
    private var stepsInCurrentMinute: Int = 0
    companion object {
        private const val MIN_ACTIVE_STEPS = 60
        private const val DEVIATION_THRESHOLD_NORMAL = 0.11
        private const val DEVIATION_THRESHOLD_HIGH = 0.08
        private const val ONE_MINUTE_MS= 60_000L
    }


    private var lastReportedSteps = -1
    private var previousTimestamp: Long = 0L // Tracks time for interpolation

    var onHardwareTotalUpdated: ((Int) -> Unit)? = null


    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_STEP_COUNTER ) {
            val totalSteps = event.values[0].toInt()
            val currentTimestamp=timeProvider.currentTimeMillis()
            if(lastReportedSteps==-1)
            {
                    lastReportedSteps = totalSteps
                    previousTimestamp = currentTimestamp
                    minuteAccumulatorStart = currentTimestamp
                    return
            }
            //If the hardware total is suddenly lower than the saved last reported steps,
            //the device rebooted . Reset the last reported steps
            if (totalSteps < lastReportedSteps) {
                lastReportedSteps = 0
            }
            val newSteps = totalSteps - lastReportedSteps
            lastReportedSteps = totalSteps
            val timeElapsedMs = currentTimestamp - previousTimestamp
            previousTimestamp = currentTimestamp

            if (newSteps > 0) {
                val minutesPassed = (timeElapsedMs / ONE_MINUTE_MS).toInt()

                if (minutesPassed > 1) {
                    val estimatedActiveMinutes = maxOf(1, newSteps / currentCadence)

                    //  Ensure no more minutes are calculated than actually passed
                    val distributionMinutes = minOf(minutesPassed, estimatedActiveMinutes)

                    //  Divide the steps densely into only the active minutes
                    val stepsPerMinute = newSteps / distributionMinutes
                    val remainderSteps = newSteps % distributionMinutes

                    for (i in 1..distributionMinutes) {

                        checkAndTriggerCadenceUpdate(stepsPerMinute)
                        // Log the steps backwards from the current timestamp
                        val interpolatedTime = currentTimestamp - ((distributionMinutes - i + 1) * ONE_MINUTE_MS)

                        val stepsToLog = if (i == distributionMinutes) stepsPerMinute + remainderSteps else stepsPerMinute

                        if (stepsToLog > 0) {
                            onStepsDetected(stepsToLog, interpolatedTime)
                            onHardwareTotalUpdated?.invoke(lastReportedSteps)
                        }
                    }
                    minuteAccumulatorStart = currentTimestamp
                    stepsInCurrentMinute = 0
                } else {
                    stepsInCurrentMinute += newSteps
                    val realTimeElapsed = currentTimestamp - minuteAccumulatorStart

                    if (realTimeElapsed >= ONE_MINUTE_MS) {
                        // A full minute has passed, check the math
                        checkAndTriggerCadenceUpdate(stepsInCurrentMinute)
                        onHardwareTotalUpdated?.invoke(lastReportedSteps)
                        // Reset the accumulator for the next minute
                        minuteAccumulatorStart = currentTimestamp
                        stepsInCurrentMinute = 0
                    }
                    onStepsDetected(newSteps, currentTimestamp)
                    onHardwareTotalUpdated?.invoke(lastReportedSteps)
                }
            }
        }
    }

    fun setLastReportedSteps(savedBaseline:Int,currentTimestamp:Long){
        lastReportedSteps = savedBaseline
        previousTimestamp = currentTimestamp
        minuteAccumulatorStart = currentTimestamp
    }
    private fun checkAndTriggerCadenceUpdate(stepsInMinute: Int) {

        if (stepsInMinute >= MIN_ACTIVE_STEPS ) {
            internalCadence=((internalCadence * 0.80) + (stepsInMinute * 0.20)).toInt()
            val deviation = abs(internalCadence - currentCadence).toDouble()/currentCadence
            val threshold= if (currentCadence<110) DEVIATION_THRESHOLD_NORMAL else  DEVIATION_THRESHOLD_HIGH
            if(deviation >= threshold)
                onCadenceUpdated?.invoke(internalCadence)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}