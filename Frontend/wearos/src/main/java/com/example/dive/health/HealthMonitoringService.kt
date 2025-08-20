package com.example.dive.health

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.preference.PreferenceManager
import com.example.dive.R
import com.example.dive.presentation.MainViewModel
import com.example.dive.presentation.ui.MarineActivityMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class HealthMonitoringService : Service() {

    private lateinit var heartRateMonitor: HeartRateMonitor
    private lateinit var sharedPreferences: SharedPreferences
    private val _marineActivityModeFlow = MutableStateFlow(MarineActivityMode.OFF)

    private val sharedPreferenceChangeListener = SharedPreferences.OnSharedPreferenceChangeListener {
            sharedPrefs, key ->
        if (key == MainViewModel.Companion.KEY_MARINE_ACTIVITY_MODE) { // Access Companion object explicitly
            val modeName = sharedPrefs.getString(key, MarineActivityMode.OFF.name)
            _marineActivityModeFlow.value = MarineActivityMode.valueOf(modeName ?: MarineActivityMode.OFF.name)
        }
    }

    override fun onCreate() {
        super.onCreate()
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        sharedPreferences.registerOnSharedPreferenceChangeListener(sharedPreferenceChangeListener)

        // Initialize with current value
        val initialModeName = sharedPreferences.getString(MainViewModel.Companion.KEY_MARINE_ACTIVITY_MODE, MarineActivityMode.OFF.name) // Access Companion object explicitly
        _marineActivityModeFlow.value = MarineActivityMode.valueOf(initialModeName ?: MarineActivityMode.OFF.name)

        heartRateMonitor = HeartRateMonitor(this, _marineActivityModeFlow)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createNotificationChannel()
        val notification = createNotification()
        startForeground(NOTIFICATION_ID, notification)

        heartRateMonitor.startMonitoring()

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        heartRateMonitor.stopMonitoring()
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(sharedPreferenceChangeListener)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Health Monitoring",
            NotificationManager.IMPORTANCE_DEFAULT
        )
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("해양 안전 모니터링")
            .setContentText("심박수를 측정하고 있습니다.")
            // .setSmallIcon(R.drawable.ic_launcher_foreground as Int) // TODO: Replace with a proper icon, ensure this drawable exists
            .build()
    }

    companion object {
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "HealthMonitoringServiceChannel"
    }
}