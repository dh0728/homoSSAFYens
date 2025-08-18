package com.example.dive.data.model

data class TideResponse(
    val status: String,
    val code: Int,
    val message: String,
    val data: TideData
)

data class TideData(
    val date: String,
    val weekday: String,
    val lunar: String,
    val locationName: String,
    val mul: String,
    val sunrise: String,
    val sunset: String,
    val moonrise: String,
    val moonset: String,
    val events: List<TideEvent>
)

data class TideEvent(
    val time: String,
    val levelCm: Int,
    val trend: String,
    val deltaCm: Int
)
