@file:OptIn(ExperimentalFoundationApi::class)

package com.example.dive.presentation

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.wear.compose.material.*
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import androidx.compose.ui.Alignment // Add this import
import androidx.compose.foundation.layout.Column // Add this import
import androidx.wear.compose.material.CircularProgressIndicator // Add this import
import androidx.wear.compose.material.Text // Add this import
import androidx.compose.foundation.layout.Box // Add this import
import androidx.wear.compose.material.MaterialTheme // Add this import
import androidx.lifecycle.lifecycleScope // Add this import
import com.example.dive.data.HealthRepository // Add this import
import com.example.dive.service.HeartRateMonitoringService
import java.util.concurrent.TimeUnit // Add this import
import com.example.dive.emergency.EmergencyManager
import com.example.dive.presentation.theme.DiveTheme
import com.example.dive.presentation.ui.CastingScreen
import com.example.dive.presentation.ui.DetailedWeatherScreen
import com.example.dive.presentation.ui.EmergencyScreen
import com.example.dive.presentation.ui.FishingPointsScreen
import com.example.dive.presentation.ui.SettingsScreen
import com.example.dive.presentation.ui.TideScreen
import com.example.dive.presentation.ui.WeatherScreen
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.cancel
import android.util.Log

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()
    private lateinit var healthRepository: HealthRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        healthRepository = HealthRepository(this)

        // Observe the isMonitoringEnabled state from ViewModel
        lifecycleScope.launch {
            viewModel.isMonitoringEnabled.collect { enabled ->
                val serviceIntent =
                    Intent(this@MainActivity, HeartRateMonitoringService::class.java)
                if (enabled) {
                    startService(serviceIntent)
                } else {
                    stopService(serviceIntent)
                }
            }
        }

        setContent {
            val activityViewModel = this@MainActivity.viewModel
            val navController = rememberSwipeDismissableNavController()
            DiveTheme {
                val initialSyncDone by viewModel.initialSyncDoneFlow.collectAsState()
                val syncHint by viewModel.syncHintState.collectAsState()

                if (!initialSyncDone && syncHint != SyncHint.PROMPT) {
                    // Show a simple loading screen while initial data is fetched
                    FullScreenLoadingScreen()
                } else {
                    SwipeDismissableNavHost(
                        navController = navController,
                        startDestination = "main"
                    ) {
                        composable("main") {
                            MainApp(viewModel = activityViewModel, navController)
                        }
                        composable("casting") {
                            CastingScreen(navController)
                        }
                    }
                }
            }
        }
    }

    private fun requestPermissions() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.BODY_SENSORS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.BODY_SENSORS,
                    Manifest.permission.ACTIVITY_RECOGNITION),
                REQUEST_BODY_SENSORS_PERMISSION
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_BODY_SENSORS_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted
            } else {
                // Permission denied
            }
        }
    }



    companion object {
        private const val REQUEST_BODY_SENSORS_PERMISSION = 1
        private const val HEART_RATE_MEASUREMENT_WORK_TAG = "HeartRateMeasurementWork"
        private const val BACK_BUTTON_LONG_PRESS_DURATION = 3000L // 3 seconds
    }

    private var longPressJob: Job? = null
    private val TAG = "MainActivity"

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        Log.d(TAG, "onKeyDown: keyCode=$keyCode, action=${event?.action}, isLongPress=${event?.isLongPress}")
        if (keyCode == KeyEvent.KEYCODE_CALL) {
            if (event?.action == KeyEvent.ACTION_DOWN) {
                Log.d(TAG, "onKeyDown: KEYCODE_CALL ACTION_DOWN detected.")
                if (longPressJob == null || longPressJob?.isActive == false) {
                    Log.d(TAG, "onKeyDown: Starting long press job.")
                    longPressJob = lifecycleScope.launch {
                        delay(BACK_BUTTON_LONG_PRESS_DURATION)
                        Log.d(TAG, "onKeyDown: Long press duration reached. Triggering SOS.")
                        EmergencyManager.triggerEmergencySOS(this@MainActivity, "뒤로가기 버튼 3초 길게 누름")
                    }
                }
                return true // Consume the event to prevent default handling
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        Log.d(TAG, "onKeyUp: keyCode=$keyCode, action=${event?.action}")
        if (keyCode == KeyEvent.KEYCODE_CALL) {
            Log.d(TAG, "onKeyUp: KEYCODE_CALL detected. Cancelling long press job.")
            longPressJob?.cancel()
            longPressJob = null // Reset the job
            return true // Consume the event
        }
        return super.onKeyUp(keyCode, event)
    }
}

@Composable
fun FullScreenLoadingScreen() {
    Box(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colors.background),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
            Text("데이터 동기화 중...", modifier = Modifier.padding(top = 16.dp), color = MaterialTheme.colors.onBackground)
        }
    }
}

@Composable
fun MainApp(
    viewModel: MainViewModel, navController: NavHostController
) {
    val tideUiState by viewModel.tideUiState.collectAsState()
    val tideWeeklyState by viewModel.tideWeeklyState.collectAsState()
    val weatherUiState by viewModel.weatherUiState.collectAsState()
    val detailedWeatherUiState by viewModel.detailedWeatherUiState.collectAsState()
    val fishingPointsUiState by viewModel.fishingPointsUiState.collectAsState()
    val emergencyUiState by viewModel.emergencyUiState.collectAsState()

    // 🔹 뒤로가기를 위해 ViewModel에 저장된 페이지에서 시작하도록 수정
    val pagerState = rememberPagerState(initialPage = viewModel.lastPagerPage, pageCount = { 6 })

    var currentTime by remember { mutableStateOf(System.currentTimeMillis()) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(TimeUnit.SECONDS.toMillis(1)) // Update every seconds
            currentTime = System.currentTimeMillis()
        }
    }

    val timeSource = object : TimeSource {
        override val currentTime: String
            @Composable
            get() = SimpleDateFormat("a hh:mm", Locale.KOREA).format(currentTime)
    }

    Scaffold(
        timeText = { TimeText(timeSource = timeSource) },
        pageIndicator = {
            HorizontalPageIndicator(
                pageIndicatorState = object : PageIndicatorState {
                    override val pageOffset: Float
                        get() = pagerState.currentPageOffsetFraction
                    override val selectedPage: Int
                        get() = pagerState.currentPage
                    override val pageCount: Int
                        get() = pagerState.pageCount
                },
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
    ) {
        Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colors.background)) {
            HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                when (page) {
                    0 -> TideScreen(uiState = tideUiState, tideWeekly = tideWeeklyState)
                    1 -> WeatherScreen(uiState = weatherUiState)
                    2 -> DetailedWeatherScreen(uiState = detailedWeatherUiState)
                    3 -> FishingPointsScreen(uiState = fishingPointsUiState, viewModel = viewModel, pagerState = pagerState)
                    4 -> EmergencyScreen(viewModel = viewModel)
                    5 -> SettingsScreen(viewModel = viewModel, navController = navController)
                }
            }
        }
    }
}