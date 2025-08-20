package com.example.dive.presentation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.wear.compose.material.ScalingLazyColumn
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
//    val currentTime = SimpleDateFormat("HH시", Locale.KOREA).format(Date())

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            top = 25.dp,
            start = 8.dp,
            end = 8.dp,
            bottom = 40.dp
        ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Location Header
        item {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(BackgroundSecondary)
                    .padding(horizontal = 12.dp, vertical = 5.dp), // 🔥 Text 주변만 배경
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = weatherData.info.city,
                    style = MaterialTheme.typography.body2,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )
            }
        }


        item { Spacer(modifier = Modifier.height(6.dp)) }

//        // Current Time
//        item {
//            Text(
//                text = "현재 $currentTime",
//                style = MaterialTheme.typography.caption1,
//                color = TextTertiary
//            )
//        }

        if (currentWeather != null) {
            // Main Weather Display
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = getWeatherEmojiFromSky(currentWeather.sky),
                        style = MaterialTheme.typography.display2,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text(
                        text = "${currentWeather.tempC}°",
                        style = MaterialTheme.typography.display1,
                        color = TextPrimary
                    )
                }
            }
            item {
                Text(
                    text = currentWeather.sky,
                    style = MaterialTheme.typography.body1,
                    color = TextSecondary,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
            }

// ✅ 상세 데이터 (Row + Column으로 정리)
            item {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                ) {
                    // 1행: 강수량 / 습도 / 파고
                    Row(
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("강수량", style = MaterialTheme.typography.caption1, color = TextSecondary)
                            Text("${currentWeather.rainMm.toInt()}mm", style = MaterialTheme.typography.body2, color = TextPrimary)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("습도", style = MaterialTheme.typography.caption1, color = TextSecondary)
                            Text("${currentWeather.humidityPct.toInt()}%", style = MaterialTheme.typography.body2, color = TextPrimary)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("파고", style = MaterialTheme.typography.caption1, color = TextSecondary)
                            Text("${currentWeather.waveHeightM ?: "-"}m", style = MaterialTheme.typography.body2, color = TextPrimary)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // 2행: 미세먼지 / 바람
                    Row(
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("미세먼지", style = MaterialTheme.typography.caption1, color = TextSecondary)
                            Text(currentWeather.pm10S, style = MaterialTheme.typography.body2, color = TextPrimary)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("바람", style = MaterialTheme.typography.caption1, color = TextSecondary)
                            Text("${currentWeather.windDir} ${currentWeather.windSpeedMs}m/s", style = MaterialTheme.typography.body2, color = TextPrimary)
                        }
                    }
                }
            }



            // 6-Hour Forecast Table
            item { Spacer(modifier = Modifier.height(16.dp)) }
//            item {
//                Text(text = "6시간 예보", style = MaterialTheme.typography.title3, color = TextPrimary)
//            }
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(BackgroundSecondary)
                        .padding(8.dp)
                ) {
                    // Table Header
                    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text("시간", style = MaterialTheme.typography.caption2, color = TextTertiary, modifier = Modifier.width(50.dp), textAlign = TextAlign.Center)
                        Text("날씨", style = MaterialTheme.typography.caption2, color = TextTertiary, modifier = Modifier.width(35.dp), textAlign = TextAlign.Center)
                        Text("기온", style = MaterialTheme.typography.caption2, color = TextTertiary, modifier = Modifier.width(35.dp), textAlign = TextAlign.Center)
                        Text("강수", style = MaterialTheme.typography.caption2, color = TextTertiary, modifier = Modifier.width(50.dp), textAlign = TextAlign.Center)
                    }
                    // Table Rows
                    weatherData.weather.forEach { hourlyWeather ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 2.dp),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.KOREA)
                            val date = inputFormat.parse(hourlyWeather.time)
                            val hourString = SimpleDateFormat("a h시", Locale.KOREA).format(date)

                            Text(hourString, style = MaterialTheme.typography.body2, color = TextPrimary, modifier = Modifier.width(50.dp), textAlign = TextAlign.Center)
                            Text(getWeatherEmojiFromSky(hourlyWeather.sky), style = MaterialTheme.typography.body2, color = TextPrimary, modifier = Modifier.width(35.dp), textAlign = TextAlign.Center)
                            Text("${hourlyWeather.tempC.toInt()}°", style = MaterialTheme.typography.body2, color = TextPrimary, modifier = Modifier.width(35.dp), textAlign = TextAlign.Center)
                            Text("${hourlyWeather.rainMm.toInt()}mm", style = MaterialTheme.typography.body2, color = TextPrimary, modifier = Modifier.width(50.dp), textAlign = TextAlign.Center)
                        }
                    }
                }
            }

        } else {
            item {
                Text(text = "현재 날씨 정보 없음", color = TextSecondary)
            }
        }
    }
}

fun getWeatherEmojiFromSky(sky: String): String {
    return when {
        sky.contains("맑음") -> "☀️"
        sky.contains("구름많음") -> "☁️"
        sky.contains("구름조금") -> "🌤️"
        sky.contains("흐림") -> "☁️"
        sky.contains("비/눈") -> "🌧️"
        sky.contains("비") -> "🌧️"
        sky.contains("눈") -> "🌨️"
        else -> "❔"
    }
}