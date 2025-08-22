package com.example.dive.service

import android.content.ComponentName
import android.content.Intent
import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.wear.tiles.TileService
import androidx.wear.watchface.complications.datasource.ComplicationDataSourceUpdateRequester
import com.example.dive.data.WatchDataRepository
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

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
