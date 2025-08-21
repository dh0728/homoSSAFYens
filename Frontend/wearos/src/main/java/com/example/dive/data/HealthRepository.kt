package com.example.dive.data

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import com.example.dive.presentation.MeasurementState
import com.example.dive.presentation.MainViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class HealthRepository(context: Context) {

    private val sharedPreferences: SharedPreferences =
        PreferenceManager.getDefaultSharedPreferences(context)

    private val _measurementState =
        MutableStateFlow(
            MeasurementState(
                isMeasuring = false,
                lastAverageHr = loadLastAverageHeartRate(),
                lastMeasuredAt = loadLastMeasuredAt()
            )
        )
    val measurementState: StateFlow<MeasurementState> = _measurementState.asStateFlow()

    fun setMeasuring(isMeasuring: Boolean) {
        _measurementState.value = _measurementState.value.copy(isMeasuring = isMeasuring)
    }

    fun setLastAverageHr(hr: Int?) {
        _measurementState.value = _measurementState.value.copy(lastAverageHr = hr)

        if (hr != null) {
            if (hr > 0) {
                // 유효 평균(>0)일 때만 마지막 측정 시각 기록
                val now = System.currentTimeMillis()
                sharedPreferences.edit()
                    .putInt(MainViewModel.KEY_LAST_AVERAGE_HR, hr)
                    .putLong(MainViewModel.KEY_LAST_MEASURED_AT, now)
                    .apply()
                _measurementState.value = _measurementState.value.copy(lastMeasuredAt = now)
            } else {
                // hr == 0: 평균만 저장, 시간은 갱신하지 않음
                sharedPreferences.edit()
                    .putInt(MainViewModel.KEY_LAST_AVERAGE_HR, hr)
                    .apply()
                // lastMeasuredAt 유지
            }
        } else {
            // null: 평균/시간 모두 제거
            sharedPreferences.edit()
                .remove(MainViewModel.KEY_LAST_AVERAGE_HR)
                .remove(MainViewModel.KEY_LAST_MEASURED_AT)
                .apply()
            _measurementState.value = _measurementState.value.copy(lastMeasuredAt = null)
        }
    }

    private fun loadLastMeasuredAt(): Long? {
        val t = sharedPreferences.getLong(MainViewModel.KEY_LAST_MEASURED_AT, 0L)
        return if (t > 0L) t else null
    }

    private fun loadLastAverageHeartRate(): Int? {
        val lastHr = sharedPreferences.getInt(MainViewModel.KEY_LAST_AVERAGE_HR, 0)
        return if (lastHr > 0) lastHr else null
    }
}
