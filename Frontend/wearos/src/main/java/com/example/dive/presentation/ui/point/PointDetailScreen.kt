package com.example.dive.presentation.ui.point

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.*
import com.example.dive.data.model.FishingPoint
import com.example.dive.presentation.MainViewModel
import com.example.dive.presentation.theme.TextPrimary
import com.example.dive.presentation.theme.TextSecondary

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PointDetailScreen(
    point: FishingPoint,
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
                // 포인트 상세 정보
                item {
                    Column (verticalArrangement = Arrangement.spacedBy(4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Text(
                            text = point.pointName,
                            style = MaterialTheme.typography.title1,
                            color = TextPrimary,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = point.name,
                            style = MaterialTheme.typography.caption1,
                            color = TextSecondary,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = point.addr,
                            style = MaterialTheme.typography.caption2,
                            color = TextSecondary,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                }

                // 상세 스펙 (가운데 정렬 3줄)
                item {
                    Column(horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)) {
                        Text(
                            text = "🌊 ${point.depth.minM}~${point.depth.maxM}m 💧 ${point.material}",
                            style = MaterialTheme.typography.body2,
                            color = TextPrimary,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "🕐 ${point.tideTime}",
                            style = MaterialTheme.typography.body2,
                            color = TextPrimary,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "📍 ${point.pointDtKm}km 거리",
                            style = MaterialTheme.typography.body2,
                            color = TextPrimary,
                            textAlign = TextAlign.Center
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                // 어종 및 낚시법
                item {
                    Column(horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 20.dp, bottom = 6.dp)) {
                        Text("어종 및 낚시법", fontWeight = FontWeight.Bold, color = TextPrimary)
                    }
                }
                items(point.targetByFish.toList()) { (fish, methods) ->
                    Text("🎣 $fish: ${methods.joinToString(", ")}", style = MaterialTheme.typography.body2, color = TextSecondary)
                    Spacer(modifier = Modifier.height(4.dp)) // 각 어종별 간격
                }

                item { Spacer(modifier = Modifier.height(16.dp)) }

                // 액션 버튼
                item {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp, horizontal = 15.dp)
                    ) {
                        Button(
                            onClick = { viewModel.showMap(point, pagerState.currentPage) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(percent = 50)
                        ) {
                            Text("지도보기")
                        }
                    }
                }
            }
        }
    }
}
