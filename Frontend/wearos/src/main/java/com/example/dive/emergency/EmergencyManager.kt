package com.example.dive.emergency

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.telephony.SmsManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import java.util.UUID
import com.example.dive.Constants

object EmergencyManager {

    private const val TAG = "EmergencyManager"
    private const val REQUEST_PATH = "/request_emergency_number"
    private const val RESPONSE_PATH = "/response_emergency_number"
    private const val KEY_EMERGENCY_NUMBER = "emergency_number"

    // Use a map to hold deferreds for multiple concurrent requests if needed
    // For a single request at a time, a single deferred is fine.
    private val emergencyNumberResponseDeferreds = mutableMapOf<String, CompletableDeferred<String?>>()

    fun triggerEmergencySOS(context: Context, reason: String) {
        Log.d(TAG, "Triggering SOS on watch...")

        // Permissions are requested by the UI before this is called.
        // We check here as a safeguard.
        val hasSmsPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED
        val hasCallPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED

        if (!hasSmsPermission || !hasCallPermission) {
            Log.e(TAG, "SOS failed: Permissions not granted.")
            // The UI should have already prompted for permissions.
            // If we are here, the user has denied them.
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            handleSosOnWatch(context, reason)
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun handleSosOnWatch(context: Context, reason: String) {
        val emergencyNumber = getEmergencyNumber(context)

        // 1. Send SMS (with manual fallback)
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
        val hasLocationPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

        var link = "위치 정보 없음"
        if (hasLocationPermission) {
            try {
                val location = fusedLocationClient.lastLocation.await()
                if (location != null) {
                    link = "https://www.google.com/maps/search/?api=1&query=${location.latitude},${location.longitude}"
                }
            } catch (e: Exception) {
                Log.e(TAG, "Watch location failed", e)
            }
        }

        val msg = "긴급 SOS!\n사유: $reason\n위치: $link"

        try {
            val smsManager = SmsManager.getDefault()
            smsManager.sendTextMessage(emergencyNumber, null, msg, null, null)
            Log.d(TAG, "SOS SMS sent directly from watch.")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send SMS from watch, falling back to manual intent.", e)
            val smsIntent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$emergencyNumber")).apply {
                putExtra("sms_body", msg)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try {
                context.startActivity(smsIntent)
            } catch (activityError: Exception) {
                Log.e(TAG, "Failed to launch SMS app on watch", activityError)
            }
        }

        // 2. Make Phone Call
        Log.d(TAG, "Initiating call from watch.")
        val callIntent = Intent(Intent.ACTION_CALL).apply {
            data = Uri.parse("tel:$emergencyNumber")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(callIntent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initiate call from watch", e)
        }
    }

    private suspend fun getEmergencyNumber(context: Context): String {
        val fallbackNumber = "01012345678"
        val requestId = UUID.randomUUID().toString()
        val requestPath = "$REQUEST_PATH/$requestId"
        val responsePath = "$RESPONSE_PATH/$requestId"

        val dataClient = Wearable.getDataClient(context)
        val deferred = CompletableDeferred<String?>()
        emergencyNumberResponseDeferreds[requestId] = deferred

        val listener = DataClient.OnDataChangedListener {
                dataEvents ->
            dataEvents.forEach { event ->
                if (event.type == DataEvent.TYPE_CHANGED) {
                    val dataItem = event.dataItem
                    if (dataItem.uri.path == responsePath) {
                        val dataMap = DataMapItem.fromDataItem(dataItem).dataMap
                        val number = dataMap.getString(KEY_EMERGENCY_NUMBER, "")
                        emergencyNumberResponseDeferreds[requestId]?.complete(number)
                        Log.d(TAG, "Received response for request $requestId: $number")
                    }
                }
            }
        }

        dataClient.addListener(listener)

        try {
            // Create and send the request DataItem
            val putDataMapReq = PutDataMapRequest.create(requestPath).apply {
                dataMap.putLong("timestamp", System.currentTimeMillis()) // Add a timestamp to ensure uniqueness
            }
            val putDataReq = putDataMapReq.asPutDataRequest().setUrgent()
            dataClient.putDataItem(putDataReq).await()
            Log.d(TAG, "Sent request for emergency number with ID: $requestId")

            // Wait for response with a timeout
            val responseNumber = withTimeoutOrNull(5000) { // 5 second timeout
                deferred.await()
            }

            return responseNumber ?: fallbackNumber
        } catch (e: Exception) {
            Log.e(TAG, "Failed to request emergency number from phone", e)
            return fallbackNumber
        } finally {
            // Clean up deferred and listener
            emergencyNumberResponseDeferreds.remove(requestId)
            dataClient.removeListener(listener)
        }
    }
}