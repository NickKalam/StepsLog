package dev.nick.stepcounter.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import dev.nick.stepcounter.R
import dev.nick.stepcounter.ui.viewmodel.UserProfileFormState

@Composable
fun MeasurementPromptDialog(
    formState: UserProfileFormState,
    onHeightChange: (String) -> Unit,
    onWeightChange: (String) -> Unit,
    onStepsGoalChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {


    AlertDialog(
        onDismissRequest = { onDismiss() },
        title = { Text(stringResource(R.string.enter_measurements)) },
        text = {
            Column {
                OutlinedTextField(
                    value = formState.heightInput,
                    onValueChange = onHeightChange,
                    label = { Text(stringResource(R.string.user_height)) },
                    placeholder = { Text(stringResource(R.string.height_placeholder)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    isError=formState.showHeightError,
                    supportingText = {
                        if (formState.showHeightError) {
                            Text(stringResource(R.string.height_error), color = MaterialTheme.colorScheme.error)
                        }
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = formState.weightInput,
                    onValueChange = onWeightChange,
                    label = { Text(stringResource(R.string.user_weight)) },
                    placeholder= { Text(stringResource(R.string.weight_placeholder)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    isError=formState.showWeightError,
                    supportingText = {
                        if (formState.showWeightError) {
                            Text(stringResource(R.string.weight_error), color = MaterialTheme.colorScheme.error)
                        }
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))


                OutlinedTextField(
                    value = formState.stepsGoalInput,
                    onValueChange = onStepsGoalChange,
                    label = { Text(stringResource(R.string.user_steps_goal)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    isError=formState.showStepsGoalError,
                    supportingText = {
                        if (formState.showStepsGoalError) {
                            Text(stringResource(R.string.daily_steps_goal_error), color = MaterialTheme.colorScheme.error)
                        }
                    }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled=formState.isFormValid
            ) {
                Text(stringResource(R.string.save_button))
            }
        },
        dismissButton = {
            TextButton(onClick = { onDismiss() }) {
                Text(stringResource(R.string.cancel_button))
            }
        }
    )
}