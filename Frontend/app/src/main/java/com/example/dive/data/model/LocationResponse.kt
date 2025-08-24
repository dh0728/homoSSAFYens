package com.example.dive.data.model

data class LocationResponse(
    val status: String,
    val code: Int,
    val message: String,
    val data: LocationData
)

data class LocationData(
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float,
    val timestamp: Long,
    val address: String? = null,
    val nearestPoint: String? = null
)
