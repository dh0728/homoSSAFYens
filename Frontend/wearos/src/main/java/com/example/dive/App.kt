package com.example.dive

import android.app.Application
import com.example.dive.data.HealthRepository
import com.example.dive.health.HeartRateMonitor
import com.example.dive.notify.WearNotif
import com.example.dive.presentation.ui.MarineActivityMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class App : Application() {

    object AppHolder {
        lateinit var appContext: Application
            internal set
    }

    lateinit var healthRepo: HealthRepository
        private set

    // HeartRateMonitor가 모드 StateFlow를 요구하므로 기본 OFF로 시작하는 fallback 제공
    private val _marineModeFallback = MutableStateFlow(MarineActivityMode.OFF)
    val marineModeFallback: StateFlow<MarineActivityMode> = _marineModeFallback

    lateinit var heartRateMonitor: HeartRateMonitor
        private set

    override fun onCreate() {
        super.onCreate()

        healthRepo = HealthRepository(this)

        heartRateMonitor = HeartRateMonitor(
            context = this,
            marineActivityModeFlow = marineModeFallback,
            healthRepository = healthRepo
        )

//        WearNotif.init(this);
        WearNotif.ensureChannel(this);
    }
}
