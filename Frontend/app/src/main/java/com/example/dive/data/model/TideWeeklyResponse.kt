package com.example.dive.data.model

data class TideWeeklyResponse(
    val status: String,
    val code: Int,
    val message: String,
    val data: List<TideData>
)
