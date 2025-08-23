package com.example.dive.health

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import androidx.core.app.NotificationCompat
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.preference.PreferenceManager
import com.example.dive.data.model.LocationData
import com.example.dive.emergency.EmergencyManager
import com.example.dive.presentation.ui.MarineActivityMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.example.dive.R // Added this

import com.example.dive.data.HealthRepository
import com.example.dive.presentation.MainViewModel
import com.example.dive.presentation.ui.EmergencyAlertActivity
import com.example.dive.presentation.ui.EmergencyCountdownActivity

class HeartRateMonitor(
    private val context: Context,
    private val healthRepository: HealthRepository
) : SensorEventListener {

    private val sensorManager: SensorManager by lazy {
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    }

    private val heartRateSensor: Sensor? by lazy {
        sensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE)
    }

    private var isMonitoring = false

    private val _latestHeartRate = MutableStateFlow(0)
    val latestHeartRate: StateFlow<Int> = _latestHeartRate.asStateFlow()

    private var heartRateSum = 0
    private var heartRateCount = 0

    private val warmupSkipCount = 5
    private var skipped = 0

    private val _averageHeartRate = MutableStateFlow(0)
    val averageHeartRate = _averageHeartRate.asStateFlow()

    // SleepAndActivityDetector is now stateless
    private val detector = SleepAndActivityDetector

    private val monitorJob = Job()
    private val monitorScope = CoroutineScope(Dispatchers.Default + monitorJob)

    // No init block needed for marineActivityModeFlow collection here anymore

    fun startMonitoring(durationMillis: Long = 40_000L, onMeasurementComplete: ((Int) -> Unit)? = null) {
        if (!isMonitoring) {
            // 리셋
            heartRateSum = 0
            heartRateCount = 0
            skipped = 0
            _latestHeartRate.value = 0

            sensorManager.registerListener(this, heartRateSensor, SensorManager.SENSOR_DELAY_NORMAL)
            isMonitoring = true
            Log.d("HeartRateMonitor", "Started monitoring heart rate")
        }

        // duration 후 자동 종료
        Handler(Looper.getMainLooper()).postDelayed({
            stopMonitoring()
            onMeasurementComplete?.invoke(if (heartRateCount > 0) heartRateSum / heartRateCount else 0)
        }, durationMillis)
    }

    fun stopMonitoring() {
        if (!isMonitoring) return
        sensorManager.unregisterListener(this)
        isMonitoring = false

        // 평균 계산 및 기록
        val avg = if (heartRateCount > 0) heartRateSum / heartRateCount else 0
        healthRepository.setLastAverageHr(avg.takeIf { it > 0 } ?: 0)
        Log.d("HeartRateMonitor", "Average Heart Rate: ${avg}")
        checkAndTriggerEmergency(avg) // Call new function
        Log.d("HeartRateMonitor", "Stopped monitoring heart rate")
    }

    private fun checkAndTriggerEmergency(measuredHr: Int) {
        val sharedPreferences: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        val modeName = sharedPreferences.getString(MainViewModel.KEY_MARINE_ACTIVITY_MODE, MarineActivityMode.OFF.name)
        val currentMarineMode = MarineActivityMode.valueOf(modeName ?: MarineActivityMode.OFF.name)

        val userState = UserState(
            isSleeping = false, // Placeholder
            activityLevel = ActivityLevel.RESTING, // Placeholder
            marineActivityMode = currentMarineMode,
            confidence = 1.0f,
            detectionMethod = "HeartRateMonitor"
        )

        val thresholds = detector.getHeartRateThresholds(userState)
        if (measuredHr > 0 && measuredHr < thresholds.criticalMin) {
            Log.e("HeartRateMonitor", "CRITICAL LOW HEART RATE DETECTED: $measuredHr bpm (Threshold: ${thresholds.criticalMin})")

            val intent = Intent(context, EmergencyCountdownActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type != Sensor.TYPE_HEART_RATE) return

        val hr = event.values[0].toInt()

        // 초기 5 이벤트 스킵(실시간/평균 모두 미반영)
        if (skipped < warmupSkipCount) {
            skipped++
            Log.d("HeartRateMonitor", "Warmup skip $skipped/$warmupSkipCount (raw=$hr)")
            return
        }

        // 실시간 반영
        _latestHeartRate.value = hr
        Log.d("HeartRateMonitor", "Current Heart Rate: $hr")

        // 평균 누적(유효값만)
        if (hr > 0) {
            heartRateSum += hr
            heartRateCount++
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        Log.d("HeartRateMonitor", "Sensor accuracy changed: $accuracy")
    }
}
