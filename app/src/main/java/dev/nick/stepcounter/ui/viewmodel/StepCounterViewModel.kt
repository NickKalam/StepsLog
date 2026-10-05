package dev.nick.stepcounter.ui.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.nick.stepcounter.data.dao.DailyStepTotal
import dev.nick.stepcounter.data.repository.StepsRepository
import dev.nick.stepcounter.data.repository.UserPreferencesRepository
import dev.nick.stepcounter.domain.usecase.ObserveDailyStatsUseCase
import dev.nick.stepcounter.domain.usecase.StepRecordsTransferUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject
import kotlin.math.floor

sealed interface StepCounterEvent {
    data class ShowMessage(val message: String) : StepCounterEvent
}
@HiltViewModel
class StepCounterViewModel @Inject constructor(
    private val stepsRepository: StepsRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val observeDailyStatsUseCase: ObserveDailyStatsUseCase,
    private val timeProvider: dev.nick.stepcounter.domain.util.TimeProvider,
    private val stepRecordsTransferUseCase: StepRecordsTransferUseCase
) : ViewModel(){



    private val _uiEvent = Channel<StepCounterEvent>()
    val uiEvent = _uiEvent.receiveAsFlow()
    private val _startOfDay = MutableStateFlow(timeProvider.getMidnightToday())
    private val _historyDays = MutableStateFlow(30L)
    val historyDays: StateFlow<Long> = _historyDays.asStateFlow()

    fun refreshDateIfNeeded()
    {
        val actualMidnight= timeProvider.getMidnightToday()
        if(_startOfDay.value!=actualMidnight)
            _startOfDay.value=actualMidnight
    }


    fun saveUserData(heightInput: String, weightInput: String,stepsGoalInput:String) {
        if (!formState.value.isFormValid) return
        val height = heightInput.toIntOrNull()
        val weight = weightInput.toDoubleOrNull()
        val stepsGoal=stepsGoalInput.toIntOrNull()
        viewModelScope.launch {
                userPreferencesRepository.saveUserData(height,weight,stepsGoal)
            }
    }
    private val _formState = MutableStateFlow(UserProfileFormState())
    val formState: StateFlow<UserProfileFormState> = _formState.asStateFlow()

    fun initForm(height: Int, weight: Double, goal: Int) {
        _formState.value = UserProfileFormState(
            heightInput = if (height > 0) height.toString() else "",
            weightInput = if (weight > 0.0) {
                if (weight % 1.0 == 0.0) weight.toInt().toString() else weight.toString()
            } else "",
            stepsGoalInput = if (goal > 0) goal.toString() else ""
        )
        validateForm()
    }

    fun onHeightChange(value: String) {
        _formState.value = _formState.value.copy(heightInput = value)
        validateForm()
    }

    fun onWeightChange(value: String) {
        _formState.value = _formState.value.copy(weightInput = value)
        validateForm()
    }

    fun onStepsGoalChange(value: String) {
        _formState.value = _formState.value.copy(stepsGoalInput = value)
        validateForm()
    }

    private fun validateForm() {
        val state = _formState.value
        val isHeightValid = state.heightInput.toIntOrNull()?.let { it in 50..250 } == true
        val isWeightValid = state.weightInput.toDoubleOrNull()?.let { it in 20.0..300.0 } == true
        val isStepsGoalValid = state.stepsGoalInput.toIntOrNull()?.let { it in 500..100000 } == true

        _formState.value = state.copy(
            showHeightError = state.heightInput.isNotEmpty() && !isHeightValid,
            showWeightError = state.weightInput.isNotEmpty() && !isWeightValid,
            showStepsGoalError = state.stepsGoalInput.isNotEmpty() && !isStepsGoalValid,
            isFormValid = isHeightValid && isWeightValid && isStepsGoalValid
        )
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<StepCounterUiState> = _startOfDay
        .flatMapLatest { start ->
            // Calculate end of day by adding 24 hours (86,400,000 ms)
            val zone = ZoneId.systemDefault()

            val end = Instant.ofEpochMilli(start)
                .atZone(zone)
                .toLocalDate()
                .plusDays(1)
                .atStartOfDay(zone)
                .toInstant()
                .toEpochMilli()

            observeDailyStatsUseCase(start, end).map { stats ->
                StepCounterUiState(
                    steps = stats.steps,
                    distanceMeters = stats.distanceMeters,
                    distanceKm = floor(stats.distanceKm),
                    minutesOfWalking = stats.minutesOfWalking,
                    caloriesBurned = stats.caloriesBurned,
                    userHeight = stats.userHeight,
                    userWeight = stats.userWeight,
                    userStepsGoal = stats.userStepsGoal
                )
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = StepCounterUiState()
        )

    fun setHistoryDays(days: Long) {
        _historyDays.value = days
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val stepHistory: StateFlow<List<DailyStepTotal>> =
        combine(_startOfDay, _historyDays) { _, days -> days }
            .flatMapLatest { days ->
                stepsRepository.getStepHistory(days)
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

    //Import/Export steps data


    fun exportDataToCsv(uri: Uri) {
        viewModelScope.launch {
            stepRecordsTransferUseCase.exportDataToCsv(uri)
                .onSuccess {
                    _uiEvent.send(StepCounterEvent.ShowMessage("Export successful"))
                }
                .onFailure { error ->
                    _uiEvent.send(StepCounterEvent.ShowMessage("Export failed: ${error.localizedMessage}"))
                    error.printStackTrace()
                }
        }
    }

    fun importDataFromCsv(uri: Uri) {
        viewModelScope.launch {
            stepRecordsTransferUseCase.importDataFromCsv(uri)
                .onSuccess {
                    _uiEvent.send(StepCounterEvent.ShowMessage("Import successful"))
                    refreshDateIfNeeded() // Refresh UI data if import changes today's stats
                }
                .onFailure { error ->
                    _uiEvent.send(StepCounterEvent.ShowMessage("Import failed: ${error.localizedMessage}"))
                    error.printStackTrace()
                }
        }
    }

}


data class StepCounterUiState(
    val steps: Int = 0,
    val distanceMeters: Double = 0.0,
    val distanceKm: Double = 0.0,
    val minutesOfWalking: Int = 0,
    val caloriesBurned: Double = 0.0,
    val userHeight: Int = 0,
    val userWeight: Double = 0.0,
    val userStepsGoal: Int = 7000
)

data class UserProfileFormState(
    val heightInput: String = "",
    val weightInput: String = "",
    val stepsGoalInput: String = "",
    val showHeightError: Boolean = false,
    val showWeightError: Boolean = false,
    val showStepsGoalError: Boolean = false,
    val isFormValid: Boolean = false
)
