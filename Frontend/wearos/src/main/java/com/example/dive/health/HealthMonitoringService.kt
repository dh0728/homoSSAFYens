package com.example.dive.health

import android.app.Service
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import com.example.dive.App

class HealthMonitoringService : Service() {

    companion object {
        const val ACTION_START_HR_MONITORING = "com.example.dive.action.START_HR_MONITORING"
        const val ACTION_STOP_HR_MONITORING  = "com.example.dive.action.STOP_HR_MONITORING"

        private const val DEFAULT_DURATION_MS = 40_000L
        private const val SAFETY_MARGIN_MS = 1_500L
    }

    private val mainHandler by lazy { Handler(Looper.getMainLooper()) }
    private var safetyStopPosted = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val app = application as App
        val hrMonitor = app.heartRateMonitor
        val repo = app.healthRepo

        Log.d("HealthMonitoringService", "hrMonitor instance=${System.identityHashCode(hrMonitor)}")

        when (intent?.action) {
            ACTION_START_HR_MONITORING -> {
                // 즉시 측정 상태 true (UI 표기용)
                repo.setMeasuring(true)
                Log.d("HealthMonitoringService", "Started HR monitoring for ${DEFAULT_DURATION_MS / 1000} seconds.")

                // 종료 콜백: HeartRateMonitor.stopMonitoring 내부에서 평균 기록(setLastAverageHr) 호출됨
                // 평균 0이면 HealthRepository 정책에 따라 lastMeasuredAt은 갱신되지 않음
                hrMonitor.startMonitoring(durationMillis = DEFAULT_DURATION_MS) {
                    if (safetyStopPosted) {
                        safetyStopPosted = false
                    }
                    repo.setMeasuring(false)
                    Log.d("HealthMonitoringService", "Monitoring finished (callback).")
                }

                // 콜백 누락 대비 안전 타이머
                mainHandler.postDelayed({
                    safetyStopPosted = true
                    hrMonitor.stopMonitoring()
                    repo.setMeasuring(false)
                    Log.d("HealthMonitoringService", "Monitoring finished (safety timer).")
                }, DEFAULT_DURATION_MS + SAFETY_MARGIN_MS)
            }

            ACTION_STOP_HR_MONITORING -> {
                hrMonitor.stopMonitoring()
                repo.setMeasuring(false)
                Log.d("HealthMonitoringService", "Stopped HR monitoring by request.")
            }
        }

        // 측정 버스트 성격이면 STICKY일 필요는 없음
        return START_NOT_STICKY
    }
}