package dev.nick.stepcounter.domain.usecase

import dev.nick.stepcounter.data.repository.StepsRepository
import dev.nick.stepcounter.data.repository.UserPreferencesRepository
import dev.nick.stepcounter.domain.calculator.calculateKm
import dev.nick.stepcounter.domain.calculator.calculateMeters
import dev.nick.stepcounter.domain.model.DailyActivityStats
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

class ObserveDailyStatsUseCase @Inject constructor(
    private val stepsRepository: StepsRepository,
    private val userPreferencesRepository: UserPreferencesRepository
) {
    operator fun invoke(startOfDay: Long, endOfDay: Long): Flow<DailyActivityStats> {
        val stepsFlow = stepsRepository.getStepsForToday(startOfDay, endOfDay)
        val profileFlow = userPreferencesRepository.userProfileFlow

        return combine(stepsFlow, profileFlow) { steps, profile ->
            DailyActivityStats(
                steps = steps,
                distanceMeters = calculateMeters(steps, profile.height),
                distanceKm = calculateKm(steps, profile.height),
                minutesOfWalking = profile.activeMinutesToday.toInt(),
                caloriesBurned = profile.activeCaloriesToday,
                userStepsGoal = profile.stepsGoal,
                userHeight = profile.height,
                userWeight = profile.weight
            )
        }
    }
}