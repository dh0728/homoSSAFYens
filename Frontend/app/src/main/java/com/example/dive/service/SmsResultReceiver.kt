package com.example.dive.service

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.telephony.SmsManager
import android.util.Log

class SmsSentReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val emergencyNumber = intent.getStringExtra("emergency_number")

        when (resultCode) {
            Activity.RESULT_OK -> {
                Log.d("SmsSentReceiver", "SMS sent successfully. Initiating call after 3 seconds.")
                if (emergencyNumber == null) {
                    Log.e("SmsSentReceiver", "Cannot make call, emergency number is null.")
                    return
                }
                // Wait 3 seconds, then make the call
                Handler(Looper.getMainLooper()).postDelayed({
                    val callIntent = Intent(Intent.ACTION_CALL).apply {
                        data = Uri.parse("tel:$emergencyNumber")
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    try {
                        context.startActivity(callIntent)
                    } catch (e: Exception) {
                        Log.e("SmsSentReceiver", "Failed to initiate call from receiver", e)
                    }
                }, 3000)
            }
            else -> {
                // Failure: Make phone call immediately
                Log.e("SmsSentReceiver", "SMS send failed. Fallback: Initiating phone call immediately.")
                val callIntent = Intent(Intent.ACTION_CALL).apply {
                    data = Uri.parse("tel:$emergencyNumber")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                try {
                    context.startActivity(callIntent)
                } catch (e: Exception) {
                    Log.e("SmsSentReceiver", "Failed to initiate fallback call", e)
                }
            }
        }
    }
}

class SmsDeliveredReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val resultCode = resultCode
        val resultText = when (resultCode) {
            Activity.RESULT_OK -> "SMS delivered successfully"
            Activity.RESULT_CANCELED -> "SMS not delivered"
            else -> "Unknown delivery status"
        }
        Log.d("SmsDeliveredReceiver", "SMS delivery result: $resultText (code: $resultCode)")
    }
}