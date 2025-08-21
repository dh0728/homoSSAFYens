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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*

@Composable
fun EmergencyScreen(uiState: EmergencyUiState) {
    val context = LocalContext.current

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        when (uiState) {
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
                    heartRate = uiState.heartRate,
                    lastMeasured = uiState.lastMeasured,
                    locationStatus = uiState.locationStatus,
                    onSosClick = { EmergencyManager.triggerEmergencySOS(context, "수동 호출") }
                )
            }
        }
    }
}

@Composable
fun EmergencyInfoCard(
    heartRate: Int,
    lastMeasured: String,
    locationStatus: String,
    onSosClick: () -> Unit
) {
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
            onClick = { /* TODO: 상세 심박수 이력 */ },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
        ) {
            Column(modifier = Modifier.padding(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Filled.Favorite, contentDescription = "Heart Rate", tint = AccentRed, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "심박수: $heartRate BPM", style = MaterialTheme.typography.body1, color = TextPrimary)
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
