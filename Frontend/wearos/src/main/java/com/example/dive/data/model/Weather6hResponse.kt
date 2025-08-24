package com.example.dive.data.model

data class Weather6hResponse(
    val status: String,
    val code: Int,
    val message: String,
    val data: Weather6hData
)

data class Weather6hData(
    val weather: List<Weather6hItem>,
    val info: Weather6hInfo
)

data class Weather6hInfo(
    val city: String,
    val cityCode: String
)

data class Weather6hItem(
    val sky: String,
    val rainMm: Double,
    val tempC: Double,
    val pm25S: String,
    val pm10S: String,
    val pm10: Int,
    val windDir: String,
    val waveHeightM: String?,
    val skyCode: String,
    val windSpeedMs: Double,
    val pm25: Int,
    val time: String,
    val humidityPct: Double
)
