package com.example.dive.emergency

import android.content.Context
import android.util.Log
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

object EmergencyManager {

    private const val TAG = "EmergencyManager"
    private const val CAPABILITY_PHONE_APP = "homossafyens_phone_app"
    private const val SOS_MESSAGE_PATH = "/emergency/sos"

    fun triggerEmergencySOS(context: Context, reason: String) {
        Log.d(TAG, "Triggering SOS with reason: $reason")
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val nodes = Wearable.getCapabilityClient(context).getCapability(CAPABILITY_PHONE_APP, CapabilityClient.FILTER_REACHABLE).await().nodes
                nodes.firstOrNull()?.id?.let { nodeId -> // Changed 'it' to 'nodeId' for clarity
                    val payload = reason.toByteArray()
                    Wearable.getMessageClient(context).sendMessage(nodeId, SOS_MESSAGE_PATH, payload)
                        .addOnSuccessListener { Log.d(TAG, "SOS message sent to $nodeId") } // Changed 'it' to 'nodeId'
                        .addOnFailureListener { e -> Log.e(TAG, "Failed to send SOS message", e) }
                } ?: Log.e(TAG, "No reachable phone node found for SOS")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to get capable nodes for SOS", e)
            }
        }
    }
}