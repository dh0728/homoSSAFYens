package com.example.dive.complication

import android.app.PendingIntent
import android.content.Intent
import androidx.wear.watchface.complications.data.*
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import androidx.wear.watchface.complications.datasource.SuspendingComplicationDataSourceService
import com.example.dive.data.WatchDataRepository
import com.example.dive.presentation.MainActivity
import kotlinx.coroutines.flow.firstOrNull

class MainComplicationService : SuspendingComplicationDataSourceService() {

    private lateinit var repository: WatchDataRepository

    override fun onCreate() {
        super.onCreate()
        repository = WatchDataRepository(this)
    }

    override fun getPreviewData(type: ComplicationType): ComplicationData? {
        if (type != ComplicationType.SHORT_TEXT) {
            return null
        }
        return ShortTextComplicationData.Builder(
            text = PlainComplicationText.Builder(text = "만조 10:30").build(),
            contentDescription = PlainComplicationText.Builder(text = "다음 물때 정보").build()
        ).setTitle(PlainComplicationText.Builder(text = "26°").build())
            .build()
    }

    override suspend fun onComplicationRequest(request: ComplicationRequest): ComplicationData? {
        val tideResponse = repository.getTideData().firstOrNull()
        val weatherResponse = repository.getWeatherData().firstOrNull()

        val tideData = tideResponse?.data
        val weatherData = weatherResponse?.data

        val nextTideEvent = tideData?.events?.firstOrNull()
        val currentWeatherData = weatherData?.weather?.firstOrNull()

        val tideStatusText = nextTideEvent?.let { "${it.trend} ${it.time}" } ?: "물때 정보 없음"
        val weatherTempText = currentWeatherData?.let { "${it.tempC}°" } ?: "-"

        val contentDescription = "물때 및 날씨 정보"

        val tapIntent = Intent(this, MainActivity::class.java)
        val tapPendingIntent = PendingIntent.getActivity(
            this,
            0,
            tapIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return when (request.complicationType) {
            ComplicationType.SHORT_TEXT -> ShortTextComplicationData.Builder(
                text = PlainComplicationText.Builder(text = tideStatusText).build(),
                contentDescription = PlainComplicationText.Builder(text = contentDescription).build()
            ).setTitle(PlainComplicationText.Builder(text = weatherTempText).build())
                .setTapAction(tapPendingIntent)
                .build()

            else -> null
        }
    }
}