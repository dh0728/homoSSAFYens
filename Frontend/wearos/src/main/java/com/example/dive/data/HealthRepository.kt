package com.example.dive.data

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import com.example.dive.presentation.MainViewModel
import com.example.dive.presentation.MeasurementState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class HealthRepository(context: Context) {

    private val sharedPreferences: SharedPreferences =
        PreferenceManager.getDefaultSharedPreferences(context.applicationContext)

    private val _measurementState = MutableStateFlow(
        MeasurementState(
            isMeasuring = loadIsMeasuring(),
            lastAverageHr = loadLastAverageHeartRate(),
            lastMeasuredAt = loadLastMeasuredAt()
        )
    )
    val measurementState: StateFlow<MeasurementState> = _measurementState.asStateFlow()

    // Pref change listener to sync cross-components (Service/UI)
    private val listener = SharedPreferences.OnSharedPreferenceChangeListener { sp, key ->
        when (key) {
            KEY_IS_MEASURING -> {
                val v = sp.getBoolean(KEY_IS_MEASURING, false)
                _measurementState.value = _measurementState.value.copy(isMeasuring = v)
            }
            MainViewModel.KEY_LAST_AVERAGE_HR -> {
                val hr = sp.getInt(MainViewModel.KEY_LAST_AVERAGE_HR, 0).let { if (it > 0) it else null }
                _measurementState.value = _measurementState.value.copy(lastAverageHr = hr)
            }
            MainViewModel.KEY_LAST_MEASURED_AT -> {
                val t = sp.getLong(MainViewModel.KEY_LAST_MEASURED_AT, 0L).let { if (it > 0L) it else null }
                _measurementState.value = _measurementState.value.copy(lastMeasuredAt = t)
            }
        }
    }

    init {
        sharedPreferences.registerOnSharedPreferenceChangeListener(listener)
    }

    fun setMeasuring(isMeasuring: Boolean) {
        // write-through to prefs so other process/instance can observe
        sharedPreferences.edit().putBoolean(KEY_IS_MEASURING, isMeasuring).apply()
        _measurementState.value = _measurementState.value.copy(isMeasuring = isMeasuring)
    }

    // 평균 0이면 lastMeasuredAt 갱신하지 않음 정책 유지
    fun setLastAverageHr(hr: Int?) {
        _measurementState.value = _measurementState.value.copy(lastAverageHr = hr)
        if (hr != null) {
            if (hr > 0) {
                val now = System.currentTimeMillis()
                sharedPreferences.edit()
                    .putInt(MainViewModel.KEY_LAST_AVERAGE_HR, hr)
                    .putLong(MainViewModel.KEY_LAST_MEASURED_AT, now)
                    .apply()
                _measurementState.value = _measurementState.value.copy(lastMeasuredAt = now)
            } else {
                sharedPreferences.edit()
                    .putInt(MainViewModel.KEY_LAST_AVERAGE_HR, hr)
                    .apply()
                // lastMeasuredAt 유지
            }
        } else {
            sharedPreferences.edit()
                .remove(MainViewModel.KEY_LAST_AVERAGE_HR)
                .remove(MainViewModel.KEY_LAST_MEASURED_AT)
                .apply()
            _measurementState.value = _measurementState.value.copy(lastMeasuredAt = null)
        }
    }

    private fun loadIsMeasuring(): Boolean =
        sharedPreferences.getBoolean(KEY_IS_MEASURING, false)

    private fun loadLastAverageHeartRate(): Int? {
        val lastHr = sharedPreferences.getInt(MainViewModel.KEY_LAST_AVERAGE_HR, 0)
        return if (lastHr > 0) lastHr else null
    }

    private fun loadLastMeasuredAt(): Long? {
        val t = sharedPreferences.getLong(MainViewModel.KEY_LAST_MEASURED_AT, 0L)
        return if (t > 0L) t else null
    }

    fun dispose() {
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(listener)
    }

    companion object {
        private const val KEY_IS_MEASURING = "health_is_measuring"
    }
}