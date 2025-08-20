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
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import com.example.dive.data.model.Weather7dData
import com.example.dive.presentation.DetailedWeatherUiState
import com.example.dive.presentation.theme.BackgroundSecondary
import com.example.dive.presentation.theme.TextPrimary
import com.example.dive.presentation.theme.TextSecondary
import com.example.dive.presentation.theme.TextTertiary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DetailedWeatherScreen(uiState: DetailedWeatherUiState) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        when (uiState) {
            is DetailedWeatherUiState.Loading -> {
                CircularProgressIndicator()
            }
            is DetailedWeatherUiState.Error -> {
                Text(
                    text = "상세 날씨 정보를 가져올 수 없습니다.",
                    modifier = Modifier.padding(16.dp),
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colors.error
                )
            }
            is DetailedWeatherUiState.Success -> {
                DetailedWeatherInfoCard(weatherData = uiState.weatherData)
            }
        }
    }
}

@Composable
fun DetailedWeatherInfoCard(weatherData: Weather7dData) {
    val currentDateTime = SimpleDateFormat("HH시 mm분", Locale.getDefault()).format(Date())
    val todayWeather = weatherData.days.firstOrNull()?.hours?.firstOrNull()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 상단 바 (간단)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(BackgroundSecondary)
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val date = SimpleDateFormat("MM.dd", Locale.getDefault()).format(Date())
            val weekday = SimpleDateFormat("E", Locale.getDefault()).format(Date())
            Text(
                text = "$date ($weekday)",
                style = MaterialTheme.typography.body2,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )
        }

        Text(
            text = currentDateTime,
            style = MaterialTheme.typography.title2,
            color = TextPrimary,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (todayWeather != null) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                DetailRow("날씨", todayWeather.sky, "풍향", todayWeather.winddir)
                DetailRow("기온", "${todayWeather.temp}°", "풍속", "${todayWeather.windspd}m/s")
                DetailRow("강수량", "${todayWeather.rainAmt}mm", "파고", "${todayWeather.waveHt ?: "-"}m")
                DetailRow("습도", "${todayWeather.humidity}%", "", "")
            }
        } else {
            Text(text = "상세 날씨 정보 없음", color = TextSecondary)
        }
    }
}

@Composable
fun DetailRow(label1: String, value1: String, label2: String, value2: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(horizontalAlignment = Alignment.Start) {
            Text(text = label1, style = MaterialTheme.typography.caption1, color = TextTertiary)
            Text(text = value1, style = MaterialTheme.typography.body2, color = TextPrimary)
        }
        if (label2.isNotEmpty()) {
            Column(horizontalAlignment = Alignment.Start) {
                Text(text = label2, style = MaterialTheme.typography.caption1, color = TextTertiary)
                Text(text = value2, style = MaterialTheme.typography.body2, color = TextPrimary)
            }
        }
    }
    Spacer(modifier = Modifier.height(4.dp))
}
