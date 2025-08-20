package com.example.dive.presentation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.CircularProgressIndicator
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import com.example.dive.data.model.Weather6hData
import com.example.dive.presentation.WeatherUiState
import com.example.dive.presentation.theme.AccentRed
import com.example.dive.presentation.theme.BackgroundSecondary
import com.example.dive.presentation.theme.TextPrimary
import com.example.dive.presentation.theme.TextSecondary
import com.example.dive.presentation.theme.TextTertiary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning

@Composable
fun WeatherScreen(uiState: WeatherUiState) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        when (uiState) {
            is WeatherUiState.Loading -> {
                CircularProgressIndicator()
            }
            is WeatherUiState.Error -> {
                Text(
                    text = "날씨 정보를 가져올 수 없습니다.",
                    modifier = Modifier.padding(16.dp),
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colors.error
                )
            }
            is WeatherUiState.Success -> {
                WeatherInfoCard(weatherData = uiState.weatherData)
            }
        }
    }
}

@Composable
fun WeatherInfoCard(weatherData: Weather6hData) {
    val currentWeather = weatherData.weather.firstOrNull()
    val currentTime = SimpleDateFormat("HH시", Locale.getDefault()).format(Date())

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 상단 바
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
                text = weatherData.info.city,
                style = MaterialTheme.typography.body2,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )
        }

        // 시간
        Text(
            text = "현재 $currentTime",
            style = MaterialTheme.typography.caption1,
            color = TextTertiary,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (currentWeather != null) {
            // 메인 날씨
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "🌤️",
                    style = MaterialTheme.typography.display1,
                    modifier = Modifier.padding(end = 4.dp)
                )
                Text(
                    text = "${currentWeather.tempC}°",
                    style = MaterialTheme.typography.display1,
                    color = TextPrimary
                )
            }

            Text(
                text = currentWeather.sky,
                style = MaterialTheme.typography.body2,
                color = TextSecondary,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 상세
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "💨", modifier = Modifier.padding(end = 8.dp))
                    Text(
                        text = "${currentWeather.windDir} ${currentWeather.windSpeedMs}m/s",
                        style = MaterialTheme.typography.body2,
                        color = TextSecondary
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "🌊", modifier = Modifier.padding(end = 8.dp))
                    Text(
                        text = "파고 ${currentWeather.waveHeightM ?: "-"}m",
                        style = MaterialTheme.typography.body2,
                        color = TextSecondary
                    )
                }

                // 알림(예시)
                val hasAlert = true
                if (hasAlert) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Warning,
                            contentDescription = "Warning",
                            tint = AccentRed,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "폭풍 발효중",
                            style = MaterialTheme.typography.body2,
                            color = AccentRed
                        )
                    }
                }
            }
        } else {
            Text(text = "현재 날씨 정보 없음", color = TextSecondary)
        }
    }
}
