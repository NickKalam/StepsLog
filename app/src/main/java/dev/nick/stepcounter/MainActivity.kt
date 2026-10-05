package dev.nick.stepcounter

import android.Manifest
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import dagger.hilt.android.AndroidEntryPoint
import dev.nick.stepcounter.receiver.DateChangeReceiver
import dev.nick.stepcounter.service.StepTrackingService
import dev.nick.stepcounter.ui.screens.StepCounterRoute
import dev.nick.stepcounter.ui.screens.StepHistoryRoute
import dev.nick.stepcounter.ui.theme.StepCounterTheme
import dev.nick.stepcounter.ui.viewmodel.StepCounterViewModel

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    enum class AppScreen(val titleResId: Int) {
        TODAY(R.string.tab_today),
        HISTORY(R.string.tab_history)
    }
    private val viewModel: StepCounterViewModel by viewModels()

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.entries.all { it.value }
        if (allGranted) {
            startStepService()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startServiceIfPermissionsGranted()

        setContent {
            StepCounterTheme {
                // Save the enum name as a string so rememberSaveable handles device rotations automatically
                var selectedScreenName by rememberSaveable {
                    mutableStateOf(AppScreen.TODAY.name)
                }
                val currentScreen = AppScreen.valueOf(selectedScreenName)

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        TabRow(selectedTabIndex = currentScreen.ordinal) {
                            AppScreen.entries.forEach { screen ->
                                Tab(
                                    selected = currentScreen == screen,
                                    onClick = { selectedScreenName = screen.name },
                                    text = { Text(stringResource(screen.titleResId)) }
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        when (currentScreen) {
                            AppScreen.TODAY -> StepCounterRoute(
                                modifier = Modifier.fillMaxSize(),
                                onRequestPermissions = {
                                    checkAndRequestPermissions()
                                }
                            )

                            AppScreen.HISTORY -> StepHistoryRoute(
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }
        }

    }

    override fun onStart() {
        super.onStart()
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_DATE_CHANGED)
            addAction(Intent.ACTION_TIME_CHANGED)
            addAction(Intent.ACTION_TIMEZONE_CHANGED)
        }
        registerReceiver(midnightReceiver, filter)
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshDateIfNeeded()
    }

    override fun onStop() {
        super.onStop()
        // Stop listening when the user minimizes the app
        unregisterReceiver(midnightReceiver)
    }

    private fun checkAndRequestPermissions() {
        val permissionsToRequest = mutableListOf<String>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            permissionsToRequest.add(Manifest.permission.ACTIVITY_RECOGNITION)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        if (permissionsToRequest.isNotEmpty()) {
            requestPermissionLauncher.launch(permissionsToRequest.toTypedArray())
        } else {
            startStepService()
        }
    }

    private val midnightReceiver = DateChangeReceiver {
        viewModel.refreshDateIfNeeded()
    }

    private fun startStepService() {
        val serviceIntent = Intent(this, StepTrackingService::class.java)
        ContextCompat.startForegroundService(this, serviceIntent)
    }

    private fun startServiceIfPermissionsGranted() {
        val hasActivityPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACTIVITY_RECOGNITION
            ) == PackageManager.PERMISSION_GRANTED
        } else true // Granted on install for older OS

        val hasNotificationPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else true

        if( hasActivityPermission && hasNotificationPermission)
            startStepService()
    }
}