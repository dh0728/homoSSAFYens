package com.example.dive.presentation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.*
import com.example.dive.emergency.EmergencyManager
import com.example.dive.presentation.EmergencyUiState
import com.example.dive.presentation.theme.AccentRed
import com.example.dive.presentation.theme.BackgroundSecondary
import com.example.dive.presentation.theme.TextPrimary
import com.example.dive.presentation.theme.TextSecondary
import com.example.dive.presentation.theme.TextTertiary
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.dive.health.HealthMonitoringService
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.LocalContext
import com.example.dive.presentation.MeasurementState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.dive.presentation.MainViewModel

@Composable
fun EmergencyScreen(viewModel: MainViewModel = viewModel()) {
    val context = LocalContext.current
    val uiStateState: State<EmergencyUiState> = viewModel.emergencyUiState.collectAsState()
    Log.d("EmergencyScreen", "Recomposed with uiState: ${uiStateState.value}")

    val liveHeartRateState: State<Int> = viewModel.liveHeartRate.collectAsState()
    val measurementState: MeasurementState = viewModel.measurementState.collectAsState().value

    val currentUiState = uiStateState.value

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        when (currentUiState) {
            is EmergencyUiState.Loading -> {
                CircularProgressIndicator()
            }
            is EmergencyUiState.Error -> {
                Text(
                    text = "응급 정보를 가져올 수 없습니다.",
                    modifier = Modifier.padding(16.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    color = MaterialTheme.colors.error
                )
            }
            is EmergencyUiState.Success -> {
                EmergencyInfoCard(
                    context = context,
                    lastMeasured = currentUiState.lastMeasured,
                    locationStatus = currentUiState.locationStatus,
                    onSosClick = { EmergencyManager.triggerEmergencySOS(context, "수동 호출") },
                    measurementState = measurementState
                )
            }
        }
    }
}

@Composable
fun EmergencyInfoCard(
    context: Context,
    lastMeasured: String,
    locationStatus: String,
    onSosClick: () -> Unit,
    measurementState: MeasurementState // New parameter
) {
    Log.d("EmergencyInfoCard", "isMeasuring: ${measurementState.isMeasuring}, lastAverageHr: ${measurementState.lastAverageHr}, lastMeasured: $lastMeasured")
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceAround
    ) {
        // 긴급 상황
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(BackgroundSecondary)
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(imageVector = Icons.Filled.Warning, contentDescription = "SOS", tint = AccentRed, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "SOS", style = MaterialTheme.typography.title1, color = AccentRed)
        }

        Text(
            text = locationStatus,
            style = MaterialTheme.typography.body2,
            color = TextSecondary,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        // 심박수 카드
        Card(
            onClick = {
                val serviceIntent = Intent(context, HealthMonitoringService::class.java).apply {
                    action = HealthMonitoringService.ACTION_START_HR_MONITORING
                }
                context.startService(serviceIntent)
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
        ) {
            Column(modifier = Modifier.padding(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Filled.Favorite, contentDescription = "Heart Rate", tint = AccentRed, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "심박수: ${if (measurementState.isMeasuring) "측정 중..." else if (measurementState.lastAverageHr != null && measurementState.lastAverageHr > 0) "${measurementState.lastAverageHr} (평균)" else "---"} BPM", style = MaterialTheme.typography.body1, color = TextPrimary)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "(정상)", style = MaterialTheme.typography.body2, color = TextSecondary)
                }
                Text(text = "마지막 측정: $lastMeasured", style = MaterialTheme.typography.caption1, color = TextTertiary)
            }
        }

        // SOS 버튼
        Button(
            onClick = onSosClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            colors = ButtonDefaults.buttonColors(backgroundColor = AccentRed)
        ) {
            Text("응급전화", style = MaterialTheme.typography.title1, color = TextPrimary)
        }
    }
}
