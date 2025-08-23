package com.example.dive.presentation

import TideWeeklyResponse
import android.app.Application
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
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
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// 낚시 포인트 탭의 내부 UI 모드 정의
sealed interface FishingPointsUiMode {
    object List : FishingPointsUiMode
    data class PointDetail(val point: FishingPoint) : FishingPointsUiMode
    data class RegionInfo(val info: FishingInfo) : FishingPointsUiMode
    data class MapView(val detail: FishingPointDetail) : FishingPointsUiMode
}

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

enum class SyncHint { NONE, PROMPT }

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = WatchDataRepository(application)
    private val sharedPreferences: SharedPreferences =
        PreferenceManager.getDefaultSharedPreferences(application)

    // --- 기존 UI States ---
    private val _tideUiState = MutableStateFlow<TideUiState>(TideUiState.Loading)
    val tideUiState: StateFlow<TideUiState> = _tideUiState.asStateFlow()
    // 🔹 주간 물때 UI State 추가
    private val _tideWeeklyState = MutableStateFlow<TideWeeklyResponse?>(null)
    val tideWeeklyState: StateFlow<TideWeeklyResponse?> = _tideWeeklyState.asStateFlow()
    private val _weatherUiState = MutableStateFlow<WeatherUiState>(WeatherUiState.Loading)
    val weatherUiState: StateFlow<WeatherUiState> = _weatherUiState.asStateFlow()
    private val _fishingPointsUiState = MutableStateFlow<FishingPointsUiState>(FishingPointsUiState.Loading)
    val fishingPointsUiState: StateFlow<FishingPointsUiState> = _fishingPointsUiState.asStateFlow()
    private val _detailedWeatherUiState = MutableStateFlow<DetailedWeatherUiState>(DetailedWeatherUiState.Loading)
    val detailedWeatherUiState: StateFlow<DetailedWeatherUiState> = _detailedWeatherUiState.asStateFlow()
    private val _emergencyUiState = MutableStateFlow<EmergencyUiState>(EmergencyUiState.Loading)
    val emergencyUiState: StateFlow<EmergencyUiState> = _emergencyUiState.asStateFlow()
    private val _syncHintState = MutableStateFlow(SyncHint.NONE)
    val syncHintState: StateFlow<SyncHint> = _syncHintState.asStateFlow()

    // --- 신규/수정된 상태 ---
    private val _fishingPointsUiMode = MutableStateFlow<FishingPointsUiMode>(FishingPointsUiMode.List)
    val fishingPointsUiMode: StateFlow<FishingPointsUiMode> = _fishingPointsUiMode.asStateFlow()
    var lastPagerPage: Int = 0 // 낚시 포인트 탭의 기본 페이지 인덱스
        private set

    private val _phoneLocation = MutableStateFlow<LocationData?>(null)

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

    // === App 싱글톤 참조 ===
    private val app = getApplication() as com.example.dive.App
    private val healthRepo = app.healthRepo
    val measurementState: StateFlow<MeasurementState> = healthRepo.measurementState
    private val hrMonitor = app.heartRateMonitor

    // 실시간 HR 스무딩: 0이면 직전 유효값 유지
    val liveHeartRateStable: StateFlow<Int> =
        hrMonitor.latestHeartRate
            .scan(0) { prevValid, now -> if (now > 0) now else prevValid }
            .distinctUntilChanged()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 0),
                initialValue = 0
            )

    private var initialSyncDone = false
    private var pollingJob: Job? = null

    init {
        // HeartRateMonitor가 모드 Flow 교체를 지원하도록 했을 때만 호출
        // hrMonitor.setModeFlow(_selectedMarineActivityMode)

        _emergencyUiState.value = EmergencyUiState.Success(
            lastMeasured = "--:--:--",
            locationStatus = "폰앱 열고 새로고침 필요",
            marineMode = _selectedMarineActivityMode.value,
            averageHeartRate = measurementState.value.lastAverageHr
        )

        // Tide
        viewModelScope.launch {
            repo.getTideData()
                .catch { _tideUiState.value = TideUiState.Error }
                .collect {
                    _tideUiState.value = TideUiState.Success(it.data)
                    markInitialSyncReceived()
                }
        }
        // MainViewModel.kt init 안에서
        viewModelScope.launch {
            repo.getTide7dData()
                .catch { e ->
                    Log.e("MainViewModel", "주간 물때 수집 실패", e)
                    _tideWeeklyState.value = null
                }
                .collect { weekly ->
                    Log.d("MainViewModel", "주간 물때 수신: ${weekly.data.size}일치")
                    _tideWeeklyState.value = weekly
                }
        }

        // Weather 6h
        viewModelScope.launch {
            repo.getWeatherData()
                .catch { _weatherUiState.value = WeatherUiState.Error }
                .collect { _weatherUiState.value = WeatherUiState.Success(it.data) }
        }

        // Weather 7d
        viewModelScope.launch {
            repo.getWeather7dData()
                .catch { _detailedWeatherUiState.value = DetailedWeatherUiState.Error }
                .collect { _detailedWeatherUiState.value = DetailedWeatherUiState.Success(it.data) }
        }

        // Fishing
        viewModelScope.launch {
            repo.getFishingPoints()
                .catch { _fishingPointsUiState.value = FishingPointsUiState.Error }
                .collect { _fishingPointsUiState.value = FishingPointsUiState.Success(it.data) }
        }

        // Emergency (위치)
        viewModelScope.launch {
            repo.getLocationData()
                .catch { /* keep fallback */ }
                .collect { locationResponse ->
                    _phoneLocation.value = locationResponse.data // 위치 정보 별도 저장

                    val lat = locationResponse.data.latitude
                    val lon = locationResponse.data.longitude
                    val locationStatus = if (!(lat == 0.0 && lon == 0.0)) {
                        val latStr = String.format(Locale.getDefault(), "%.4f", lat)
                        val lonStr = String.format(Locale.getDefault(), "%.4f", lon)
                        "($latStr, $lonStr)"
                    } else "폰앱 열고 새로고침 필요"

                    val measuredAt = measurementState.value.lastMeasuredAt
                    val lastMeasured = measuredAt?.let {
                        SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(it))
                    } ?: "--:--:--"

                    _emergencyUiState.value = EmergencyUiState.Success(
                        lastMeasured = lastMeasured,
                        locationStatus = locationStatus,
                        marineMode = _selectedMarineActivityMode.value,
                        averageHeartRate = measurementState.value.lastAverageHr
                    )

                    markInitialSyncReceived()
                }
        }

        

        // 초기 및 폴링 요청
        viewModelScope.launch { safeRequestRefresh("initial") }
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
        viewModelScope.launch {
            val gotAny = waitAnyDataArrived(timeoutMs = 8000L)
            if (!gotAny) {
                _syncHintState.value = SyncHint.PROMPT
                startPollingRefresh()
            }
        }
    }

    // --- 신규 화면 전환 함수 ---
    fun showPointDetail(point: FishingPoint, fromPage: Int) {
        lastPagerPage = fromPage
        _fishingPointsUiMode.value = FishingPointsUiMode.PointDetail(point)
    }

    fun showRegionInfo(fromPage: Int) {
        val currentData = (fishingPointsUiState.value as? FishingPointsUiState.Success)?.fishingData
        if (currentData != null) {
            lastPagerPage = fromPage
            _fishingPointsUiMode.value = FishingPointsUiMode.RegionInfo(currentData.info)
        }
    }

    fun showMap(point: FishingPoint, fromPage: Int) {
        lastPagerPage = fromPage
        val detail = FishingPointDetail(point = point, phoneLocation = _phoneLocation.value)
        _fishingPointsUiMode.value = FishingPointsUiMode.MapView(detail)
        
    }

    fun returnToList() {
        _fishingPointsUiMode.value = FishingPointsUiMode.List
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
        // hrMonitor.setModeFlow(_selectedMarineActivityMode) // 필요 시
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
