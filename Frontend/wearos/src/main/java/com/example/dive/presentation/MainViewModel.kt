package com.example.dive.presentation

import android.app.Application
import android.content.SharedPreferences
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.preference.PreferenceManager
import com.example.dive.data.WatchDataRepository
import com.example.dive.data.model.*
import com.example.dive.health.HeartRateMonitor
import com.example.dive.presentation.ui.MarineActivityMode
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Tide UI 상태
sealed interface TideUiState {
    data class Success(val tideData: TideData) : TideUiState
    object Error : TideUiState
    object Loading : TideUiState
}

// Weather UI 상태
sealed interface WeatherUiState {
    data class Success(val weatherData: Weather6hData) : WeatherUiState
    object Error : WeatherUiState
    object Loading : WeatherUiState
}

// Detailed Weather UI 상태
sealed interface DetailedWeatherUiState {
    data class Success(val weatherData: Weather7dData) : DetailedWeatherUiState
    object Error : DetailedWeatherUiState
    object Loading : DetailedWeatherUiState
}

// Fishing Points UI 상태
sealed interface FishingPointsUiState {
    data class Success(val fishingData: FishingData) : FishingPointsUiState
    object Error : FishingPointsUiState
    object Loading : FishingPointsUiState
}


// Emergency UI 상태
sealed interface EmergencyUiState {
    data class Success(
        val heartRate: Int,
        val lastMeasured: String,
        val locationStatus: String,
        val marineMode: MarineActivityMode
    ) : EmergencyUiState
    object Error : EmergencyUiState
    object Loading : EmergencyUiState
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val watchDataRepository = WatchDataRepository(application)
    private val sharedPreferences: SharedPreferences =
        PreferenceManager.getDefaultSharedPreferences(application)

    private val _tideUiState = MutableStateFlow<TideUiState>(TideUiState.Loading)
    val tideUiState: StateFlow<TideUiState> = _tideUiState.asStateFlow()

    private val _weatherUiState = MutableStateFlow<WeatherUiState>(WeatherUiState.Loading)
    val weatherUiState: StateFlow<WeatherUiState> = _weatherUiState.asStateFlow()

    private val _fishingPointsUiState =
        MutableStateFlow<FishingPointsUiState>(FishingPointsUiState.Loading)
    val fishingPointsUiState: StateFlow<FishingPointsUiState> = _fishingPointsUiState.asStateFlow()

    private val _detailedWeatherUiState =
        MutableStateFlow<DetailedWeatherUiState>(DetailedWeatherUiState.Loading)
    val detailedWeatherUiState: StateFlow<DetailedWeatherUiState> =
        _detailedWeatherUiState.asStateFlow()

    private val _emergencyUiState =
        MutableStateFlow<EmergencyUiState>(EmergencyUiState.Loading)
    val emergencyUiState: StateFlow<EmergencyUiState> = _emergencyUiState.asStateFlow()

    private val _selectedMarineActivityMode: MutableStateFlow<MarineActivityMode> by lazy {
        MutableStateFlow(loadMarineActivityMode())
    }
    val selectedMarineActivityMode: StateFlow<MarineActivityMode> =
        _selectedMarineActivityMode.asStateFlow()

    private val _isMonitoringEnabled: MutableStateFlow<Boolean> by lazy {
        MutableStateFlow(loadMonitoringEnabled())
    }
    val isMonitoringEnabled: StateFlow<Boolean> = _isMonitoringEnabled.asStateFlow()

    private val heartRateMonitor: HeartRateMonitor by lazy {
        HeartRateMonitor(getApplication(), _selectedMarineActivityMode)
    }

    val phoneConnected: StateFlow<Boolean> = watchDataRepository.isConnected

    init {
        viewModelScope.launch {
            watchDataRepository.getTideData()
                .catch { _tideUiState.value = TideUiState.Error }
                .collect { _tideUiState.value = TideUiState.Success(it.data) }
        }

        viewModelScope.launch {
            watchDataRepository.getWeatherData()
                .catch { _weatherUiState.value = WeatherUiState.Error }
                .collect { _weatherUiState.value = WeatherUiState.Success(it.data) }
        }

        viewModelScope.launch {
            watchDataRepository.getWeather7dData()
                .catch { _detailedWeatherUiState.value = DetailedWeatherUiState.Error }
                .collect { _detailedWeatherUiState.value = DetailedWeatherUiState.Success(it.data) }
        }

        viewModelScope.launch {
            watchDataRepository.getFishingPoints()
                .catch { _fishingPointsUiState.value = FishingPointsUiState.Error }
                .collect { _fishingPointsUiState.value = FishingPointsUiState.Success(it.data) }
        }

        // Emergency
        viewModelScope.launch {
            combine(
                heartRateMonitor.latestHeartRate,
                watchDataRepository.getLocationData().catch {
                    emit(
                        LocationResponse(
                            "error", 0, "위치 정보 없음",
                            LocationData(0.0, 0.0, 0.0f, 0L, "위치 정보 없음")
                        )
                    )
                },
                _selectedMarineActivityMode
            ) { heartRate, locationResponse, marineMode ->
                val locationData = locationResponse.data
                val lastMeasured =
                    SimpleDateFormat("m분 전", Locale.getDefault()).format(Date())
                val locationStatus = locationData.address ?: "위치 정보 없음"
                EmergencyUiState.Success(heartRate, lastMeasured, locationStatus, marineMode)
            }.catch {
                _emergencyUiState.value = EmergencyUiState.Error
            }.collect { combined ->
                _emergencyUiState.value = combined
            }
        }
    }

    fun setSelectedMarineActivityMode(mode: MarineActivityMode) {
        _selectedMarineActivityMode.value = mode
        sharedPreferences.edit().putString(KEY_MARINE_ACTIVITY_MODE, mode.name).apply()
    }

    fun setMonitoringEnabled(enabled: Boolean) {
        _isMonitoringEnabled.value = enabled
        sharedPreferences.edit().putBoolean(KEY_MONITORING_ENABLED, enabled).apply()
    }

    private fun loadMarineActivityMode(): MarineActivityMode {
        val modeName =
            sharedPreferences.getString(KEY_MARINE_ACTIVITY_MODE, MarineActivityMode.OFF.name)
        return MarineActivityMode.valueOf(modeName ?: MarineActivityMode.OFF.name)
    }

    private fun loadMonitoringEnabled(): Boolean {
        return sharedPreferences.getBoolean(KEY_MONITORING_ENABLED, false)
    }

    companion object {
        internal const val KEY_MARINE_ACTIVITY_MODE = "marine_activity_mode"
        internal const val KEY_MONITORING_ENABLED = "monitoring_enabled"
    }
}
