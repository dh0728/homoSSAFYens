package com.example.dive.presentation.ui

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.*
import com.example.dive.data.model.FishingData
import com.example.dive.presentation.FishingPointsUiState
import com.example.dive.presentation.theme.TextPrimary
import com.example.dive.presentation.theme.TextSecondary
import com.example.dive.presentation.theme.TextTertiary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun FishingPointsScreen(uiState: FishingPointsUiState) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        when (uiState) {
            is FishingPointsUiState.Loading -> {
                CircularProgressIndicator()
            }
            is FishingPointsUiState.Error -> {
                Text(
                    text = "포인트 정보를 가져올 수 없습니다.",
                    modifier = Modifier.padding(16.dp),
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colors.error
                )
            }
            is FishingPointsUiState.Success -> {
                LocationInfoCard(fishingData = uiState.fishingData)
            }
        }
    }
}

@Composable
fun LocationInfoCard(fishingData: FishingData) {
    val nearestPoint = fishingData.points.firstOrNull()
    val lastUpdate = SimpleDateFormat("m분 전", Locale.getDefault()).format(Date())

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 위치 상태
        Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "📱 폰 위치 기반",
                style = MaterialTheme.typography.body2,
                color = TextSecondary
            )
            Text(
                text = "${nearestPoint?.addr ?: "위치 알 수 없음"} - ${nearestPoint?.pointName ?: ""}",
                style = MaterialTheme.typography.body1,
                color = TextPrimary,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 좌표/업데이트
        Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "좌표: ${nearestPoint?.lat ?: "-"}, ${nearestPoint?.lon ?: "-"}",
                style = MaterialTheme.typography.caption1,
                color = TextTertiary
            )
            Text(
                text = "마지막 업데이트: $lastUpdate",
                style = MaterialTheme.typography.caption1,
                color = TextTertiary
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 액션 버튼
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(onClick = { /* TODO: 새로고침 */ }) { Text("새로고침") }
            Button(onClick = { /* TODO: 저장 */ }) { Text("저장") }
            Button(onClick = { /* TODO: 상세 */ }) { Text("상세") }
        }
    }
}
