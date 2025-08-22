package com.example.dive.health

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Handler
import android.os.Looper
import android.util.Log
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

import com.example.dive.data.HealthRepository

class HeartRateMonitor(
    private val context: Context,
    private var marineActivityModeFlow: StateFlow<MarineActivityMode>,
    private val healthRepository: HealthRepository
) : SensorEventListener {

    fun setModeFlow(flow: StateFlow<MarineActivityMode>) {
        this.marineActivityModeFlow = flow
    }

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

    fun startMonitoring(durationMillis: Long = 40_000L, onStopped: (() -> Unit)? = null) {
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
            onStopped?.invoke()
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
        Log.d("HeartRateMonitor", "Stopped monitoring heart rate")
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
