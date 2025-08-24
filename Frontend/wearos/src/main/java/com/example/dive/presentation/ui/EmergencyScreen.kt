package com.example.dive.presentation.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Warning
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.wear.compose.material.*
import com.example.dive.emergency.EmergencyManager
import com.example.dive.health.HealthMonitoringService
import com.example.dive.presentation.EmergencyUiState
import com.example.dive.presentation.MainViewModel
import com.example.dive.presentation.MeasurementState
import com.example.dive.presentation.theme.AccentRed
import com.example.dive.presentation.theme.TextPrimary
import com.example.dive.presentation.theme.TextSecondary
import com.example.dive.presentation.theme.TextTertiary

@Composable
fun EmergencyScreen(viewModel: MainViewModel = viewModel()) {
    val context = LocalContext.current

    val permissions = arrayOf(Manifest.permission.SEND_SMS, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.CALL_PHONE)

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissionsMap ->
        if (permissionsMap.values.all { it }) {
            Log.d("EmergencyScreen", "All SOS permissions granted.")
            Toast.makeText(context, "권한이 허용되었습니다. 다시 시도해주세요.", Toast.LENGTH_SHORT).show()
        } else {
            Log.e("EmergencyScreen", "SOS permissions denied.")
            Toast.makeText(context, "SOS 기능에 필요한 권한이 거부되었습니다.", Toast.LENGTH_LONG).show()
        }
    }

    val onSosClick = {
        val hasTelephony = context.packageManager.hasSystemFeature(PackageManager.FEATURE_TELEPHONY)
        if (hasTelephony) {
            // If watch can send SMS, check permissions for it
            val permissionsGranted = permissions.all {
                ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
            }
            if (permissionsGranted) {
                EmergencyManager.triggerEmergencySOS(context, "수동 호출")
            } else {
                launcher.launch(permissions)
            }
        } else {
            // If watch cannot send SMS, just delegate to phone
            EmergencyManager.triggerEmergencySOS(context, "수동 호출")
        }
    }

    val uiState = viewModel.emergencyUiState.collectAsState().value
    val liveHr = viewModel.liveHeartRateStable.collectAsState().value
    val ms = viewModel.measurementState.collectAsState().value

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        when (uiState) {
            is EmergencyUiState.Loading -> CircularProgressIndicator()
            is EmergencyUiState.Error -> {
                Text(
                    text = "응급 정보를 가져올 수 없습니다.",
                    modifier = Modifier.padding(16.dp),
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colors.error
                )
            }
            is EmergencyUiState.Success -> {
                EmergencyInfoCard(
                    context = context,
                    lastMeasured = uiState.lastMeasured,
                    locationStatus = uiState.locationStatus,
                    onSosClick = onSosClick, // Use the new onSosClick lambda
                    measurementState = ms,
                    liveHeartRate = liveHr
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
    measurementState: MeasurementState,
    liveHeartRate: Int
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp, vertical = 0.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceAround
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .clip(RoundedCornerShape(12.dp)),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.Warning, contentDescription = "SOS", tint = AccentRed, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(8.dp))
            Text("SOS", style = MaterialTheme.typography.title1, color = AccentRed)
        }

        Card(
            onClick = {
                Log.d("EmergencyScreen", "HR card clicked")
                val serviceIntent = Intent(context, HealthMonitoringService::class.java).apply {
                    action = HealthMonitoringService.ACTION_START_HR_MONITORING
                }
                context.startService(serviceIntent)
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        ) {
            Column(Modifier.padding(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Favorite, contentDescription = "Heart Rate", tint = AccentRed, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))

                    val mainValue = when {
                        measurementState.isMeasuring && liveHeartRate > 0 -> "$liveHeartRate BPM"
                        measurementState.isMeasuring && liveHeartRate <= 0 -> "측정 중…"
                        (measurementState.lastAverageHr ?: 0) > 0 -> "${measurementState.lastAverageHr} BPM"
                        else -> "--- BPM"
                    }

                    Text(
                        text = "심박수: $mainValue",
                        style = MaterialTheme.typography.body1,
                        color = TextPrimary
                    )
                }

                Spacer(Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val lastMeasuredText = measurementState.lastMeasuredAt?.let {
                        java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date(it))
                    } ?: lastMeasured

                    Text(
                        text = "마지막 측정: $lastMeasuredText",
                        style = MaterialTheme.typography.caption1,
                        color = TextTertiary,
                        modifier = Modifier.weight(1f)
                    )

                    val suffix = when {
                        measurementState.isMeasuring && liveHeartRate > 0 -> "(실시간)"
                        (!measurementState.isMeasuring && (measurementState.lastAverageHr ?: 0) > 0) -> "(평균)"
                        measurementState.isMeasuring && liveHeartRate <= 0 -> "(측정 중)"
                        else -> ""
                    }

                    if (suffix.isNotEmpty()) {
                        Text(
                            text = suffix,
                            style = MaterialTheme.typography.caption1,
                            color = TextSecondary,
                            modifier = Modifier.wrapContentWidth(Alignment.End)
                        )
                    }
                }
            }
        }

        Button(
            onClick = onSosClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(55.dp),
            colors = ButtonDefaults.buttonColors(backgroundColor = AccentRed)
        ) {
            Text("응급전화", style = MaterialTheme.typography.title1, color = TextPrimary)
        }
    }
}