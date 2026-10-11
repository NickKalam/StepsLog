package dev.nick.stepcounter.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.nick.stepcounter.R
import dev.nick.stepcounter.data.dao.DailyStepTotal
import dev.nick.stepcounter.ui.components.SelectedDayCell
import dev.nick.stepcounter.ui.viewmodel.StepCounterViewModel
import java.text.NumberFormat
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun StepHistoryRoute(
    modifier: Modifier = Modifier,
    viewModel: StepCounterViewModel = hiltViewModel()
) {
    val history by viewModel.stepHistory.collectAsStateWithLifecycle()
    val today by remember { mutableStateOf(LocalDate.now()) }
    var selectedDay by rememberSaveable { mutableStateOf(today.toString()) }
    var currentDisplayedMonth by rememberSaveable { mutableStateOf(YearMonth.now()) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        val newToday = LocalDate.now()
        viewModel.refreshDateIfNeeded()

        if (newToday != today) {
            selectedDay = newToday.toString()
            currentDisplayedMonth=YearMonth.now()
        }
    }

    StepHistoryScreen(
        modifier = modifier,
        history = history,
        selectedDay = selectedDay,
        currentMonth = currentDisplayedMonth,
        onDaySelected = { selectedDay = it },
        onMonthChanged={ currentDisplayedMonth = it }
    )
}

@Composable
fun StepHistoryScreen(
    modifier: Modifier = Modifier,
    history: List<DailyStepTotal>,
    selectedDay: String,
    currentMonth: YearMonth,
    onDaySelected: (String) -> Unit,
    onMonthChanged: (YearMonth) -> Unit
) {
    val daysInMonth = remember(currentMonth) {
        (1..currentMonth.lengthOfMonth()).map { currentMonth.atDay(it) }
    }

    val stepsByDate = remember(history) {
        history.associateBy { it.day }
    }

    val selectedRecord = stepsByDate[selectedDay]

    val dateFormatter = remember {
        DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", Locale.getDefault())
    }

    val monthFormatter = remember {
        DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(7),
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        item(span = { GridItemSpan(7) }) {
            Spacer(modifier = Modifier.height(16.dp))
        }

        item(span = { GridItemSpan(7) }) {
            Text(
                text = stringResource(R.string.history_title),
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }

        item(span = { GridItemSpan(7) }) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { onMonthChanged(currentMonth.minusMonths(1)) }) {
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = "Previous Month"
                    )
                }

                Text(
                    text = currentMonth.format(monthFormatter),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary
                )

                val isCurrentOrFutureMonth = currentMonth >= YearMonth.now()
                IconButton(
                    onClick = { onMonthChanged(currentMonth.plusMonths(1)) },
                    enabled = !isCurrentOrFutureMonth
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "Next Month"
                    )
                }
            }
        }

        item(span = { GridItemSpan(7) }) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            ) {
                DayOfWeek.entries.forEach { day ->
                    Text(
                        text = day.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        val firstDayOfWeek = daysInMonth.first().dayOfWeek.value - 1
        items(firstDayOfWeek, key = { "empty_start_$it" }, contentType = { "empty_spacer" }) {
            Spacer(modifier = Modifier.aspectRatio(1f))
        }

        items(
            items = daysInMonth,
            key = { date -> date.toEpochDay() },
            contentType = { "day_cell" }
        ) { date ->
            val dateKey = date.toString()
            val hasRecord = stepsByDate.containsKey(dateKey)
            val isSelected = selectedDay == dateKey

            SelectedDayCell(
                date = date,
                isSelected = isSelected,
                hasRecord = hasRecord,
                onClick = { onDaySelected(dateKey) }
            )
        }

        val lastDayOfWeek = daysInMonth.last().dayOfWeek.value
        val trailingEmpty = (7 - lastDayOfWeek) % 7
        items(trailingEmpty, key = { "empty_end_$it" }, contentType = { "empty_spacer" }) {
            Spacer(modifier = Modifier.aspectRatio(1f))
        }

        item(span = { GridItemSpan(7) }) {
            OutlinedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 32.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = LocalDate.parse(selectedDay).format(dateFormatter),
                        style = MaterialTheme.typography.titleMedium
                    )

                    Text(
                        text = if (selectedRecord != null)
                            stringResource(
                                R.string.history_steps_count,
                                NumberFormat.getIntegerInstance().format(selectedRecord.totalSteps)
                            )
                        else
                            stringResource(R.string.history_no_activity),
                        style = MaterialTheme.typography.headlineSmall
                    )
                }
            }
        }
    }
}