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
import androidx.wear.compose.material.ScalingLazyColumn
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
        item {
            Text(
                text = "주간 날씨",
                style = MaterialTheme.typography.title2,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        weatherData.days.forEach { day ->
            item {
                DayWeatherCard(day = day)
            }
        }
    }
}

@Composable
fun DayWeatherCard(day: com.example.dive.data.model.WeatherDay) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(BackgroundSecondary)
            .padding(12.dp)
    ) {
        // Date Header
        val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.KOREA)
        val outputFormat = SimpleDateFormat("MM월 dd일 (E)", Locale.KOREA)
        val date = inputFormat.parse(day.date)
        Text(
            text = outputFormat.format(date),
            style = MaterialTheme.typography.title3,
            color = TextPrimary,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            textAlign = TextAlign.Center
        )

        // Table Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = "시간", style = MaterialTheme.typography.caption1, color = TextTertiary, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
            Text(text = "날씨", style = MaterialTheme.typography.caption1, color = TextTertiary, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
            Text(text = "기온", style = MaterialTheme.typography.caption1, color = TextTertiary, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
            Text(text = "강수", style = MaterialTheme.typography.caption1, color = TextTertiary, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
        }

        // Table Rows
        day.hours.forEach { hour ->
            HourlyWeatherRow(hour = hour)
        }
    }
}

@Composable
fun HourlyWeatherRow(hour: com.example.dive.data.model.WeatherHour) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.KOREA)
        val date = inputFormat.parse(hour.time)
        val hourString = SimpleDateFormat("HH시", Locale.KOREA).format(date)

        Text(text = hourString, style = MaterialTheme.typography.body2, color = TextPrimary, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
        Text(text = getWeatherEmojiFromSky(hour.sky), style = MaterialTheme.typography.body2, color = TextPrimary, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
        Text(text = "${hour.temp}°", style = MaterialTheme.typography.body2, color = TextPrimary, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
        Text(text = "${hour.rainAmt}mm", style = MaterialTheme.typography.body2, color = TextPrimary, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
    }
}
