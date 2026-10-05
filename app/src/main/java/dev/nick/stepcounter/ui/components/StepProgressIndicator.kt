package dev.nick.stepcounter.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.nick.stepcounter.R

@Composable
fun StepProgressIndicator(
    steps: Int,
    goal: Int
)
{
    val context = LocalContext.current
    val animatedProgress by animateFloatAsState(
        targetValue = (steps.toFloat() / goal.coerceAtLeast(1)).coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "StepGoalProgress"
    )

    Box(
        modifier = Modifier
            .padding(vertical = 32.dp)
            .semantics(mergeDescendants = true) {
                contentDescription = context.getString(R.string.step_progress_desc, steps, goal)
            },
        contentAlignment = Alignment.Center,
    )
    {
        CircularProgressIndicator(
            progress = { animatedProgress },
            modifier = Modifier.size(220.dp),
            color = MaterialTheme.colorScheme.primary,
            strokeWidth = 12.dp,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
        )
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = steps.toString(),
                fontSize = 56.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = stringResource(
                    R.string.out_of_goal_steps,
                    goal.coerceAtLeast(1)
                ),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
        }

    }
}