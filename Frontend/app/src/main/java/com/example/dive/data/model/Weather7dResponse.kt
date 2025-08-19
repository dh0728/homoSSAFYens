package com.example.dive.data.model

data class Weather7dResponse(
    val status: String,
    val code: Int,
    val message: String,
    val data: Weather7dData
)

data class Weather7dData(
    val days: List<WeatherDay>
)

data class WeatherDay(
    val date: String,
    val hours: List<WeatherHour>
)

data class WeatherHour(
    val time: String,
    val sky: String,
    val skyCode: String,
    val rain: Int,
    val rainAmt: Double,
    val temp: Double,
    val winddir: String,
    val windspd: Double,
    val humidity: Double,
    val wavePrd: Double,
    val waveHt: Double,
    val waveDir: String
)
