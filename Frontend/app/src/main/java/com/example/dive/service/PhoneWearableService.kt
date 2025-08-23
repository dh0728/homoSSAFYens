package com.example.dive.service

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.location.Location
import android.telephony.SmsManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.dive.R
import com.example.dive.data.api.RetrofitClient
import com.example.dive.data.model.*
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Wearable
import com.google.android.gms.wearable.WearableListenerService
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.IOException

class PhoneWearableService : WearableListenerService() {

    private val gson = Gson()
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val okHttpClient = OkHttpClient()
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate() 
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    // 캐시 유틸
    private object PhoneCache {
        private const val PREF = "phone_cache"
        private val gson = Gson()
        private fun sp(ctx: Context) = ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)

        fun saveJson(ctx: Context, key: String, any: Any) {
            sp(ctx).edit().putString(key, gson.toJson(any)).apply()
        }

        fun loadJson(ctx: Context, key: String): String? =
            sp(ctx).getString(key, null)
    }

    @SuppressLint("MissingPermission")
    override fun onMessageReceived(messageEvent: MessageEvent) {
        Log.d(TAG, "Received message: ${messageEvent.path}")
        val nodeId = messageEvent.sourceNodeId

        when (messageEvent.path) {
            "/request/tide" -> {
                PhoneCache.loadJson(this, KEY_TIDE)?.let {
                    sendMessageToWatch("/response/tide", it.toByteArray(), nodeId)
                }
                requestRefresh(nodeId)
            }
            "/request/weather" -> {
                PhoneCache.loadJson(this, KEY_W6H)?.let {
                    sendMessageToWatch("/response/weather", it.toByteArray(), nodeId)
                }
                requestRefresh(nodeId)
            }
            "/request/7dweather" -> {
                PhoneCache.loadJson(this, KEY_W7D)?.let {
                    sendMessageToWatch("/response/7dweather", it.toByteArray(), nodeId)
                }
                requestRefresh(nodeId)
            }
            "/request/locations" -> {
                PhoneCache.loadJson(this, KEY_FISH)?.let {
                    sendMessageToWatch("/response/locations", it.toByteArray(), nodeId)
                }
                requestRefresh(nodeId)
            }
            "/request/current_location" -> {
                PhoneCache.loadJson(this, KEY_LAST_LOC)?.let {
                    sendMessageToWatch("/response/current_location", it.toByteArray(), nodeId)
                }
                requestRefresh(nodeId)
            }
            "/request/refresh_all_data" -> {
                requestRefresh(nodeId)
            }
            "/request/map_for_point" -> {
                handleMapRequest(nodeId, messageEvent.data)
            }
            "/emergency/sos" -> {
                val reason = String(messageEvent.data)
                handleSosTrigger(reason)
            }
            "/request/tide7d" -> {
                PhoneCache.loadJson(this, KEY_TIDE7D)?.let {
                    sendMessageToWatch("/response/tide7d", it.toByteArray(), nodeId)
                }
                requestRefresh(nodeId)
            }

            else -> Unit
        }
    }

    private fun handleMapRequest(nodeId: String, data: ByteArray) {
        serviceScope.launch {
            try {
                val detail = gson.fromJson(data.decodeToString(), FishingPointDetail::class.java)
                val url = createStaticMapUrl(detail)
                Log.d(TAG, "Fetching map from URL: $url")

                val request = Request.Builder().url(url).build()
                okHttpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) throw IOException("Unexpected code $response")

                    val imageBytes = response.body?.bytes()
                    if (imageBytes != null) {
                        sendMessageToWatch("/response/map_image", imageBytes, nodeId)
                    } else {
                        Log.e(TAG, "Map image response body is null")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to handle map request", e)
            }
        }
    }

    private fun createStaticMapUrl(detail: FishingPointDetail): String {
        val point = detail.point
        val phoneLoc = detail.phoneLocation

        val baseUrl = "https://static-map.openstreetmap.de/staticmap.php"
        val zoom = "14"
        val size = "360x240"

        val pointMarker = "markers=${point.lat},${point.lon},red-pushpin"
        val phoneMarker = phoneLoc?.let {
            if (it.latitude != 0.0 && it.longitude != 0.0) {
                "|${it.latitude},${it.longitude},blue-dot"
            } else ""
        } ?: ""

        val center = "center=${point.lat},${point.lon}"

        return "$baseUrl?$center&zoom=$zoom&size=$size&$pointMarker$phoneMarker"
    }

    // 위치 확보: lastLocation → 실패 시 getCurrentLocation → 그래도 실패면 좌표 캐시 사용
    @SuppressLint("MissingPermission")
    private fun requestRefresh(nodeId: String) {
        fusedLocationClient.lastLocation
            .addOnSuccessListener { loc ->
                if (loc != null) {
                    refreshAllData(nodeId, loc)
                } else {
                    getCurrentLocationNow { cur -> refreshAllData(nodeId, cur) }
                }
            }
            .addOnFailureListener {
                getCurrentLocationNow { cur -> refreshAllData(nodeId, cur) }
            }
    }

    @SuppressLint("MissingPermission")
    private fun getCurrentLocationNow(onResult: (Location?) -> Unit) {
        val cts = com.google.android.gms.tasks.CancellationTokenSource()
        LocationServices.getFusedLocationProviderClient(this)
            .getCurrentLocation(
                com.google.android.gms.location.Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                cts.token
            )
            .addOnSuccessListener { onResult(it) }
            .addOnFailureListener { onResult(null) }

        // 3초 타임아웃 보조
        android.os.Handler(mainLooper).postDelayed({ onResult(null) }, 3000)
    }

    private fun resolveLatLon(loc: Location?): Pair<Double?, Double?> {
        if (loc != null) return loc.latitude to loc.longitude
        PhoneCache.loadJson(this, KEY_LAST_LOC)?.let { json ->
            return runCatching { gson.fromJson(json, LocationData::class.java) }
                .getOrNull()
                ?.let { it.latitude to it.longitude } ?: (null to null)
        }
        return null to null
    }

    private fun refreshAllData(nodeId: String, location: Location?) {
        val (lat, lon) = resolveLatLon(location)
        fetchTideData(lat, lon, nodeId)
        fetchTide7dData(lat, lon, nodeId)
        fetchWeatherData(lat, lon, nodeId)
        fetchWeather7dData(lat, lon, nodeId)
        fetchFishingPoints(lat, lon, nodeId)
        fetchCurrentLocation(nodeId, location)
    }

    private fun fetchTideData(lat: Double?, lon: Double?, nodeId: String) {
        if (lat == null || lon == null) {
            sendMessageToWatch("/response/tide/error", "Location not available".toByteArray(), nodeId)
            return
        }
        RetrofitClient.instance.getTodayTide(lat, lon)
            .enqueue(object : Callback<TideResponse> {
                override fun onResponse(call: Call<TideResponse>, response: Response<TideResponse>) {
                    if (response.isSuccessful) {
                        val body = response.body()
                        val payload = gson.toJson(body).toByteArray()
                        sendMessageToWatch("/response/tide", payload, nodeId)
                        body?.let { PhoneCache.saveJson(this@PhoneWearableService, KEY_TIDE, it) } // 캐시 저장
                    } else {
                        sendMessageToWatch("/response/tide/error", "API Error: ${response.code()}".toByteArray(), nodeId)
                    }
                }
                override fun onFailure(call: Call<TideResponse>, t: Throwable) {
                    sendMessageToWatch("/response/tide/error", (t.message ?: "error").toByteArray(), nodeId)
                }
            })
    }

    private fun fetchWeatherData(lat: Double?, lon: Double?, nodeId: String) {
        if (lat == null || lon == null) {
            sendMessageToWatch("/response/weather/error", "Location not available".toByteArray(), nodeId)
            return
        }
        RetrofitClient.instance.getWeather6h(lat, lon)
            .enqueue(object : Callback<Weather6hResponse> {
                override fun onResponse(call: Call<Weather6hResponse>, response: Response<Weather6hResponse>) {
                    if (response.isSuccessful) {
                        val body = response.body()
                        val payload = gson.toJson(body).toByteArray()
                        sendMessageToWatch("/response/weather", payload, nodeId)
                        body?.let { PhoneCache.saveJson(this@PhoneWearableService, KEY_W6H, it) } // 캐시 저장
                    } else {
                        sendMessageToWatch("/response/weather/error", "API Error: ${response.code()}".toByteArray(), nodeId)
                    }
                }
                override fun onFailure(call: Call<Weather6hResponse>, t: Throwable) {
                    sendMessageToWatch("/response/weather/error", (t.message ?: "error").toByteArray(), nodeId)
                }
            })
    }

    private fun fetchWeather7dData(lat: Double?, lon: Double?, nodeId: String) {
        if (lat == null || lon == null) {
            sendMessageToWatch("/response/7dweather/error", "Location not available".toByteArray(), nodeId)
            return
        }
        RetrofitClient.instance.getWeather7d(lat, lon)
            .enqueue(object : Callback<Weather7dResponse> {
                override fun onResponse(call: Call<Weather7dResponse>, response: Response<Weather7dResponse>) {
                    if (response.isSuccessful) {
                        val body = response.body()
                        val payload = gson.toJson(body).toByteArray()
                        sendMessageToWatch("/response/7dweather", payload, nodeId)
                        body?.let { PhoneCache.saveJson(this@PhoneWearableService, KEY_W7D, it) } // 캐시 저장
                    } else {
                        sendMessageToWatch("/response/7dweather/error", "API Error: ${response.code()}".toByteArray(), nodeId)
                    }
                }
                override fun onFailure(call: Call<Weather7dResponse>, t: Throwable) {
                    sendMessageToWatch("/response/7dweather/error", (t.message ?: "error").toByteArray(), nodeId)
                }
            })
    }

    private fun fetchFishingPoints(lat: Double?, lon: Double?, nodeId: String) {
        if (lat == null || lon == null) {
            sendMessageToWatch("/response/locations/error", "Location not available".toByteArray(), nodeId)
            return
        }
        RetrofitClient.instance.getFishingPoints(lat, lon)
            .enqueue(object : Callback<FishingResponse> {
                override fun onResponse(call: Call<FishingResponse>, response: Response<FishingResponse>) {
                    if (response.isSuccessful) {
                        val body = response.body()
                        val payload = gson.toJson(body).toByteArray()
                        sendMessageToWatch("/response/locations", payload, nodeId)
                        body?.let { PhoneCache.saveJson(this@PhoneWearableService, KEY_FISH, it) } // 캐시 저장
                    } else {
                        sendMessageToWatch("/response/locations/error", "API Error: ${response.code()}".toByteArray(), nodeId)
                    }
                }
                override fun onFailure(call: Call<FishingResponse>, t: Throwable) {
                    sendMessageToWatch("/response/locations/error", (t.message ?: "error").toByteArray(), nodeId)
                }
            })
    }

    @SuppressLint("MissingPermission")
    private fun fetchCurrentLocation(nodeId: String, location: Location?) {
        if (location != null) {
            val data = LocationData(
                latitude = location.latitude,
                longitude = location.longitude,
                accuracy = location.accuracy,
                timestamp = location.time,
                address = "",
                nearestPoint = ""
            )
            val resp = LocationResponse("success", 200, "Location fetched", data)
            PhoneCache.saveJson(this, KEY_LAST_LOC, data) // 좌표 캐시 갱신
            sendMessageToWatch("/response/current_location", gson.toJson(resp).toByteArray(), nodeId)
        } else {
            fusedLocationClient.lastLocation
                .addOnSuccessListener { last ->
                    if (last != null) {
                        val data = LocationData(
                            latitude = last.latitude,
                            longitude = last.longitude,
                            accuracy = last.accuracy,
                            timestamp = last.time,
                            address = "",
                            nearestPoint = ""
                        )
                        val resp = LocationResponse("success", 200, "Location fetched", data)
                        PhoneCache.saveJson(this, KEY_LAST_LOC, data) // 좌표 캐시 갱신
                        sendMessageToWatch("/response/current_location", gson.toJson(resp).toByteArray(), nodeId)
                    } else {
                        sendMessageToWatch("/response/current_location/error", "Location not available".toByteArray(), nodeId)
                    }
                }
                .addOnFailureListener { e ->
                    sendMessageToWatch("/response/current_location/error", "Failed to get location: ${e.message}".toByteArray(), nodeId)
                }
        }
    }

    // SOS 처리(기존 로직)
    @SuppressLint("MissingPermission")
    private fun handleSosTrigger(reason: String) {
        Log.e(TAG, "!!! SOS TRIGGERED !!! Reason: $reason")
        val emergencyNumber = "01012345678" // TODO 실제 번호
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location ->
                val link = location?.let {
                    "https://www.google.com/maps/search/?api=1&query=${it.latitude},${it.longitude}"
                } ?: "위치 정보 없음"
                val sms = SmsManager.getDefault()
                val msg = "긴급 SOS!\n사유: $reason\n위치: $link"
                try {
                    sms.sendTextMessage(emergencyNumber, null, msg, null, null)
                    Log.d(TAG, "SOS SMS sent")
                } catch (e: Exception) {
                    Log.e(TAG, "SOS SMS failed: ${e.message}")
                }
                createSosNotification(reason, link)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "SOS location failed: ${e.message}")
                createSosNotification(reason, "위치 정보 없음")
            }
    }

    private fun createSosNotification(reason: String, locationLink: String) {
        val channelId = "sos_channel"
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val ch = NotificationChannel(
                channelId,
                "SOS Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply { description = "Notifications for emergency SOS alerts" }
            nm.createNotificationChannel(ch)
        }

        val n = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("긴급 SOS 발동!")
            .setContentText("사유: $reason | 위치: $locationLink")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        nm.notify(SOS_NOTIFICATION_ID, n)
    }

    private fun sendMessageToWatch(path: String, data: ByteArray, nodeId: String) {
        Wearable.getMessageClient(this)
            .sendMessage(nodeId, path, data)
            .addOnSuccessListener { Log.d(TAG, "Message sent to $nodeId path $path") }
            .addOnFailureListener { e -> Log.e(TAG, "Failed to send message", e) }
    }

    companion object {
        private const val TAG = "PhoneWearableService"
        private const val SOS_NOTIFICATION_ID = 1001

        // 캐시 키
        private const val KEY_LAST_LOC = "last_loc_json"
        private const val KEY_TIDE = "last_tide_json"
        private const val KEY_W6H = "last_w6h_json"
        private const val KEY_W7D = "last_w7d_json"
        private const val KEY_FISH = "last_fish_json"
        private const val KEY_TIDE7D = "last_tide7d_json"

    }

    private fun fetchTide7dData(lat: Double?, lon: Double?, nodeId: String) {
        Log.d(TAG, "🌊 fetchTide7dData 요청 lat=$lat lon=$lon nodeId=$nodeId")
        if (lat == null || lon == null) {
            sendMessageToWatch("/response/tide7d/error", "Location not available".toByteArray(), nodeId)
            return
        }
        RetrofitClient.instance.getWeeklyTide(lat, lon)   // ✅ 여기서 getWeeklyTide 사용
            .enqueue(object : Callback<TideWeeklyResponse> {
                override fun onResponse(call: Call<TideWeeklyResponse>, response: Response<TideWeeklyResponse>) {
                    if (response.isSuccessful) {
                        val body = response.body()
                        val payload = gson.toJson(body).toByteArray()
                        Log.d(TAG, "🌊 fetchTide7dData 성공 ${body?.data?.size}일치")
                        sendMessageToWatch("/response/tide7d", payload, nodeId)
                        body?.let { PhoneCache.saveJson(this@PhoneWearableService, KEY_TIDE7D, it) }
                    } else {
                        sendMessageToWatch("/response/tide7d/error", "API Error: ${response.code()}".toByteArray(), nodeId)
                    }
                }
                override fun onFailure(call: Call<TideWeeklyResponse>, t: Throwable) {
                    Log.e(TAG, "🌊 fetchTide7dData 실패: ${t.message}")
                    sendMessageToWatch("/response/tide7d/error", (t.message ?: "error").toByteArray(), nodeId)
                }
            })
    }



}