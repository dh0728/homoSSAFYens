package com.example.dive.data.model

data class FishingResponse(
    val status: String,
    val code: Int,
    val message: String,
    val data: FishingData
)

data class FishingData(
    val info: FishingInfo,
    val points: List<FishingPoint>
)

data class FishingInfo(
    val intro: String,
    val forecast: String,
    val ebbf: String,
    val notice: String,
    val waterTemps: Map<String, SeasonTemp>,
    val fishBySeason: Map<String, List<String>>
)

data class SeasonTemp(
    val surface: Double,
    val bottom: Double
)

data class FishingPoint(
    val name: String,
    val pointName: String,
    val depth: Depth,
    val material: String,
    val tideTime: String,
    val targetByFish: Map<String, List<String>>,
    val lat: Double,
    val lon: Double,
    val photo: String,
    val addr: String,
    val seaside: Boolean,
    val pointDtKm: Double
)

data class Depth(
    val minM: Double,
    val maxM: Double
)
