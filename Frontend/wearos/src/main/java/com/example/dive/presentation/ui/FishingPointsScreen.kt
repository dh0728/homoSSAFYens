package com.example.dive.presentation.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.CircularProgressIndicator
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import com.example.dive.presentation.FishingPointsUiMode
import com.example.dive.presentation.FishingPointsUiState
import com.example.dive.presentation.MainViewModel
import com.example.dive.presentation.ui.point.MapViewScreen
import com.example.dive.presentation.ui.point.PointDetailScreen
import com.example.dive.presentation.ui.point.PointListScreen
import com.example.dive.presentation.ui.point.RegionInfoScreen

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FishingPointsScreen(uiState: FishingPointsUiState, viewModel: MainViewModel, pagerState: PagerState) {
    val fishingPointsUiMode by viewModel.fishingPointsUiMode.collectAsState()

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        when (val state = uiState) {
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
                // 낚시 포인트 탭의 내부 내비게이션
                when (val mode = fishingPointsUiMode) {
                    is FishingPointsUiMode.List -> {
                        PointListScreen(fishingData = state.fishingData, viewModel = viewModel, pagerState = pagerState)
                    }
                    is FishingPointsUiMode.PointDetail -> {
                        PointDetailScreen(point = mode.point, viewModel = viewModel, pagerState = pagerState)
                    }
                    is FishingPointsUiMode.RegionInfo -> {
                        RegionInfoScreen(info = mode.info, viewModel = viewModel, pagerState = pagerState)
                    }
                    is FishingPointsUiMode.MapView -> {
                        MapViewScreen(mapState = mode, viewModel = viewModel)
                    }
                }
            }
        }
    }
}