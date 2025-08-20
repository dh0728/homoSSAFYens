package com.example.dive.data

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.example.dive.data.model.FishingResponse
import com.example.dive.data.model.LocationResponse
import com.example.dive.data.model.TideResponse
import com.example.dive.data.model.Weather6hResponse
import com.example.dive.data.model.Weather7dResponse
import com.example.dive.service.WatchMessageListenerService
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.CapabilityInfo
import com.google.android.gms.wearable.Wearable
import com.google.gson.Gson
import kotlinx.coroutines.channels.awaitClose
//import kotlinx.coroutines.channels.trySend
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class WatchDataRepository(private val context: Context) {

    private val messageClient by lazy { Wearable.getMessageClient(context) }
    private val capabilityClient by lazy { Wearable.getCapabilityClient(context) }
    private val gson = Gson()

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    init {
        // Initial check for connection status
        updateConnectionStatus()

        // Listen for changes in capability (phone app connection)
        capabilityClient.addListener(
            { capabilityInfo: CapabilityInfo ->
                _isConnected.value = capabilityInfo.nodes.any { it.isNearby }
                Log.d("WatchDataRepository", "Connection status changed: ${_isConnected.value}")
            },
            CAPABILITY_PHONE_APP
        )
    }

    private fun updateConnectionStatus() {
        // Launch a coroutine to get the current status
        // This is a suspend function, so it needs to be called from a coroutine
        // For simplicity, we'll use a global scope here, but in a real app,
        // you'd manage this lifecycle more carefully.
        // However, since this is a repository, it might be okay.
        // TODO: Consider proper coroutine scope management if this causes issues.
        kotlinx.coroutines.GlobalScope.launch {
            try {
                val capabilityInfo = capabilityClient.getCapability(CAPABILITY_PHONE_APP, CapabilityClient.FILTER_REACHABLE).await()
                _isConnected.value = capabilityInfo.nodes.any { it.isNearby }
                Log.d("WatchDataRepository", "Initial connection status: ${_isConnected.value}")
            } catch (e: Exception) {
                Log.e("WatchDataRepository", "Failed to get initial capability info", e)
                _isConnected.value = false
            }
        }
    }

    fun getTideData(): Flow<TideResponse> = callbackFlow {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.action == WatchMessageListenerService.ACTION_MESSAGE_RECEIVED) {
                    val path = intent.getStringExtra(WatchMessageListenerService.EXTRA_PATH)
                    if (path == "/response/tide") {
                        val data = intent.getByteArrayExtra(WatchMessageListenerService.EXTRA_DATA)
                        if (data != null) {
                            val response = gson.fromJson(String(data), TideResponse::class.java)
                            this@callbackFlow.trySend(response) // Explicitly use this@callbackFlow
                        }
                    } else if (path == "/response/tide/error") {
                        this@callbackFlow.close() // Explicitly use this@callbackFlow
                    }
                }
            }
        }
        LocalBroadcastManager.getInstance(context).registerReceiver(receiver, IntentFilter(WatchMessageListenerService.ACTION_MESSAGE_RECEIVED))
        requestDataFromServer("/request/tide")
        awaitClose { LocalBroadcastManager.getInstance(context).unregisterReceiver(receiver) }
    }

    fun getWeatherData(): Flow<Weather6hResponse> = callbackFlow {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.action == WatchMessageListenerService.ACTION_MESSAGE_RECEIVED) {
                    val path = intent.getStringExtra(WatchMessageListenerService.EXTRA_PATH)
                    if (path == "/response/weather") {
                        val data = intent.getByteArrayExtra(WatchMessageListenerService.EXTRA_DATA)
                        if (data != null) {
                            val response = gson.fromJson(String(data), Weather6hResponse::class.java)
                            this@callbackFlow.trySend(response)
                        }
                    } else if (path == "/response/weather/error") {
                        this@callbackFlow.close()
                    }
                }
            }
        }
        LocalBroadcastManager.getInstance(context).registerReceiver(receiver, IntentFilter(WatchMessageListenerService.ACTION_MESSAGE_RECEIVED))
        requestDataFromServer("/request/weather")
        awaitClose { LocalBroadcastManager.getInstance(context).unregisterReceiver(receiver) }
    }

    fun getFishingPoints(): Flow<FishingResponse> = callbackFlow {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.action == WatchMessageListenerService.ACTION_MESSAGE_RECEIVED) {
                    val path = intent.getStringExtra(WatchMessageListenerService.EXTRA_PATH)
                    if (path == "/response/locations") {
                        val data = intent.getByteArrayExtra(WatchMessageListenerService.EXTRA_DATA)
                        if (data != null) {
                            val response = gson.fromJson(String(data), FishingResponse::class.java)
                            this@callbackFlow.trySend(response)
                        }
                    } else if (path == "/response/locations/error") {
                        this@callbackFlow.close()
                    }
                }
            }
        }
        LocalBroadcastManager.getInstance(context).registerReceiver(receiver, IntentFilter(WatchMessageListenerService.ACTION_MESSAGE_RECEIVED))
        requestDataFromServer("/request/locations")
        awaitClose { LocalBroadcastManager.getInstance(context).unregisterReceiver(receiver) }
    }

    fun getWeather7dData(): Flow<Weather7dResponse> = callbackFlow {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.action == WatchMessageListenerService.ACTION_MESSAGE_RECEIVED) {
                    val path = intent.getStringExtra(WatchMessageListenerService.EXTRA_PATH)
                    if (path == "/response/7dweather") {
                        val data = intent.getByteArrayExtra(WatchMessageListenerService.EXTRA_DATA)
                        if (data != null) {
                            val response = gson.fromJson(String(data), Weather7dResponse::class.java)
                            this@callbackFlow.trySend(response)
                        }
                    } else if (path == "/response/7dweather/error") {
                        this@callbackFlow.close()
                    }
                }
            }
        }
        LocalBroadcastManager.getInstance(context).registerReceiver(receiver, IntentFilter(WatchMessageListenerService.ACTION_MESSAGE_RECEIVED))
        requestDataFromServer("/request/7dweather")
        awaitClose { LocalBroadcastManager.getInstance(context).unregisterReceiver(receiver) }
    }

    fun getLocationData(): Flow<LocationResponse> = callbackFlow {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.action == WatchMessageListenerService.ACTION_MESSAGE_RECEIVED) {
                    val path = intent.getStringExtra(WatchMessageListenerService.EXTRA_PATH)
                    if (path == "/response/current_location") {
                        val data = intent.getByteArrayExtra(WatchMessageListenerService.EXTRA_DATA)
                        if (data != null) {
                            val response = gson.fromJson(String(data), LocationResponse::class.java)
                            this@callbackFlow.trySend(response)
                        }
                    } else if (path == "/response/current_location/error") {
                        this@callbackFlow.close()
                    }
                }
            }
        }
        LocalBroadcastManager.getInstance(context).registerReceiver(receiver, IntentFilter(WatchMessageListenerService.ACTION_MESSAGE_RECEIVED))
        requestDataFromServer("/request/current_location")
        awaitClose { LocalBroadcastManager.getInstance(context).unregisterReceiver(receiver) }
    }

    private suspend fun requestDataFromServer(path: String) {
        try {
            val nodes = capabilityClient.getCapability(CAPABILITY_PHONE_APP, CapabilityClient.FILTER_REACHABLE).await().nodes
            nodes.firstOrNull()?.id?.let { nodeId -> // Changed 'it' to 'nodeId' for clarity
                messageClient.sendMessage(nodeId, path, null)
                    .addOnSuccessListener { Log.d("WatchDataRepository", "Request message sent to $nodeId") } // Changed 'it' to 'nodeId'
                    .addOnFailureListener { e -> Log.e("WatchDataRepository", "Failed to send request message", e) }
            } ?: Log.e("WatchDataRepository", "No reachable phone node found")
        } catch (e: Exception) {
            Log.e("WatchDataRepository", "Failed to get capable nodes", e)
        }
    }

    companion object {
        private const val CAPABILITY_PHONE_APP = "homossafyens_phone_app"
    }
}