
package com.example.dive.presentation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
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
            .padding(horizontal = 4.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Spacer(modifier = Modifier.height(1.dp))

        // 상단 바
        TopBar(date = tideData.date, weekday = tideData.weekday)

        // 메인 타이틀
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = tideData.locationName,
                style = MaterialTheme.typography.title2,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.width(6.dp))
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .clip(CircleShape)
                    .background(AccentYellow)
            )
            Spacer(modifier = Modifier.width(1.dp))
            Text(
                text = tideData.mul,
                style = MaterialTheme.typography.title2,
                color = AccentYellow
            )
        }

        Spacer(modifier = Modifier.height(1.dp))

        // 2 x 2 배치 (Row + Column)
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 0.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                TideEventCell(
                    event = tideData.events[0],
                    modifier = Modifier.weight(0.45f)
                )
                TideEventCell(
                    event = tideData.events[1],
                    modifier = Modifier.weight(0.45f)
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                TideEventCell(
                    event = tideData.events[2],
                    modifier = Modifier.weight(0.45f)
                )
                TideEventCell(
                    event = tideData.events[3],
                    modifier = Modifier.weight(0.45f)
                )
            }
        }
    }
}

@Composable
fun TopBar(date: String, weekday: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .padding(horizontal = 10.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$date ($weekday)",
            style = MaterialTheme.typography.body2,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun TideEventCell(event: TideEvent, modifier: Modifier = Modifier) {
    val trendLabel = when (event.trend.uppercase()) {
        "RISING" -> "만조"
        "FALLING" -> "간조"
        else -> event.trend
    }

    // 🔹 배경은 회색 그대로 유지
    val backgroundColor = BackgroundSecondary.copy(alpha = 0.8f)

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
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .padding(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // 상단 배지
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(
                    when (trendLabel) {
                        "만조" -> AccentRed
                        "간조" -> AccentBlue
                        else -> TextTertiary
                    }
                )
                .padding(horizontal = 4.dp, vertical = 2.dp)
        ) {
            Text(
                text = trendLabel,
                style = MaterialTheme.typography.caption1.copy(fontWeight = FontWeight.SemiBold),
                color = TextPrimary
            )
        }

        Spacer(Modifier.height(2.dp))

        // 시간
        Text(
            text = event.time,
            style = MaterialTheme.typography.title3,
            color = TextPrimary
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = "(${event.levelCm})",
                style = MaterialTheme.typography.caption1,
                color = TextSecondary
            )

            Spacer(Modifier.width(4.dp))

            if (arrowIcon != null) {
                Icon(
                    imageVector = arrowIcon,
                    contentDescription = trendLabel,
                    tint = arrowColor,
                    modifier = Modifier.size(14.dp)
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
