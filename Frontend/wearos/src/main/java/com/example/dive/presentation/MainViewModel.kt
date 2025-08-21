package com.example.dive.presentation

import android.app.Application
import android.content.SharedPreferences
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.preference.PreferenceManager
import com.example.dive.data.HealthRepository
import com.example.dive.data.WatchDataRepository
import com.example.dive.data.model.*
import com.example.dive.health.HeartRateMonitor
import com.example.dive.presentation.ui.MarineActivityMode
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.onStart
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
        val lastMeasured: String,
        val locationStatus: String,
        val marineMode: MarineActivityMode,
        val averageHeartRate: Int?
    ) : EmergencyUiState
    object Error : EmergencyUiState
    object Loading : EmergencyUiState
}

data class MeasurementState(
    val isMeasuring: Boolean,
    val lastAverageHr: Int?,
    val lastMeasuredAt: Long?
)

// 첫 화면 동기화 힌트 상태(옵션)
enum class SyncHint { NONE, PROMPT }

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = WatchDataRepository(application)
    private val sharedPreferences: SharedPreferences =
        PreferenceManager.getDefaultSharedPreferences(application)

    // UI States
    private val _tideUiState = MutableStateFlow<TideUiState>(TideUiState.Loading)
    val tideUiState: StateFlow<TideUiState> = _tideUiState.asStateFlow()

    private val _weatherUiState = MutableStateFlow<WeatherUiState>(WeatherUiState.Loading)
    val weatherUiState: StateFlow<WeatherUiState> = _weatherUiState.asStateFlow()

    private val _fishingPointsUiState = MutableStateFlow<FishingPointsUiState>(FishingPointsUiState.Loading)
    val fishingPointsUiState: StateFlow<FishingPointsUiState> = _fishingPointsUiState.asStateFlow()

    private val _detailedWeatherUiState = MutableStateFlow<DetailedWeatherUiState>(DetailedWeatherUiState.Loading)
    val detailedWeatherUiState: StateFlow<DetailedWeatherUiState> = _detailedWeatherUiState.asStateFlow()

    private val _emergencyUiState = MutableStateFlow<EmergencyUiState>(EmergencyUiState.Loading)
    val emergencyUiState: StateFlow<EmergencyUiState> = _emergencyUiState.asStateFlow()

    // 첫 화면 안내 배지(옵션)
    private val _syncHintState = MutableStateFlow(SyncHint.NONE)
    val syncHintState: StateFlow<SyncHint> = _syncHintState.asStateFlow()

    // Preferences-backed states
    private val _selectedMarineActivityMode: MutableStateFlow<MarineActivityMode> by lazy {
        MutableStateFlow(loadMarineActivityMode())
    }
    val selectedMarineActivityMode: StateFlow<MarineActivityMode> = _selectedMarineActivityMode.asStateFlow()

    private val _isMonitoringEnabled: MutableStateFlow<Boolean> by lazy {
        MutableStateFlow(loadMonitoringEnabled())
    }
    val isMonitoringEnabled: StateFlow<Boolean> = _isMonitoringEnabled.asStateFlow()

    val phoneConnected: StateFlow<Boolean> = repo.isConnected

    // Health
    private val healthRepo = HealthRepository(application)
    val measurementState: StateFlow<MeasurementState> = healthRepo.measurementState
    private val hrMonitor = HeartRateMonitor(getApplication(), _selectedMarineActivityMode, healthRepo)
    val liveHeartRate: StateFlow<Int> = hrMonitor.latestHeartRate

    // 내부 플래그/작업
    private var initialSyncDone = false
    private var pollingJob: Job? = null

    init {
        // Emergency 화면: 즉시 폴백 Success 한 번 표시
        _emergencyUiState.value = EmergencyUiState.Success(
            lastMeasured = "--:--:--",
            locationStatus = "위치 정보 없음",
            marineMode = _selectedMarineActivityMode.value,
            averageHeartRate = hrMonitor.averageHeartRate.value
        )

        // 1) Tide
        viewModelScope.launch {
            repo.getTideData()
                .catch { _tideUiState.value = TideUiState.Error }
                .collect {
                    _tideUiState.value = TideUiState.Success(it.data)
                    markInitialSyncReceived()
                }
        }

        // 2) Weather 6h
        viewModelScope.launch {
            repo.getWeatherData()
                .catch { _weatherUiState.value = WeatherUiState.Error }
                .collect { _weatherUiState.value = WeatherUiState.Success(it.data) }
        }

        // 3) Weather 7d
        viewModelScope.launch {
            repo.getWeather7dData()
                .catch { _detailedWeatherUiState.value = DetailedWeatherUiState.Error }
                .collect { _detailedWeatherUiState.value = DetailedWeatherUiState.Success(it.data) }
        }

        // 4) Fishing
        viewModelScope.launch {
            repo.getFishingPoints()
                .catch { _fishingPointsUiState.value = FishingPointsUiState.Error }
                .collect { _fishingPointsUiState.value = FishingPointsUiState.Success(it.data) }
        }

        // 5) Emergency - 위치 도착 시 갱신
        viewModelScope.launch {
            repo.getLocationData()
                .catch { /* 위치 실패는 폴백 유지 */ }
                .collect { locationResponse ->
                    val measuredAt = measurementState.value.lastMeasuredAt
                    val lastMeasured = measuredAt?.let {
                        SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(it))
                    } ?: "--:--:--"
                    val locationStatus = locationResponse.data.address ?: "위치 정보 없음"
                    _emergencyUiState.value = EmergencyUiState.Success(
                        lastMeasured = lastMeasured,
                        locationStatus = locationStatus,
                        marineMode = _selectedMarineActivityMode.value,
                        averageHeartRate = hrMonitor.averageHeartRate.value
                    )
                    markInitialSyncReceived()
                }
        }

        // 6) 자동 요청 - 앱 시작 직후 1회
        viewModelScope.launch { safeRequestRefresh("initial") }

        // 7) 연결 true 전환 시 최초 1회 재요청
        viewModelScope.launch {
            var fired = false
            phoneConnected
                .onStart { emit(false) }
                .distinctUntilChanged()
                .collect { connected ->
                    if (connected && !fired && !initialSyncDone) {
                        fired = true
                        safeRequestRefresh("on-connect")
                    }
                }
        }

        // 8) 초기 타임아웃 후 폴링 시작(응답 없을 때)
        viewModelScope.launch {
            val gotAny = waitAnyDataArrived(timeoutMs = 8000L)
            if (!gotAny) {
                _syncHintState.value = SyncHint.PROMPT
                startPollingRefresh()
            }
        }
    }

    private suspend fun waitAnyDataArrived(timeoutMs: Long): Boolean {
        val start = System.currentTimeMillis()
        while (System.currentTimeMillis() - start < timeoutMs) {
            val tideHas = (_tideUiState.value is TideUiState.Success)
            val emergencyHas = (_emergencyUiState.value is EmergencyUiState.Success)
            if (tideHas || emergencyHas) return true
            delay(200L)
        }
        return false
    }

    private fun markInitialSyncReceived() {
        if (!initialSyncDone) {
            initialSyncDone = true
            _syncHintState.value = SyncHint.NONE
            pollingJob?.cancel()
            pollingJob = null
        }
    }

    private fun startPollingRefresh() {
        if (pollingJob?.isActive == true) return
        pollingJob = viewModelScope.launch {
            val backoffs = listOf(6000L, 8000L, 10000L)
            for ((idx, waitMs) in backoffs.withIndex()) {
                if (initialSyncDone) break

                if (phoneConnected.value) {
                    safeRequestRefresh("polling-connect-$idx")
                } else {
                    safeRequestRefresh("polling-$idx")
                }

                val got = waitAnyDataArrived(timeoutMs = waitMs)
                if (got) break
            }
        }
    }

    private suspend fun safeRequestRefresh(tag: String) {
        runCatching { repo.requestDataFromServer("/request/refresh_all_data") }
            .onSuccess { Log.d("MainViewModel", "Refresh request($tag) sent.") }
            .onFailure { Log.e("MainViewModel", "Refresh request($tag) failed: ${it.message}") }
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
        val modeName = sharedPreferences.getString(KEY_MARINE_ACTIVITY_MODE, MarineActivityMode.OFF.name)
        return MarineActivityMode.valueOf(modeName ?: MarineActivityMode.OFF.name)
    }

    private fun loadMonitoringEnabled(): Boolean {
        return sharedPreferences.getBoolean(KEY_MONITORING_ENABLED, false)
    }

    fun requestDataRefresh() {
        viewModelScope.launch { safeRequestRefresh("manual") }
    }

    companion object {
        internal const val KEY_MARINE_ACTIVITY_MODE = "marine_activity_mode"
        internal const val KEY_MONITORING_ENABLED = "monitoring_enabled"
        internal const val KEY_LAST_AVERAGE_HR = "last_average_heart_rate"
        internal const val KEY_LAST_MEASURED_AT = "last_measured_at"
    }
}