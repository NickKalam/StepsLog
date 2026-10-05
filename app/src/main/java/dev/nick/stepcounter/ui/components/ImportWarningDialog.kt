package dev.nick.stepcounter.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import dev.nick.stepcounter.R

@Composable
fun ImportWarningDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
)
{

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.overwrite_data_title)) },
        text = { Text(stringResource(R.string.overwrite_data_text)) },
        confirmButton = {
            Button(
                onClick = onConfirm
            ) {
                Text(stringResource(R.string.import_button_text))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel_button))
            }
        }
    )
}