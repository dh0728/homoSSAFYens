package com.example.dive.service

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.telephony.SmsManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.dive.R
import com.example.dive.data.api.RetrofitClient
import com.example.dive.data.model.FishingResponse
import com.example.dive.data.model.LocationData
import com.example.dive.data.model.LocationResponse
import com.example.dive.data.model.TideResponse
import com.example.dive.data.model.Weather6hResponse
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Wearable
import com.google.android.gms.wearable.WearableListenerService
import com.google.gson.Gson
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import android.location.Location

class PhoneWearableService : WearableListenerService() {

    private val gson = Gson()
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    override fun onCreate() {
        super.onCreate() 
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
    }

    @SuppressLint("MissingPermission") // Permissions are handled in AndroidManifest.xml
    override fun onMessageReceived(messageEvent: MessageEvent) {
        Log.d("PhoneWearableService", "Received message: ${messageEvent.path}")

        val nodeId = messageEvent.sourceNodeId

        fusedLocationClient.lastLocation
            .addOnSuccessListener { location ->
                if (location != null) {
                    val lat = location.latitude
                    val lon = location.longitude

                    when (messageEvent.path) {
                        "/request/tide" -> fetchTideData(lat, lon, nodeId)
                        "/request/weather" -> fetchWeatherData(lat, lon, nodeId)
                        "/request/locations" -> fetchFishingPoints(lat, lon, nodeId)
                        "/request/current_location" -> fetchCurrentLocation(nodeId)
                        "/emergency/sos" -> {
                            val reason = String(messageEvent.data)
                            handleSosTrigger(reason)
                        }
                    }
                } else {
                    Log.e("PhoneWearableService", "Location not available for message: ${messageEvent.path}")
                    // Handle case where location is not available
                    when (messageEvent.path) {
                        "/request/tide" -> sendMessageToWatch("/response/tide/error", "Location not available".toByteArray(), nodeId)
                        "/request/weather" -> sendMessageToWatch("/response/weather/error", "Location not available".toByteArray(), nodeId)
                        "/request/locations" -> sendMessageToWatch("/response/locations/error", "Location not available".toByteArray(), nodeId)
                        "/request/current_location" -> sendMessageToWatch("/response/current_location/error", "Location not available".toByteArray(), nodeId)
                        "/emergency/sos" -> handleSosTrigger("Location not available for SOS")
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.e("PhoneWearableService", "Failed to get location for message: ${messageEvent.path}, Error: ${e.message}")
                // Handle case where location fetching failed
                when (messageEvent.path) {
                    "/request/tide" -> sendMessageToWatch("/response/tide/error", "Failed to get location".toByteArray(), nodeId)
                    "/request/weather" -> sendMessageToWatch("/response/weather/error", "Failed to get location".toByteArray(), nodeId)
                    "/request/locations" -> sendMessageToWatch("/response/locations/error", "Failed to get location".toByteArray(), nodeId)
                    "/request/current_location" -> sendMessageToWatch("/response/current_location/error", "Failed to get location".toByteArray(), nodeId)
                    "/emergency/sos" -> handleSosTrigger("Failed to get location for SOS")
                }
            }
    }

    private fun fetchTideData(lat: Double, lon: Double, nodeId: String) {
        RetrofitClient.instance.getTodayTide(lat, lon)
            .enqueue(object : Callback<TideResponse> {
                override fun onResponse(call: Call<TideResponse>, response: Response<TideResponse>) {
                    if (response.isSuccessful) {
                        val dataResponse = response.body()
                        val payload = gson.toJson(dataResponse).toByteArray()
                        sendMessageToWatch("/response/tide", payload, nodeId)
                    } else {
                        val errorMsg = "API Error: ${response.code()}"
                        sendMessageToWatch("/response/tide/error", errorMsg.toByteArray(), nodeId)
                    }
                }

                override fun onFailure(call: Call<TideResponse>, t: Throwable) {
                    sendMessageToWatch("/response/tide/error", t.message?.toByteArray() ?: byteArrayOf(), nodeId)
                }
            })
    }

    private fun fetchWeatherData(lat: Double, lon: Double, nodeId: String) {
        RetrofitClient.instance.getWeather6h(lat, lon)
            .enqueue(object : Callback<Weather6hResponse> {
                override fun onResponse(call: Call<Weather6hResponse>, response: Response<Weather6hResponse>) {
                    if (response.isSuccessful) {
                        val dataResponse = response.body()
                        val payload = gson.toJson(dataResponse).toByteArray()
                        sendMessageToWatch("/response/weather", payload, nodeId)
                    } else {
                        val errorMsg = "API Error: ${response.code()}"
                        sendMessageToWatch("/response/weather/error", errorMsg.toByteArray(), nodeId)
                    }
                }

                override fun onFailure(call: Call<Weather6hResponse>, t: Throwable) {
                    sendMessageToWatch("/response/weather/error", t.message?.toByteArray() ?: byteArrayOf(), nodeId)
                }
            })
    }

    private fun fetchFishingPoints(lat: Double, lon: Double, nodeId: String) {
        RetrofitClient.instance.getFishingPoints(lat, lon)
            .enqueue(object : Callback<FishingResponse> {
                override fun onResponse(call: Call<FishingResponse>, response: Response<FishingResponse>) {
                    if (response.isSuccessful) {
                        val dataResponse = response.body()
                        val payload = gson.toJson(dataResponse).toByteArray()
                        sendMessageToWatch("/response/locations", payload, nodeId)
                    } else {
                        val errorMsg = "API Error: ${response.code()}"
                        sendMessageToWatch("/response/locations/error", errorMsg.toByteArray(), nodeId)
                    }
                }

                override fun onFailure(call: Call<FishingResponse>, t: Throwable) {
                    sendMessageToWatch("/response/locations/error", t.message?.toByteArray() ?: byteArrayOf(), nodeId)
                }
            })
    }

    @SuppressLint("MissingPermission") // Permissions are handled in AndroidManifest.xml
    private fun fetchCurrentLocation(nodeId: String) {
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location ->
                if (location != null) {
                    val locationData = LocationData(
                        latitude = location.latitude,
                        longitude = location.longitude,
                        accuracy = location.accuracy,
                        timestamp = location.time,
                        address = "", // TODO: Implement reverse geocoding
                        nearestPoint = "" // TODO: Implement nearest point logic
                    )
                    val response = LocationResponse("success", 200, "Location fetched", locationData)
                    val payload = gson.toJson(response).toByteArray()
                    sendMessageToWatch("/response/current_location", payload, nodeId)
                } else {
                    val errorMsg = "Location not available"
                    sendMessageToWatch("/response/current_location/error", errorMsg.toByteArray(), nodeId)
                }
            }
            .addOnFailureListener { e ->
                val errorMsg = "Failed to get location: ${e.message}"
                sendMessageToWatch("/response/current_location/error", errorMsg.toByteArray(), nodeId)
            }
    }

    @SuppressLint("MissingPermission") // Permissions are handled in AndroidManifest.xml
    private fun handleSosTrigger(reason: String) {
        Log.e("PhoneWearableService", "!!! SOS TRIGGERED !!! Reason: $reason")

        val emergencyNumber = "01012345678" // TODO: Replace with actual emergency contact or 119

        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            val locationLink = if (location != null) {
                "https://www.google.com/maps/search/?api=1&query=${location.latitude},${location.longitude}"
            } else {
                "위치 정보 없음"
            }

            val smsManager = SmsManager.getDefault()
            val smsMessage = "긴급 SOS!\n사유: $reason\n위치: $locationLink"

            // Send SMS
            try {
                smsManager.sendTextMessage(emergencyNumber, null, smsMessage, null, null)
                Log.d("PhoneWearableService", "SOS SMS sent to $emergencyNumber")
            } catch (e: Exception) {
                Log.e("PhoneWearableService", "Failed to send SOS SMS: ${e.message}")
            }

            // Show Notification
            createSosNotification(reason, locationLink)

            // TODO: Implement actual call logic (requires CALL_PHONE permission and user confirmation)
            // val callIntent = Intent(Intent.ACTION_CALL)
            // callIntent.data = Uri.parse("tel:$emergencyNumber")
            // callIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            // startActivity(callIntent)

        }.addOnFailureListener { e ->
            Log.e("PhoneWearableService", "Failed to get location for SOS: ${e.message}")
            createSosNotification(reason, "위치 정보 없음")
        }
    }

    private fun createSosNotification(reason: String, locationLink: String) {
        val channelId = "sos_channel"
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create notification channel for Android O and above
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "SOS Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for emergency SOS alerts"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground) // TODO: Use a proper SOS icon
            .setContentTitle("긴급 SOS 발동!")
            .setContentText("사유: $reason | 위치: $locationLink")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(SOS_NOTIFICATION_ID, notification)
    }

    private fun sendMessageToWatch(path: String, data: ByteArray, nodeId: String) {
        Wearable.getMessageClient(this)
            .sendMessage(nodeId, path, data)
            .addOnSuccessListener { Log.d("PhoneWearableService", "Message sent to $nodeId path $path") }
            .addOnFailureListener { e -> Log.e("PhoneWearableService", "Failed to send message", e) }
    }

    companion object {
        private const val SOS_NOTIFICATION_ID = 1001
    }
}