@file:OptIn(ExperimentalFoundationApi::class)

package com.example.dive.presentation

import android.Manifest
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
import com.example.dive.emergency.EmergencyManager
import com.example.dive.presentation.theme.DiveTheme
import com.example.dive.presentation.ui.CastingScreen
import com.example.dive.presentation.ui.DetailedWeatherScreen
import com.example.dive.presentation.ui.EmergencyScreen
import com.example.dive.presentation.ui.FishingPointsScreen
import com.example.dive.presentation.ui.SettingsScreen
import com.example.dive.presentation.ui.TideScreen
import com.example.dive.presentation.ui.WeatherScreen
import java.text.SimpleDateFormat
import java.util.Locale

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    // For Crown Press SOS
    private var crownPressCount = 0
    private var lastCrownPressTime = 0L
    private val CROWN_PRESS_TIMEOUT = 2000L // 2 seconds

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestPermissions()

        setContent {
            val activityViewModel = this@MainActivity.viewModel
            val navController = rememberSwipeDismissableNavController()
            DiveTheme {
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
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_STEM_PRIMARY) {
            val currentTime = System.currentTimeMillis()

            if (currentTime - lastCrownPressTime > CROWN_PRESS_TIMEOUT) {
                crownPressCount = 1
            } else {
                crownPressCount++
            }
            lastCrownPressTime = currentTime

            if (crownPressCount >= 3) {
                EmergencyManager.triggerEmergencySOS(this, "크라운 3회 누름")
                crownPressCount = 0
            }
            return true
        }
        return super.onKeyDown(keyCode, event)
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

    val timeSource = object : TimeSource {
        override val currentTime: String
            @Composable
            get() = remember { SimpleDateFormat("a hh:mm", Locale.KOREA).format(System.currentTimeMillis()) }
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
