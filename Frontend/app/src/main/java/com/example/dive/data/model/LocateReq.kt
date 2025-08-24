package com.example.dive.data.model

data class LocateReq(
    val deviceId: String,
    val lat: Double,
    val lon: Double,
    val ts: Long
)
