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
    syncHint: SyncHint = SyncHint.NONE // 추가: 상단 안내 배지 노출용 (옵션)
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        when (uiState) {
            is TideUiState.Loading -> {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // 안내 배지(옵션)
                    if (syncHint == SyncHint.PROMPT) {
                        SyncPromptBadge(
                            text = "휴대폰에서 앱을 열어 동기화하세요"
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    CircularProgressIndicator()
                }
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
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // 성공 상태에서도 아직 다른 데이터 동기화 중이면 배지 표시 가능
                    if (syncHint == SyncHint.PROMPT) {
                        SyncPromptBadge(
                            text = "휴대폰에서 앱을 열어 동기화하세요"
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                    TideInfoCard(tideData = uiState.tideData)
                }
            }
        }
    }
}

@Composable
private fun SyncPromptBadge(text: String) {
    Card(onClick = { /* no-op */ }, modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
        Box(
            modifier = Modifier.padding(vertical = 6.dp, horizontal = 8.dp),
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

        // 2 x 2 배치
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 0.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                TideEventCell(
                    event = tideData.events.getOrNull(0) ?: TideEvent("--:--", 0, "만조", 0),
                    modifier = Modifier.weight(0.45f)
                )
                TideEventCell(
                    event = tideData.events.getOrNull(1) ?: TideEvent("--:--", 0, "간조", 0),
                    modifier = Modifier.weight(0.45f)
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                TideEventCell(
                    event = tideData.events.getOrNull(2) ?: TideEvent("--:--", 0, "만조", 0),
                    modifier = Modifier.weight(0.45f)
                )
                TideEventCell(
                    event = tideData.events.getOrNull(3) ?: TideEvent("--:--", 0, "간조", 0),
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
            Spacer(Modifier.width(4.dp))
            Icon(
                imageVector = arrowIcon,
                contentDescription = trendLabel,
                tint = arrowColor,
                modifier = Modifier.size(14.dp)
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
