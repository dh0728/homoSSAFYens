package com.example.dive.health

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
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

class HeartRateMonitor(private val context: Context, private val marineActivityModeFlow: StateFlow<MarineActivityMode>, private val healthRepository: HealthRepository) : SensorEventListener {

    private val sensorManager: SensorManager by lazy {
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    }

    private val heartRateSensor: Sensor? by lazy {
        sensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE)
    }

    private var isMonitoring = false

    private val _latestHeartRate = MutableStateFlow(0)
    val latestHeartRate = _latestHeartRate.asStateFlow()

    private var heartRateSum = 0
    private var heartRateCount = 0
    private val _averageHeartRate = MutableStateFlow(0)
    val averageHeartRate = _averageHeartRate.asStateFlow()

    // SleepAndActivityDetector is now stateless
    private val detector = SleepAndActivityDetector

    private val monitorJob = Job()
    private val monitorScope = CoroutineScope(Dispatchers.Default + monitorJob)

    // No init block needed for marineActivityModeFlow collection here anymore

    fun startMonitoring() {
        if (heartRateSensor == null) {
            Log.e("HeartRateMonitor", "Heart rate sensor not available")
            return
        }
        if (!isMonitoring) {
            // Reset for new measurement
            heartRateSum = 0
            heartRateCount = 0
            _averageHeartRate.value = 0 // Reset average as well

            sensorManager.registerListener(this, heartRateSensor, SensorManager.SENSOR_DELAY_NORMAL)
            isMonitoring = true
            Log.d("HeartRateMonitor", "Started monitoring heart rate")
        }
    }

    fun stopMonitoring() {
        if (isMonitoring) {
            sensorManager.unregisterListener(this)
            isMonitoring = false

            // Calculate average heart rate
            if (heartRateCount > 0) {
                val avgHr = heartRateSum / heartRateCount
                _averageHeartRate.value = avgHr
                Log.d("HeartRateMonitor", "Average Heart Rate: ${_averageHeartRate.value}")
                healthRepository.setLastAverageHr(avgHr)
            } else {
                _averageHeartRate.value = 0 // No valid data
                Log.d("HeartRateMonitor", "No valid heart rate data for average calculation.")
                healthRepository.setLastAverageHr(0)
            }
            Log.d("HeartRateMonitor", "Stopped monitoring heart rate")
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_HEART_RATE) {
            val heartRate = event.values[0].toInt()
            _latestHeartRate.value = heartRate
            Log.d("HeartRateMonitor", "Current Heart Rate: $heartRate")

            // Accumulate heart rate for average calculation
            if (heartRate > 0) { // Only accumulate valid heart rates
                heartRateSum += heartRate
                heartRateCount++
            }

            // Dummy values for now, these should come from actual sensors/data
            val dummyLocationData = LocationData(0.0, 0.0, 0.0f, 0L, "Dummy Address")
            val dummyRecentActivityLevel = 50
            val dummyRecentHeartRates = listOf(heartRate, heartRate + 1, heartRate - 1)
            val dummyRecentPositionData = listOf(0.1f, 0.2f, 0.15f)
            val dummyRecentMovementSpeed = 0
            val dummyRecentMovementData = listOf(0.1f, 0.2f, 0.15f)

            val currentUserState = detector.detectCurrentUserState(
                marineMode = marineActivityModeFlow.value,
                currentHeartRate = heartRate,
                locationData = dummyLocationData,
                recentActivityLevel = dummyRecentActivityLevel,
                recentHeartRates = dummyRecentHeartRates,
                recentPositionData = dummyRecentPositionData,
                recentMovementSpeed = dummyRecentMovementSpeed,
                recentMovementData = dummyRecentMovementData
            )
            val thresholds = detector.getHeartRateThresholds(currentUserState)

            // Anomaly detection using dynamic thresholds
            if (heartRate > 0 && heartRate < thresholds.criticalMin) {
                EmergencyManager.triggerEmergencySOS(context, "자동 감지: 심각한 서맥 감지 (${heartRate}bpm)")
            } else if (heartRate > 0 && heartRate < thresholds.warningMin) {
                Log.w("HeartRateMonitor", "Warning: Heart rate below warning threshold (${heartRate}bpm)")
                // TODO: Implement warning notification/vibration
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        Log.d("HeartRateMonitor", "Sensor accuracy changed: $accuracy")
    }
}
