// Tide7dayResponse.kt
data class TideWeeklyResponse(
    val status: String,
    val code: Int,
    val message: String,
    val data: List<Tide7day>
)

data class Tide7day(
    val date: String,
    val weekday: String,
    val lunar: String,
    val locationName: String,
    val mul: String,
    val sunrise: String,
    val sunset: String,
    val moonrise: String?,
    val moonset: String?,
    val events: List<Tide7dayEvent>
)

data class Tide7dayEvent(
    val time: String,
    val levelCm: Int,
    val trend: String,
    val deltaCm: Int
)
