package dev.nick.stepcounter.domain.usecase

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.nick.stepcounter.data.repository.StepsRepository
import dev.nick.stepcounter.data.repository.UserPreferencesRepository
import dev.nick.stepcounter.domain.calculator.calculateActiveCaloriesDelta
import dev.nick.stepcounter.domain.util.TimeProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import javax.inject.Inject

class StepRecordsTransferUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val stepsRepository: StepsRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val timeProvider: TimeProvider
) {
    suspend fun exportDataToCsv(uri:Uri): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val steps = stepsRepository.getAllStepsSync()

            val csvData = buildString {
                append("timestamp,steps\n")
                steps.forEach {
                    append("${it.timestamp},${it.steps}\n")
                }
            }

            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.write(csvData.toByteArray())
            } ?: throw IllegalStateException("Unable to open output stream")
        }
    }


    suspend fun importDataFromCsv(uri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val importedRecords = mutableListOf<Pair<Long, Int>>()
            var todayImportedSteps = 0
            val todayStart = timeProvider.getMidnightToday()

            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).useLines { lines ->
                    lines.drop(1).forEach { line ->
                        val parts = line.split(",")
                        if (parts.size == 2) {
                            val timestamp = parts[0].toLongOrNull()
                            val steps = parts[1].toIntOrNull()
                            if (timestamp != null && steps != null) {
                                importedRecords.add(Pair(timestamp, steps))

                                if (timestamp >= todayStart) {
                                    todayImportedSteps += steps
                                }
                            }
                        }
                    }
                }
            } ?: throw IllegalStateException("Unable to open input stream")

            if (importedRecords.isNotEmpty()) {
                stepsRepository.restoreHistory(importedRecords)

                if (todayImportedSteps > 0) {
                    val profile = userPreferencesRepository.userProfileFlow.first()

                    val estimatedMinutes = todayImportedSteps / 90.0
                    val estimatedCalories = calculateActiveCaloriesDelta(
                        cadence = 90,
                        weight = profile.weight,
                        minutes = estimatedMinutes
                    )

                    userPreferencesRepository.updateUserActiveMinutes(estimatedMinutes)
                    userPreferencesRepository.updateUserActiveCalories(estimatedCalories)
                }
            }
        }
    }

}