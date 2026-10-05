package dev.nick.stepcounter.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.nick.stepcounter.R

@Composable
fun DailyStatsDashboard(
    distanceMeters:Double,
    distanceKm:Double,
    minutesOfWalking:Int,
    caloriesBurned:Double
)
{
    Text(
        text= stringResource(R.string.distance_meters_format,distanceMeters),
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(vertical = 24.dp)
    )
    Text(
        text= stringResource(R.string.distance_km_floor_format,distanceKm),
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(vertical = 16.dp)
    )

    Text(
        text= stringResource(R.string.user_minutes) +":${minutesOfWalking}",
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(vertical = 16.dp)
    )

    Text(
        text= stringResource(R.string.calories_format, caloriesBurned),
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(vertical = 16.dp)
    )
}