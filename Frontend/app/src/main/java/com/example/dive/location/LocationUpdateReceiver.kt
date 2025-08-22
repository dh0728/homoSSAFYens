package com.example.dive.location

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.OneTimeWorkRequestBuilder
import com.example.dive.work.LocateWorker
import com.google.android.gms.location.LocationResult

import androidx.work.WorkManager
import androidx.work.workDataOf

/**
 * 위치 브로드캐스트를 받아 WorkManager로 /locate 호출을 위임
 */
class LocationUpdateReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        android.util.Log.d("LocationUpdateReceiver", "onReceive(): $intent")
        val result = com.google.android.gms.location.LocationResult.extractResult(intent) ?: run {
            android.util.Log.e("LocationUpdateReceiver", "No LocationResult")
            return
        }
        val loc = result.lastLocation ?: run {
            android.util.Log.e("LocationUpdateReceiver", "lastLocation null")
            return
        }
        android.util.Log.d("LocationUpdateReceiver", "lat=${loc.latitude}, lon=${loc.longitude}")


        val w = OneTimeWorkRequestBuilder<LocateWorker>()
            .setInputData(workDataOf(
                "lat" to loc.latitude,
                "lon" to loc.longitude,
                "ts" to (System.currentTimeMillis()/1000L)
            ))
            .build()

        WorkManager.getInstance(context).enqueue(w)
    }
}
