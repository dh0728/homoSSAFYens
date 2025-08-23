package com.example.dive.service

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataItem
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import com.google.android.gms.wearable.WearableListenerService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class EmergencyNumberDataService : WearableListenerService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        super.onDataChanged(dataEvents)
        Log.d(TAG, "onDataChanged: ${dataEvents.count} events")

        dataEvents.forEach { event ->
            if (event.type == DataEvent.TYPE_CHANGED) {
                val dataItem = event.dataItem
                if (dataItem.uri.path?.startsWith(REQUEST_PATH) == true) {
                    val requestPath = dataItem.uri.path!! // Extract path before launching coroutine
                    Log.d(TAG, "Received request for emergency number: $requestPath")
                    serviceScope.launch {
                        sendEmergencyNumberResponse(requestPath)
                    }
                }
            }
        }
    }

    private suspend fun sendEmergencyNumberResponse(requestPath: String) {
        val prefs = getSharedPreferences("emergency_settings", Context.MODE_PRIVATE)
        val emergencyNumber = prefs.getString("emergency_number", null)

        val requestId = requestPath.substringAfterLast("/")
        val responsePath = "$RESPONSE_PATH/$requestId"

        val putDataMapReq = PutDataMapRequest.create(responsePath).apply {
            dataMap.putString(KEY_EMERGENCY_NUMBER, emergencyNumber ?: "")
        }

        try {
            Wearable.getDataClient(this).putDataItem(putDataMapReq.asPutDataRequest().setUrgent()).await()
            Log.d(TAG, "Sent emergency number response: $emergencyNumber for request $requestId")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send emergency number response", e)
        }
    }

    companion object {
        private const val TAG = "EmergencyNumDataService"
        private const val REQUEST_PATH = "/request_emergency_number"
        private const val RESPONSE_PATH = "/response_emergency_number"
        private const val KEY_EMERGENCY_NUMBER = "emergency_number"
    }

    private fun initiateEmergencyCall() {
        val prefs = getSharedPreferences("emergency_settings", Context.MODE_PRIVATE)
        val emergencyNumber = prefs.getString("emergency_number", null)

        if (emergencyNumber.isNullOrEmpty()) {
            Log.e(TAG, "Emergency number not set. Cannot initiate call.")
            return
        }

        val callIntent = Intent(Intent.ACTION_CALL).apply {
            data = Uri.parse("tel:$emergencyNumber")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startActivity(callIntent)
        Log.d(TAG, "Initiated emergency call to: $emergencyNumber")
    }
}