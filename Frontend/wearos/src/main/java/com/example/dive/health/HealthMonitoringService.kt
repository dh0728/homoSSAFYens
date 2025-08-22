package com.example.dive.health

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.dive.App
import com.example.dive.R

class HealthMonitoringService : Service() {

    companion object {
        const val ACTION_START_HR_MONITORING = "com.example.dive.action.START_HR_MONITORING"
        const val ACTION_STOP_HR_MONITORING = "com.example.dive.action.STOP_HR_MONITORING"

        private const val DEFAULT_DURATION_MS = 40_000L
        private const val SAFETY_MARGIN_MS = 1_500L

        // Foreground notification
        private const val CHANNEL_ID = "HealthMonitoringServiceChannel"
        private const val CHANNEL_NAME = "Health Monitoring"
        private const val NOTIFICATION_ID = 1001
    }

    private val mainHandler by lazy { Handler(Looper.getMainLooper()) }
    private var safetyStopPosted = false
    private var foregroundStarted = false
    private var stoppedOnce = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val app = application as App
        val hrMonitor = app.heartRateMonitor
        val repo = app.healthRepo

        Log.d("HealthMonitoringService", "hrMonitor instance=${System.identityHashCode(hrMonitor)}")

        when (intent?.action) {
            ACTION_START_HR_MONITORING -> {
                // 1) Foreground로 승격 (즉시)
                ensureForeground()

                // 2) UI 표시용 측정 상태 on
                repo.setMeasuring(true)
                Log.d("HealthMonitoringService", "Started HR monitoring for ${DEFAULT_DURATION_MS / 1000} seconds.")

                // 3) 모니터링 시작 (콜백은 stopMonitoring 내부 평균 기록 이후)
                hrMonitor.startMonitoring(durationMillis = DEFAULT_DURATION_MS) {
                    // 콜백이 먼저 오면 safety 타이머 중복 정지 방지
                    if (safetyStopPosted) {
                        safetyStopPosted = false
                    }
                    // UI 상태 off
                    repo.setMeasuring(false)
                    Log.d("HealthMonitoringService", "Monitoring finished (callback).")
                    // 안전 종료
                    stopForegroundSafely()
                }

                // 4) 콜백 누락 대비 안전 타이머
                mainHandler.postDelayed({
                    safetyStopPosted = true
                    hrMonitor.stopMonitoring() // 평균 기록은 stopMonitoring 내부에서 수행
                    repo.setMeasuring(false)
                    Log.d("HealthMonitoringService", "Monitoring finished (safety timer).")
                    stopForegroundSafely()
                }, DEFAULT_DURATION_MS + SAFETY_MARGIN_MS)
            }

            ACTION_STOP_HR_MONITORING -> {
                hrMonitor.stopMonitoring()
                repo.setMeasuring(false)
                Log.d("HealthMonitoringService", "Stopped HR monitoring by request.")
                stopForegroundSafely()
            }

            else -> {
                // no-op
            }
        }

        return START_NOT_STICKY
    }

    private fun ensureForeground() {
        if (foregroundStarted) return
        createNotificationChannel()
        val notification = buildNotification(content = "심박수 측정 중…")
        startForeground(NOTIFICATION_ID, notification)
        foregroundStarted = true
    }

    private fun stopForegroundSafely() {
        if (stoppedOnce) return
        stoppedOnce = true
        try {
            if (foregroundStarted) {
                stopForeground(true)
            }
        } catch (_: Throwable) {
        } finally {
            foregroundStarted = false
            stopSelf()
        }
    }

    private fun createNotificationChannel() {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_LOW // Wear에는 LOW/DEFAULT 권장
        ).apply {
            description = "해양 안전을 위한 심박수 측정"
            setShowBadge(false)
        }
        manager.createNotificationChannel(channel)
    }

    private fun buildNotification(content: String): Notification {
        // R.drawable.ic_stat_heart 같은 리소스가 없다면 android 기본 아이콘 사용
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_info_details)
            .setContentTitle("해양 안전 모니터링")
            .setContentText(content)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
}
