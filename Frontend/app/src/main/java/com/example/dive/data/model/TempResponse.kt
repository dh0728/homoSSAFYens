package com.example.dive.data.model

data class TempResponse(
    val status: String,
    val code: Int,
    val message: String,
    val data: List<TempData>
)

data class TempData(
    val lat: Double,
    val lon: Double,
    val obsName: String,   // 관측소 이름
    val obsWt: Double,     // 수온 (°C)
    val obsTime: String,   // 관측 시각
    val obsDt: Double      // 수심 (m)
)
