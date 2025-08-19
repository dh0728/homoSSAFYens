// FineDustResponse.kt
data class FineDustResponse(
    val status: String,
    val code: Int,
    val message: String,
    val data: FineDustData
)

data class FineDustData(
    val time: String,
    val stationName: String,
    val data: AirQualityData
)

data class AirQualityData(
    val pm10: AirItem,
    val pm25: AirItem,
    val o3: AirItem,
    val co: AirItem,
    val so2: AirItem,
    val no2: AirItem,
    val khai: AirItem
)

data class AirItem(
    val value: Double,
    val level: String,
    val unit: String
)
