package com.example.dive.service

import android.content.Intent
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService

class WatchMessageListenerService : WearableListenerService() {

    override fun onMessageReceived(messageEvent: MessageEvent) {
        // 폰으로부터 받은 데이터를 브로드캐스트로 전달
        val intent = Intent(ACTION_MESSAGE_RECEIVED).apply {
            // Note: At runtime, the system uses the URI's path, which is set by the sender.
            // We add it to the Intent here just for the receiver's convenience.
            putExtra(EXTRA_PATH, messageEvent.path)
            putExtra(EXTRA_DATA, messageEvent.data)
        }
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }

    companion object {
        const val ACTION_MESSAGE_RECEIVED = "com.example.dive.MESSAGE_RECEIVED"
        const val EXTRA_PATH = "extra_path"
        const val EXTRA_DATA = "extra_data"
    }
}
