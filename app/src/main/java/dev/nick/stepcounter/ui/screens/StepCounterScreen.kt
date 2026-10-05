package dev.nick.stepcounter.ui.screens


import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.nick.stepcounter.R
import dev.nick.stepcounter.ui.components.DailyStatsDashboard
import dev.nick.stepcounter.ui.components.ImportWarningDialog
import dev.nick.stepcounter.ui.components.LanguageSelectionDialog
import dev.nick.stepcounter.ui.components.MeasurementPromptDialog
import dev.nick.stepcounter.ui.components.SettingsMenu
import dev.nick.stepcounter.ui.components.StepProgressIndicator
import dev.nick.stepcounter.ui.viewmodel.StepCounterEvent
import dev.nick.stepcounter.ui.viewmodel.StepCounterUiState
import dev.nick.stepcounter.ui.viewmodel.StepCounterViewModel
import dev.nick.stepcounter.ui.viewmodel.UserProfileFormState
import dev.nick.stepcounter.util.changeAppLanguage
import dev.nick.stepcounter.util.hasRequiredPermissions

private const val CSV_MIME_TYPE = "text/csv"
private const val BACKUP_FILE_NAME = "\nCounter_backup.csv"

@Composable
fun StepCounterRoute(
    modifier: Modifier = Modifier,
    viewModel: StepCounterViewModel = hiltViewModel(),
    onRequestPermissions: () -> Unit
)
{
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val formState by viewModel.formState.collectAsStateWithLifecycle()
    val isTrackingActive = context.hasRequiredPermissions()
    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                is StepCounterEvent.ShowMessage -> {
                    Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument(CSV_MIME_TYPE)
    ) { uri ->
        uri?.let { viewModel.exportDataToCsv(it) }
    }
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { viewModel.importDataFromCsv(it) }
    }
    StepCounterScreen(
        modifier = modifier,
        uiState = uiState,
        formState = formState,
        isTrackingActive = isTrackingActive,
        onRequestPermissions = onRequestPermissions,
        onExportClick = { exportLauncher.launch(BACKUP_FILE_NAME) },
        onImportClick = { importLauncher.launch(arrayOf(CSV_MIME_TYPE, "*/*")) },
        onInitForm = viewModel::initForm,
        onHeightChange = viewModel::onHeightChange,
        onWeightChange = viewModel::onWeightChange,
        onStepsGoalChange = viewModel::onStepsGoalChange,
        onSaveUserData = viewModel::saveUserData
    )
}
@Composable
fun StepCounterScreen(
    modifier:Modifier=Modifier,
    uiState: StepCounterUiState,
    formState: UserProfileFormState,
    isTrackingActive: Boolean,
    onRequestPermissions: () -> Unit,
    onExportClick: () -> Unit,
    onImportClick: () -> Unit,
    onInitForm: (Int, Double, Int) -> Unit,
    onHeightChange: (String) -> Unit,
    onWeightChange: (String) -> Unit,
    onStepsGoalChange: (String) -> Unit,
    onSaveUserData: (String, String, String) -> Unit
){

    var showDialog by remember { mutableStateOf(false) }
    var showImportWarning by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }




    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = stringResource(R.string.user_steps),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            StepProgressIndicator(uiState.steps,uiState.userStepsGoal.coerceAtLeast(1))

            DailyStatsDashboard(
                distanceMeters = uiState.distanceMeters,
                distanceKm = uiState.distanceKm,
                minutesOfWalking = uiState.minutesOfWalking,
                caloriesBurned = uiState.caloriesBurned,
            )


            Button(onClick = {
                if(!isTrackingActive) {
                    onRequestPermissions()
                }
                onInitForm(
                    uiState.userHeight,
                    uiState.userWeight,
                    uiState.userStepsGoal
                )
                showDialog = true

            } ) {
                    Text(stringResource(if (!isTrackingActive) R.string.start_tracking else R.string.update_user_data ))

            }


            if (showDialog) {
                MeasurementPromptDialog(
                    formState = formState,
                    onHeightChange = onHeightChange,
                    onWeightChange = onWeightChange,
                    onStepsGoalChange = onStepsGoalChange,
                    onDismiss = { showDialog = false },
                    onConfirm = {
                        onSaveUserData(formState.heightInput, formState.weightInput,formState.stepsGoalInput)
                        showDialog = false
                    }
                )
            }


        }

        SettingsMenu(
            isTrackingActive = isTrackingActive,
            onLanguageClick = {showLanguageDialog = true},
            onExportClick = onExportClick,
            onImportClick = {showImportWarning=true},
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
        )


        if(showImportWarning)
        {
            ImportWarningDialog(
                onDismiss = { showImportWarning = false },
                onConfirm = {
                    showImportWarning = false
                    onImportClick()
                }
            )
        }

        if(showLanguageDialog)
        {
            LanguageSelectionDialog(
                onDismiss = { showLanguageDialog = false },
                onLanguageSelected = { languageCode ->
                    showLanguageDialog = false
                    changeAppLanguage(languageCode)
                }
            )
        }

    }
}

