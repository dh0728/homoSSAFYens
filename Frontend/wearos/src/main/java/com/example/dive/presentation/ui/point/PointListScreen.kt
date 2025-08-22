package com.example.dive.presentation.ui.point

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.PagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.*
import com.example.dive.data.model.FishingData
import com.example.dive.data.model.FishingPoint
import com.example.dive.presentation.MainViewModel
import com.example.dive.presentation.theme.TextPrimary
import com.example.dive.presentation.theme.TextSecondary

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PointListScreen(fishingData: FishingData, viewModel: MainViewModel, pagerState: PagerState) {
    val listState = rememberScalingLazyListState()
    LaunchedEffect(Unit) {
        listState.scrollToItem(0)
    }
    ScalingLazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            top = 24.dp,
            start = 8.dp,
            end = 8.dp,
            bottom = 40.dp
        ),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Header
        item {
            Chip(
                onClick = { viewModel.showRegionInfo(pagerState.currentPage) },
                modifier = Modifier.padding(vertical = 8.dp),
                label = { Text(fishingData.info.intro.substringBefore(" ").trim()) }, // 지역명
                secondaryLabel = { Text("총 ${fishingData.points.size}개 포인트") },
                icon = { Icon(Icons.Default.Info, contentDescription = "지역 정보 보기") },
                colors = ChipDefaults.secondaryChipColors()
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Point Cards
        items(fishingData.points) { point ->
            FishingPointCard(point = point, onClick = { viewModel.showPointDetail(point, pagerState.currentPage) })
        }
    }
}

@Composable
fun FishingPointCard(point: FishingPoint, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${point.pointName} (${point.pointDtKm}km)",
                    style = MaterialTheme.typography.title3,
                    color = TextPrimary,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "${point.depth.minM}~${point.depth.maxM}m · ${point.material} · ${point.tideTime}",
                style = MaterialTheme.typography.body2,
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(6.dp))

            val targetFish = point.targetByFish.keys.take(3).joinToString(", ")
            Text(
                text = "🐟 $targetFish",
                style = MaterialTheme.typography.body1,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
