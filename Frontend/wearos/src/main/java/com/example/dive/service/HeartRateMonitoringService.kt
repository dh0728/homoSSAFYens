package com.example.dive.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.dive.R
import com.example.dive.data.HealthRepository
import com.example.dive.health.HeartRateMonitor
import com.example.dive.presentation.MainActivity
import java.util.concurrent.TimeUnit

class HeartRateMonitoringService : Service() {

    private lateinit var heartRateMonitor: HeartRateMonitor
    private lateinit var healthRepository: HealthRepository
    private val handler = Handler(Looper.getMainLooper())
    private val measurementIntervalMillis = TimeUnit.MINUTES.toMillis(1) // 1-minute interval
    private val MEASUREMENT_DURATION_MILLIS = 40_000L // Duration for each measurement

    private val measurementRunnable = object : Runnable {
        override fun run() {
            Log.d(TAG, "Triggering periodic heart rate measurement.")
            heartRateMonitor.startMonitoring(MEASUREMENT_DURATION_MILLIS) { avgHr ->
                Log.d(TAG, "Periodic measurement complete. Average HR: $avgHr")
                // You might want to do something with avgHr here, like save it or update UI
            }
            handler.postDelayed(this, measurementIntervalMillis) // Schedule next measurement
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "HeartRateMonitoringService onCreate")
        healthRepository = HealthRepository(applicationContext)
        heartRateMonitor = HeartRateMonitor(applicationContext, healthRepository)
        startForegroundService()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "HeartRateMonitoringService onStartCommand")
        handler.post(measurementRunnable) // Start the periodic measurements
        return START_STICKY // Service will be restarted if killed by system
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "HeartRateMonitoringService onDestroy")
        handler.removeCallbacks(measurementRunnable) // Stop periodic measurements
        heartRateMonitor.stopMonitoring() // Ensure sensor listener is unregistered
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null // Not a bound service
    }

    private fun startForegroundService() {
        val channelId = "heart_rate_monitoring_channel"
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create Notification Channel
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "심박수 모니터링",
                NotificationManager.IMPORTANCE_LOW // Low importance for ongoing background task
            ).apply {
                description = "백그라운드에서 심박수를 모니터링 중입니다."
            }
            notificationManager.createNotificationChannel(channel)
        }

        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            notificationIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("심박수 모니터링 중")
            .setContentText("백그라운드에서 심박수를 측정하고 있습니다.")
            .setSmallIcon(R.drawable.logo) // Use your app's icon
            .setContentIntent(pendingIntent)
            .setOngoing(true) // Makes the notification non-dismissible
            .build()

        startForeground(NOTIFICATION_ID, notification)
    }

    companion object {
        private const val TAG = "HRMonitoringService"
        private const val NOTIFICATION_ID = 1002 // Unique ID for foreground service notification
    }
}
