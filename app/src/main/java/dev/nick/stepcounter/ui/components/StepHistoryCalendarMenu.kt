package dev.nick.stepcounter.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import dev.nick.stepcounter.R


@Composable
fun StepHistoryCalendarMenu(
    numberOfDays:Long,
    onWeek:()->Unit,
    onMonth:()->Unit,
    onThreeMonths:()->Unit,
    onYear:()->Unit,
    modifier: Modifier = Modifier
)
{

    var menuExpanded by remember { mutableStateOf(false) }
    val expandedDesc = stringResource(R.string.menu_expanded_desc)
    val collapsedDesc = stringResource(R.string.menu_collapsed_desc)
    Box(modifier =  modifier
        .fillMaxSize()
        .padding(16.dp),) {

        Button(
            onClick = { menuExpanded = !menuExpanded },
            Modifier.fillMaxWidth(),

            shape = RectangleShape,
            colors= ButtonDefaults.buttonColors(
                containerColor = Color.Gray,
                contentColor = Color.White)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {

                Text(
                    text=if (menuExpanded) "▲" else "▼",
                    modifier = Modifier.clearAndSetSemantics {
                        contentDescription = if (menuExpanded) expandedDesc else collapsedDesc
                    })
                Text(stringResource(R.string.history_subtitle, numberOfDays))
            }
        }


        DropdownMenu(
            expanded = menuExpanded,
            onDismissRequest = { menuExpanded = false }
        ) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.history_subtitle,7L)) },
                onClick = {
                    menuExpanded = false
                    onWeek()
                }
            )
            DropdownMenuItem(
                text={ Text(stringResource(R.string.history_subtitle,30L)) },
                onClick = {
                    menuExpanded=false
                    onMonth()
                })
            DropdownMenuItem(
                text={  Text(stringResource(R.string.history_subtitle,90L))},
                onClick = {
                    menuExpanded=false
                    onThreeMonths()
                })
            DropdownMenuItem(
                text={  Text(stringResource(R.string.history_subtitle,365L))},
                onClick = {
                    menuExpanded=false
                    onYear()
                })
        }
    }

}