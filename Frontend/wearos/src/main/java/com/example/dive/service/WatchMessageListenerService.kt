package com.example.dive.service

import android.content.ComponentName
import android.content.Intent
import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.wear.tiles.TileService
import androidx.wear.watchface.complications.datasource.ComplicationDataSourceUpdateRequester
import com.example.dive.data.WatchDataRepository
import com.example.dive.notify.WearNotif
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.nio.charset.StandardCharsets

class WatchMessageListenerService : WearableListenerService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var repo: WatchDataRepository

    private var lastTileUpdateAt = 0L
    private var lastCompUpdateAt = 0L
    private val UPDATE_DEBOUNCE_MS = 1200L

    override fun onCreate() {
        super.onCreate()
        repo = WatchDataRepository(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }

    override fun onMessageReceived(messageEvent: MessageEvent) {
        val path = messageEvent.path
        val data = messageEvent.data

        scope.launch {
            var updateTile = false
            var updateComp = false
            try {
                when (path) {
                    // ✅ 폰 → 워치 임시 테스트 경로 (문자열 payload)
                    // sendMessage(..., "/tide/test", "bridge-check".bytes)
                    "/tide/test" -> {
                        val payload = String(data, StandardCharsets.UTF_8)
                        WearNotif.show(
                            ctx = this@WatchMessageListenerService,
                            title = "브릿지 테스트",
                            body  = if (payload.isNotBlank()) payload else "폰에서 이벤트 수신!",
                            id    = 9999
                        )
                    }

                    // ✅ [신규] 폰에서 워치 알림만 즉시 띄우고 싶을 때 쓰는 전용 경로
                    //    sendMessage(..., "/alert/tide", {"title":"...","body":"...","id":123} as bytes)
                    "/alert/tide" -> {
                        val json = String(data, StandardCharsets.UTF_8)
                        val obj = JSONObject(json)
                        val title = obj.optString("title", "만조 알림")
                        val body  = obj.optString("body",  "만조 임박")
                        val id    = obj.optInt("id", (System.currentTimeMillis() % 100000).toInt())
                        WearNotif.show(this@WatchMessageListenerService, title, body, id)
                    }

                    "/response/tide" -> { repo.saveTideJson(data.decodeToString()); updateTile = true; updateComp = true }
                    "/response/weather" -> { repo.saveWeather6hJson(data.decodeToString()); updateComp = true }
                    "/response/7dweather" -> { repo.saveWeather7dJson(data.decodeToString()) }
                    "/response/locations" -> { repo.saveFishingJson(data.decodeToString()) }
                    "/response/current_location" -> { repo.saveLocationJson(data.decodeToString()); updateTile = true }
                }
            } catch (e: Exception) {
                Log.e("WatchMsgSvc", "Save to DataStore failed: ${e.message}", e)
            }

            if (updateTile) safeRequestTileUpdate()
            if (updateComp) safeRequestComplicationUpdate()
        }

        // 필요 시 유지(문제 발생하면 주석 처리)
        val intent = Intent(ACTION_MESSAGE_RECEIVED).apply {
            putExtra(EXTRA_PATH, path)
            putExtra(EXTRA_DATA, data)
        }
        LocalBroadcastManager.getInstance(this@WatchMessageListenerService).sendBroadcast(intent)
    }

    private fun safeRequestTileUpdate() {
        val now = System.currentTimeMillis()
        if (now - lastTileUpdateAt < UPDATE_DEBOUNCE_MS) return
        lastTileUpdateAt = now
        try {
            TileService.getUpdater(this@WatchMessageListenerService)
                .requestUpdate(com.example.dive.tile.MainTileService::class.java)
        } catch (e: Exception) {
            Log.e("WatchMsgSvc", "Tile update request failed: ${e.message}", e)
        }
    }

    private fun safeRequestComplicationUpdate() {
        val now = System.currentTimeMillis()
        if (now - lastCompUpdateAt < UPDATE_DEBOUNCE_MS) return
        lastCompUpdateAt = now
        try {
            val requester = ComplicationDataSourceUpdateRequester.create(
                this@WatchMessageListenerService,
                ComponentName(
                    this@WatchMessageListenerService,
                    com.example.dive.complication.MainComplicationService::class.java
                )
            )
            requester.requestUpdateAll()
        } catch (e: Exception) {
            Log.e("WatchMsgSvc", "Complication update request failed: ${e.message}", e)
        }
    }

    companion object {
        const val ACTION_MESSAGE_RECEIVED = "com.example.dive.MESSAGE_RECEIVED"
        const val EXTRA_PATH = "extra_path"
        const val EXTRA_DATA = "extra_data"
    }
}
