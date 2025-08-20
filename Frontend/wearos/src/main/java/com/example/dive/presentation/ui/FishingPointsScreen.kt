package com.example.dive.presentation.ui

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.*
import com.example.dive.data.model.FishingData
import com.example.dive.data.model.FishingPoint
import com.example.dive.presentation.FishingPointsUiState
import com.example.dive.presentation.theme.TextPrimary
import com.example.dive.presentation.theme.TextSecondary

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
                FishingPointsList(fishingData = uiState.fishingData)
            }
        }
    }
}

@Composable
fun FishingPointsList(fishingData: FishingData) {
    ScalingLazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            top = 24.dp,
            start = 8.dp,
            end = 8.dp,
            bottom = 40.dp
        ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header
        item {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = fishingData.info.intro.substringBefore(" ").trim(), // e.g., "수영만"
                    style = MaterialTheme.typography.title3,
                    color = TextPrimary
                )
                Text(
                    text = "총 ${fishingData.points.size}개 포인트",
                    style = MaterialTheme.typography.body2,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
        }

        // Point Cards
        items(fishingData.points) { point ->
            FishingPointCard(point = point)
        }
    }
}

@Composable
fun FishingPointCard(point: FishingPoint) {
    Card(
        onClick = { /* TODO: Navigate to detail screen */ },
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // Point Name and Distance
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = point.pointName,
                    style = MaterialTheme.typography.title3,
                    color = TextPrimary,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${point.pointDtKm}km",
                    style = MaterialTheme.typography.body1,
                    color = TextSecondary
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Details
            Text(
                text = "${point.depth.minM}~${point.depth.maxM}m · ${point.material} · ${point.tideTime}",
                style = MaterialTheme.typography.body2,
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Target Fish
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