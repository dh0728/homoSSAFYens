package com.example.dive.presentation.ui

import Tide7day
import TideWeeklyResponse
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.wear.compose.material.Card
import androidx.wear.compose.material.CircularProgressIndicator
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import com.example.dive.data.model.TideData
import com.example.dive.data.model.TideEvent
import com.example.dive.presentation.SyncHint
import com.example.dive.presentation.TideUiState
import com.example.dive.presentation.theme.*

@Composable
fun TideScreen(
    uiState: TideUiState,
    tideWeekly: TideWeeklyResponse? = null,
    syncHint: SyncHint = SyncHint.NONE
) {
    when (uiState) {
        is TideUiState.Loading -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                if (syncHint == SyncHint.PROMPT) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        SyncPromptBadge("휴대폰에서 앱을 열어 동기화하세요")
                        Spacer(Modifier.height(8.dp))
                        CircularProgressIndicator()
                    }
                } else {
                    CircularProgressIndicator()
                }
            }
        }

        is TideUiState.Error -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "물때 정보를 가져올 수 없습니다.",
                    modifier = Modifier.padding(16.dp),
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colors.error
                )
            }
        }

        is TideUiState.Success -> {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp),
                contentPadding = PaddingValues(
                    top = 4.dp,
                    bottom = 24.dp
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 오늘 물때
                item {
                    TideInfoCard(tideData = uiState.tideData)
                }

                tideWeekly?.let { response ->
                    Log.d("TideScreen", "주간 물때 UI 진입: ${response.data.size}일치")
                    val weeklyData = response.data.drop(1)
                    items(weeklyData.size) { index ->
                        Log.d("TideScreen", "렌더링 index=$index -> ${weeklyData[index].date}")
                        TideInfoCard(tideData = weeklyData[index].toTideData())
                    }
                }

            }
        }
    }
}

@Composable
fun TideInfoCard(tideData: Tide7day) {
    TideInfoCard(tideData.toTideData())
}



fun Tide7day.toTideData(): TideData {
    return TideData(
        date = this.date,
        weekday = this.weekday,
        lunar = this.lunar,
        locationName = this.locationName,
        mul = this.mul,
        sunrise = this.sunrise,
        sunset = this.sunset,
        moonrise = this.moonrise ?: "",
        moonset = this.moonset ?: "",
        events = this.events.map {
            TideEvent(
                time = it.time,
                levelCm = it.levelCm,
                trend = it.trend,
                deltaCm = it.deltaCm
            )
        }
    )
}



@Composable
private fun SyncPromptBadge(text: String) {
    Card(onClick = { /* no-op */ }, modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
        Box(
            modifier = Modifier.padding(vertical = 7.dp, horizontal = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.caption1,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun TideInfoCard(tideData: TideData) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 7.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Spacer(modifier = Modifier.height(1.dp))

        // 상단 바
        TopBar(date = tideData.date, weekday = tideData.weekday)

        // 메인 타이틀
        Row(
            modifier = Modifier.padding(top = 2.dp), //.fillMaxWidth(),
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

        // 2 x 2 배치
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp)
            ) {
                TideEventCell(
                    event = tideData.events.getOrNull(0) ?: TideEvent("--:--", 0, "만조", 0),
                    modifier = Modifier.weight(0.48f)
                )
                TideEventCell(
                    event = tideData.events.getOrNull(1) ?: TideEvent("--:--", 0, "간조", 0),
                    modifier = Modifier.weight(0.48f)
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 9.dp)
            ) {
                TideEventCell(
                    event = tideData.events.getOrNull(2) ?: TideEvent("--:--", 0, "만조", 0),
                    modifier = Modifier.weight(0.48f)
                )
                TideEventCell(
                    event = tideData.events.getOrNull(3) ?: TideEvent("--:--", 0, "간조", 0),
                    modifier = Modifier.weight(0.48f)
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
            .padding(horizontal = 8.dp, vertical = 2.dp),
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

    val backgroundColor = BackgroundSecondary.copy(alpha = 0.8f)
    val arrowColor = when {
        (event.deltaCm ?: 0) >= 0 -> AccentRed
        else -> AccentBlue
    }
    val arrowIcon = when {
        (event.deltaCm ?: 0) >= 0 -> Icons.Filled.ArrowUpward
        else -> Icons.Filled.ArrowDownward
    }

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .padding(horizontal = 3.dp, vertical = 3.dp),
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
                .padding(horizontal = 3.dp, vertical = 1.dp)
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
            text = (event.time.takeIf { it.length >= 5 } ?: "--:--").substring(0, 5),
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
            Spacer(Modifier.width(3.dp))
            Icon(
                imageVector = arrowIcon,
                contentDescription = trendLabel,
                tint = arrowColor,
                modifier = Modifier.size(10.dp)
            )
            Spacer(Modifier.width(2.dp))
            val d = event.deltaCm ?: 0
            val deltaText = if (d > 0) "+$d" else "$d"
            Text(
                text = deltaText,
                style = MaterialTheme.typography.caption1,
                color = arrowColor
            )
        }
    }
}
