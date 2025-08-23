package com.example.dive.sensors.casting

data class CastingResult(
    val speed: Double,
    val releaseTiming: Long,   // timingError -> releaseTiming으로 통일
    val stability: Double,
    val smoothness: Double,
    val score: Int,
    val coaching: String
)
