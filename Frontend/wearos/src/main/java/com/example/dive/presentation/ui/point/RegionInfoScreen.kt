package com.example.dive.presentation.ui.point

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.*
import com.example.dive.data.model.FishingInfo
import com.example.dive.presentation.MainViewModel
import com.example.dive.presentation.theme.TextPrimary
import com.example.dive.presentation.theme.TextSecondary
import java.util.Calendar

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RegionInfoScreen(
    info: FishingInfo,
    viewModel: MainViewModel,
    pagerState: PagerState
) {
    val listState = rememberScalingLazyListState()
    LaunchedEffect(Unit) {
        listState.scrollToItem(0)
    }

    SwipeToDismissBox(
        onDismissed = { viewModel.returnToList() }
    ) { isBackground ->
        if (isBackground) {
            Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colors.background))
        } else {
            ScalingLazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 24.dp, start = 16.dp, end = 16.dp, bottom = 40.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 지역 정보 화면
                item {
                    Text(text = info.intro.substringBefore(" ").trim(), style = MaterialTheme.typography.title1, color = TextPrimary, textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.height(20.dp))
                }

                // 현재 계절 정보
                item {
                    val currentSeason = remember { getCurrentSeason() }
                    val seasonEmoji = when(currentSeason) {
                        "SPRING" -> "🌸"
                        "SUMMER" -> "☀️"
                        "FALL" -> "🍂"
                        "WINTER" -> "❄️"
                        else -> ""
                    }
                    val seasonName = when(currentSeason) {
                        "SPRING" -> "봄"
                        "SUMMER" -> "여름"
                        "FALL" -> "가을"
                        "WINTER" -> "겨울"
                        else -> ""
                    }

                    val fishForSeason = info.fishBySeason[currentSeason]?.joinToString(", ") ?: "정보 없음"
                    val tempForSeason = info.waterTemps[currentSeason]
                    Column (verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp)
                    ) {
                        Text("$seasonEmoji $seasonName 철 어종", fontWeight = FontWeight.Bold, color = TextPrimary, textAlign = TextAlign.Center, modifier = Modifier.padding(vertical = 4.dp))
                        Text(fishForSeason, style = MaterialTheme.typography.body2, textAlign = TextAlign.Center, color = TextSecondary)
                        Spacer(modifier = Modifier.height(8.dp))

                        if (tempForSeason != null) {
                            Text("🌡️ 수온", fontWeight = FontWeight.Bold, color = TextPrimary, textAlign = TextAlign.Center)
                            Text("표층 ${tempForSeason.surface}°C, 저층 ${tempForSeason.bottom}°C", style = MaterialTheme.typography.body2, textAlign = TextAlign.Center, color = TextSecondary)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }

                // 지역 특징
                item {
                    Column (verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp)
                    ) {
                        Text(
                            "ℹ️ 지역 특징",
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            info.intro,
                            style = MaterialTheme.typography.body2,
                            color = TextSecondary,
                            textAlign = TextAlign.Left
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }

                // 주의사항
                item {
                    Column (verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp)
                    ) {
                        Text(
                            "⚠️ 주의사항",
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            info.notice,
                            style = MaterialTheme.typography.body2,
                            color = TextSecondary,
                            textAlign = TextAlign.Left
                        )
                    }
                }
            }
        }
    }
}

private fun getCurrentSeason(): String {
    return when (Calendar.getInstance().get(Calendar.MONTH) + 1) {
        in 3..5 -> "SPRING"
        in 6..8 -> "SUMMER"
        in 9..11 -> "FALL"
        else -> "WINTER"
    }
}