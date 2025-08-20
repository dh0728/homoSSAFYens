package com.example.dive.presentation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.CircularProgressIndicator
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import com.example.dive.data.model.TideData
import com.example.dive.data.model.TideEvent
import com.example.dive.presentation.TideUiState
import com.example.dive.presentation.WeatherUiState
import com.example.dive.presentation.theme.*

@Composable
fun TideScreen(uiState: TideUiState) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        when (uiState) {
            is TideUiState.Loading -> {
                CircularProgressIndicator()
            }
            is TideUiState.Error -> {
                Text(
                    text = "물때 정보를 가져올 수 없습니다.",
                    modifier = Modifier.padding(16.dp),
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colors.error
                )
            }
            is TideUiState.Success -> {
                TideInfoCard(tideData = uiState.tideData)
            }

            WeatherUiState.Loading -> TODO()
        }
    }
}

@Composable
fun TideInfoCard(tideData: TideData) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // 상단 바 - 둥근 회색 배경
        TopBar(date = tideData.date, weekday = tideData.weekday)

        // 메인 타이틀 - 좌측 정렬
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = tideData.locationName,
                style = MaterialTheme.typography.title1,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.width(6.dp))
            // 노란 원형 아이콘
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .clip(CircleShape)
                    .background(AccentYellow)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = tideData.mul, // "3물"
                style = MaterialTheme.typography.title1,
                color = AccentYellow
            )
        }

        // 4분할 그리드 레이아웃 (2열)
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            userScrollEnabled = true
        ) {
            items(tideData.events) { event ->
                TideEventCell(event = event)
            }
        }
    }
}

@Composable
fun TopBar(date: String, weekday: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(BackgroundSecondary)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$date ($weekday)", // "07.22 (화)"
            style = MaterialTheme.typography.body2,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun TideEventCell(event: TideEvent) {
    val backgroundColor = when (event.trend) {
        "만조" -> AccentRed.copy(alpha = 0.3f)
        "간조" -> AccentBlue.copy(alpha = 0.3f)
        else -> BackgroundSecondary.copy(alpha = 0.3f)
    }

    val arrowColor = when {
        event.deltaCm == null -> TextSecondary
        event.deltaCm >= 0 -> AccentRed
        else -> AccentBlue
    }

    val arrowIcon = when {
        event.deltaCm == null -> null
        event.deltaCm >= 0 -> Icons.Filled.ArrowUpward
        else -> Icons.Filled.ArrowDownward
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // 상단 배지(만조/간조)
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(
                    when (event.trend) {
                        "만조" -> AccentRed
                        "간조" -> AccentBlue
                        else -> TextTertiary
                    }
                )
                .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            Text(
                text = event.trend, // "만조" or "간조"
                style = MaterialTheme.typography.caption1,
                color = TextPrimary
            )
        }

        Spacer(Modifier.height(6.dp))

        // 시간
        Text(
            text = event.time, // "00:06"
            style = MaterialTheme.typography.body1,
            color = TextPrimary
        )

        // 수치 (레벨)
        Text(
            text = "(${event.levelCm})",
            style = MaterialTheme.typography.body2,
            color = TextSecondary
        )

        // 변화량 (아이콘 + 숫자)
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (arrowIcon != null) {
                Icon(
                    imageVector = arrowIcon,
                    contentDescription = event.trend,
                    tint = arrowColor,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(Modifier.width(2.dp))
            }
            val deltaText = when (val d = event.deltaCm) {
                null -> ""
                else -> if (d > 0) "+$d" else "$d"
            }
            Text(
                text = deltaText,
                style = MaterialTheme.typography.caption1,
                color = arrowColor
            )
        }
    }
}
