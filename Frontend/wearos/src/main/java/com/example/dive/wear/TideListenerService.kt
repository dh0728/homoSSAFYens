package com.example.dive.wear

import android.util.Log
import com.example.dive.notify.WearNotif
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import org.json.JSONObject
import java.nio.charset.StandardCharsets

class TideListenerService : WearableListenerService() {
    override fun onMessageReceived(messageEvent: MessageEvent) {
        if (messageEvent.path == "/tide_alert") {
            try {
                val json = String(messageEvent.data, StandardCharsets.UTF_8)
                val obj = JSONObject(json)
                val title = obj.optString("title", "만조 알림")
                val body  = obj.optString("body", "만조 임박")
                val id    = obj.optInt("id", 1001)
                WearNotif.show(this, title, body, id)
            } catch (e: Exception) {
                Log.e("TideListenerService", "parse failed: ${e.message}", e)
            }
        } else {
            super.onMessageReceived(messageEvent)
        }
    }
}