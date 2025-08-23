package com.example.dive.data

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.dive.data.model.*
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.CapabilityInfo
import com.google.android.gms.wearable.Wearable
import com.google.gson.Gson
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class WatchDataRepository(private val context: Context) {

    private val messageClient by lazy { Wearable.getMessageClient(context) }
    private val capabilityClient by lazy { Wearable.getCapabilityClient(context) }
    private val gson = Gson()

    // 싱글톤 DataStore
    private val dataStore: DataStore<Preferences> = AppDataStores.preferences(context)

    // 연결 상태
    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    init {
        updateConnectionStatus()
        capabilityClient.addListener(
            { info: CapabilityInfo ->
                _isConnected.value = info.nodes.any { it.isNearby }
                Log.d(TAG, "Connection status changed: ${_isConnected.value}")
            },
            CAPABILITY_PHONE_APP
        )
    }

    private fun updateConnectionStatus() {
        GlobalScope.launch {
            try {
                val info = capabilityClient
                    .getCapability(CAPABILITY_PHONE_APP, CapabilityClient.FILTER_REACHABLE)
                    .await()
                _isConnected.value = info.nodes.any { it.isNearby }
                Log.d(TAG, "Initial connection: ${_isConnected.value}")
            } catch (e: Exception) {
                _isConnected.value = false
                Log.e(TAG, "Initial capability check failed", e)
            }
        }
    }

    private suspend fun capableNodeId(): String? =
        try {
            capabilityClient
                .getCapability(CAPABILITY_PHONE_APP, CapabilityClient.FILTER_REACHABLE)
                .await()
                .nodes
                .firstOrNull()
                ?.id
        } catch (e: Exception) {
            Log.e(TAG, "capableNodeId failed", e)
            null
        }

    private object Keys {
        val TIDE = stringPreferencesKey("tide_json")
        val W6H = stringPreferencesKey("weather6h_json")
        val W7D = stringPreferencesKey("weather7d_json")
        val FISH = stringPreferencesKey("fishing_json")
        val LOC  = stringPreferencesKey("location_json")
    }

    // Write APIs
    suspend fun saveTideJson(raw: String) { dataStore.edit { it[Keys.TIDE] = raw } }
    suspend fun saveWeather6hJson(raw: String) { dataStore.edit { it[Keys.W6H] = raw } }
    suspend fun saveWeather7dJson(raw: String) { dataStore.edit { it[Keys.W7D] = raw } }
    suspend fun saveFishingJson(raw: String) { dataStore.edit { it[Keys.FISH] = raw } }
    suspend fun saveLocationJson(raw: String) { dataStore.edit { it[Keys.LOC] = raw } }

    // Read Flows with parse-safety
    fun getTideData(): Flow<TideResponse> =
        dataStore.data.map { it[Keys.TIDE] }.filterNotNull().map { json ->
            runCatching { gson.fromJson(json, TideResponse::class.java) }
                .getOrElse { e ->
                    Log.e(TAG, "Parse tide failed: ${e.message}", e)
                    TideResponse(
                        status = "error", code = 0, message = "parse error",
                        data = TideData(
                            date = "", weekday = "", locationName = "", mul = "",
                            lunar = "", sunrise = "", sunset = "", moonrise = "", moonset = "",
                            events = emptyList()
                        )
                    )
                }.copy()
        }

    fun getWeatherData(): Flow<Weather6hResponse> =
        dataStore.data.map { it[Keys.W6H] }.filterNotNull().map { json ->
            runCatching { gson.fromJson(json, Weather6hResponse::class.java) }
                .getOrElse { e ->
                    Log.e(TAG, "Parse weather6h failed: ${e.message}", e)
                    Weather6hResponse(
                        status = "error", code = 0, message = "parse error",
                        data = Weather6hData(
                            weather = emptyList<Weather6hItem>(),
                            info = Weather6hInfo(city = "", cityCode = "")
                        )
                    )
                }.copy()
        }

    fun getWeather7dData(): Flow<Weather7dResponse> =
        dataStore.data.map { it[Keys.W7D] }.filterNotNull().map { json ->
            runCatching { gson.fromJson(json, Weather7dResponse::class.java) }
                .getOrElse { e ->
                    Log.e(TAG, "Parse weather7d failed: ${e.message}", e)
                    Weather7dResponse(
                        status = "error", code = 0, message = "parse error",
                        data = Weather7dData(days = emptyList())
                    )
                }.copy()
        }

    fun getFishingPoints(): Flow<FishingResponse> =
        dataStore.data.map { it[Keys.FISH] }.filterNotNull().map { json ->
            runCatching { gson.fromJson(json, FishingResponse::class.java) }
                .getOrElse { e ->
                    Log.e(TAG, "Parse fishing failed: ${e.message}", e)
                    FishingResponse(
                        status = "error", code = 0, message = "parse error",
                        data = FishingData(
                            info = FishingInfo(
                                intro = "", forecast = "", ebbf = "", notice = "",
                                waterTemps = emptyMap(), fishBySeason = emptyMap()
                            ),
                            points = emptyList()
                        )
                    )
                }.copy()
        }

    fun getLocationData(): Flow<LocationResponse> =
        dataStore.data.map { it[Keys.LOC] }.filterNotNull().map { json ->
            runCatching { gson.fromJson(json, LocationResponse::class.java) }
                .getOrElse { e ->
                    Log.e(TAG, "Parse location failed: ${e.message}", e)
                    LocationResponse(
                        status = "error", code = 0, message = "parse error",
                        data = LocationData(
                            latitude = 0.0, longitude = 0.0, accuracy = 0f, timestamp = 0L,
                            address = "위치 정보 없음", nearestPoint = ""
                        )
                    )
                }.copy()
        }

    // 워치 → 폰 요청
    suspend fun requestDataFromServer(path: String) {
        try {
            val nodeId = capableNodeId()
            if (nodeId == null) {
                Log.e(TAG, "No reachable phone node")
                return
            }
            messageClient.sendMessage(nodeId, path, null)
                .addOnSuccessListener { Log.d(TAG, "Request sent to $nodeId: $path") }
                .addOnFailureListener { e -> Log.e(TAG, "Request send failed", e) }
        } catch (e: Exception) {
            Log.e(TAG, "requestDataFromServer error", e)
        }
    }

    companion object {
        private const val TAG = "WatchDataRepository"
        private const val CAPABILITY_PHONE_APP = "homossafyens_phone_app"
    }
}