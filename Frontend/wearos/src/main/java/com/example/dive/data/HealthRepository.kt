package com.example.dive.data

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
//import androidx.room.util.copy
import com.example.dive.presentation.MeasurementState
import com.example.dive.presentation.MainViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class HealthRepository(context: Context) {

    private val sharedPreferences: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

    private val _measurementState = MutableStateFlow(MeasurementState(false, loadLastAverageHeartRate()))
    val measurementState: StateFlow<MeasurementState> = _measurementState.asStateFlow()

    fun setMeasuring(isMeasuring: Boolean) {
        _measurementState.value = _measurementState.value.copy(isMeasuring = isMeasuring)
    }

    fun setLastAverageHr(hr: Int?) {
        _measurementState.value = _measurementState.value.copy(lastAverageHr = hr)
        // Save to SharedPreferences for persistence
        if (hr != null) {
            sharedPreferences.edit().putInt(MainViewModel.KEY_LAST_AVERAGE_HR, hr).apply()
        } else {
            sharedPreferences.edit().remove(MainViewModel.KEY_LAST_AVERAGE_HR).apply()
        }
    }

    private fun loadLastAverageHeartRate(): Int? {
        val lastHr = sharedPreferences.getInt(MainViewModel.KEY_LAST_AVERAGE_HR, 0)
        return if (lastHr > 0) lastHr else null
    }
}
