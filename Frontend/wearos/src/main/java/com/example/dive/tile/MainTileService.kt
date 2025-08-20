package com.example.dive.tile

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.concurrent.futures.CallbackToFutureAdapter
import androidx.wear.tiles.ActionBuilders
import androidx.wear.tiles.ColorBuilders.argb
import androidx.wear.tiles.DimensionBuilders.dp
import androidx.wear.tiles.DimensionBuilders.sp
import androidx.wear.tiles.LayoutElementBuilders.*
import androidx.wear.tiles.ModifiersBuilders.*
import androidx.wear.tiles.RequestBuilders
import androidx.wear.tiles.ResourceBuilders
import androidx.wear.tiles.TileBuilders
import androidx.wear.tiles.TileService
import androidx.wear.tiles.TimelineBuilders
import com.example.dive.data.WatchDataRepository
import com.example.dive.data.model.TideData
import com.example.dive.data.model.TideEvent
import com.example.dive.health.HeartRateMonitor
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow

private const val RESOURCES_VERSION = "1"
private const val TAG = "MainTileService"

private object TileColors {
    const val BackgroundPrimary = 0xFF121212.toInt()
    const val TextPrimary       = 0xFFFFFFFF.toInt()
    const val TextSecondary     = 0xFFCCCCCC.toInt()
    const val AccentRed         = 0xFFFF4444.toInt()
    const val AccentBlue        = 0xFF4488FF.toInt()
    const val AccentYellow      = 0xFFFFD700.toInt()
}

/** 타일 전용 로컬 캐시 (가볍게 SharedPreferences 사용) */
private object TileCache {
    private const val PREF = "tile_cache"
    private const val KEY_TIDE = "tide_json"
    private val gson = Gson()

    fun saveTide(context: Context, data: TideData) {
        val json = gson.toJson(data)
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .edit().putString(KEY_TIDE, json).apply()
    }

    fun loadTide(context: Context): TideData? {
        val json = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .getString(KEY_TIDE, null) ?: return null
        return runCatching { gson.fromJson(json, TideData::class.java) }.getOrNull()
    }
}

class MainTileService : TileService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var repository: WatchDataRepository
    private lateinit var heartRateMonitor: HeartRateMonitor

    override fun onCreate() {
        super.onCreate()
        repository = WatchDataRepository(this)
        // Pass a dummy flow for marineActivityModeFlow as it's not relevant for TileService's HR monitoring
        heartRateMonitor = HeartRateMonitor(this, MutableStateFlow(com.example.dive.presentation.ui.MarineActivityMode.OFF))
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    override fun onTileRequest(
        requestParams: RequestBuilders.TileRequest
    ): ListenableFuture<TileBuilders.Tile> {
        return CallbackToFutureAdapter.getFuture { completer: CallbackToFutureAdapter.Completer<TileBuilders.Tile> ->
            val cached = TileCache.loadTide(this@MainTileService)
            val initial = cached ?: TideData(
                date = "",
                weekday = "",
                locationName = "로딩중",
                mul = "",
                lunar = "",
                sunrise = "",
                sunset = "",
                moonrise = "",
                moonset = "",
                events = fallbackEvents()
            )

            // Get latest heart rate value
            val currentHeartRate = heartRateMonitor.latestHeartRate.value

            val initialContent = wrapClickable(
                layout(
                    locationName = initial.locationName,
                    mul         = initial.mul,
                    events      = initial.events,
                    currentHeartRate = currentHeartRate
                )
            )
            completer.set(buildTile(initialContent))

            // 백그라운드에서 최신 데이터로 갱신 → 캐시 저장 → 타일 업데이트 요청
            val job = serviceScope.launch {
                try {
                    val latest = repository.getTideData().firstOrNull()?.data
                    if (latest != null) {
                        TileCache.saveTide(this@MainTileService, latest)
                        requestTileUpdate() // 다음 프레임에서 최신으로
                    } else {
                        Log.w(TAG, "Latest tide data is null; keeping cached tile.")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Fetch latest tide failed", e)
                }
            }
            completer.addCancellationListener({ job.cancel() }, MoreExecutors.directExecutor())
            "tile-request-cached"
        }
    }

    override fun onResourcesRequest(
        requestParams: RequestBuilders.ResourcesRequest
    ): ListenableFuture<ResourceBuilders.Resources> {
        return CallbackToFutureAdapter.getFuture { completer: CallbackToFutureAdapter.Completer<ResourceBuilders.Resources> ->
            val res = ResourceBuilders.Resources.Builder()
                .setVersion(RESOURCES_VERSION)
                .build()
            completer.set(res)
            "resources-request"
        }
    }

    /** 타일 리프레시 트리거 */
    private fun requestTileUpdate() {
        TileService.getUpdater(this).requestUpdate(MainTileService::class.java)
    }

    /** 타일 공통 빌드 */
    private fun buildTile(content: LayoutElement): TileBuilders.Tile {
        val entry = TimelineBuilders.TimelineEntry.Builder()
            .setLayout(Layout.Builder().setRoot(content).build())
            .build()
        val timeline = TimelineBuilders.Timeline.Builder()
            .addTimelineEntry(entry)
            .build()
        return TileBuilders.Tile.Builder()
            .setResourcesVersion(RESOURCES_VERSION)
            .setTimeline(timeline)
            .build()
    }

    /** 전체 탭 → 앱 메인으로 이동 */
    private fun wrapClickable(child: LayoutElement): LayoutElement {
        val launch = ActionBuilders.LaunchAction.Builder()
            .setAndroidActivity(
                ActionBuilders.AndroidActivity.Builder()
                    .setPackageName(this.packageName)
                    .setClassName("com.example.dive.presentation.MainActivity")
                    .build()
            )
            .build()
        return Column.Builder()
            .setModifiers(
                Modifiers.Builder()
                    .setClickable(
                        Clickable.Builder()
                            .setId("open_app")
                            .setOnClick(launch)
                            .build()
                    )
                    .setBackground(
                        Background.Builder()
                            .setColor(argb(TileColors.BackgroundPrimary))
                            .build()
                    )
                    .build()
            )
            .addContent(child)
            .build()
    }

    /** 메인 레이아웃 */
    private fun layout(
        locationName: String,
        mul: String,
        events: List<TideEvent>,
        currentHeartRate: Int
    ): LayoutElement {
        val highs = events.filter { it.trend == "만조" }.take(2)
        val lows  = events.filter { it.trend == "간조" }.take(2)

        val header = Row.Builder()
            .addContent(
                Text.Builder()
                    .setText(locationName)
                    .setFontStyle(
                        FontStyle.Builder()
                            .setSize(sp(16f))
                            .setColor(argb(TileColors.TextPrimary))
                            .build()
                    )
                    .build()
            )
            .addContent(Spacer.Builder().setWidth(dp(6f)).build())
            .addContent(
                Box.Builder()
                    .setWidth(dp(10f))
                    .setHeight(dp(10f))
                    .setModifiers(
                        Modifiers.Builder()
                            .setBackground(
                                Background.Builder().setColor(argb(TileColors.AccentYellow)).build()
                            )
                            .build()
                    )
                    .build()
            )
            .addContent(Spacer.Builder().setWidth(dp(6f)).build())
            .addContent(
                Text.Builder()
                    .setText(mul.ifEmpty { " " })
                    .setFontStyle(
                        FontStyle.Builder()
                            .setSize(sp(16f))
                            .setColor(argb(TileColors.AccentYellow))
                            .build()
                    )
                    .build()
            )
            .build()

        val dividerV = Box.Builder()
            .setWidth(dp(1f))
            .setHeight(dp(56f))
            .setModifiers(
                Modifiers.Builder()
                    .setBackground(Background.Builder().setColor(argb(0x33FFFFFF.toInt())).build())
                    .build()
            )
            .build()

        // Heart Rate Section - 앱 실행으로 변경
        val heartRateSection = Row.Builder()
            .addContent(
                Text.Builder()
                    .setText("HR: ${if (currentHeartRate > 0) currentHeartRate else "---"} bpm")
                    .setFontStyle(
                        FontStyle.Builder()
                            .setSize(sp(14f))
                            .setColor(argb(TileColors.TextPrimary))
                            .build()
                    )
                    .build()
            )
            .build()

        return Column.Builder()
            .addContent(Spacer.Builder().setHeight(dp(12f)).build())
            .addContent(header)
            .addContent(Spacer.Builder().setHeight(dp(10f)).build())
            .addContent(
                Row.Builder()
                    .addContent(
                        Column.Builder()
                            .addContent(tideRow(highs.getOrNull(0), invert = false))
                            .addContent(Spacer.Builder().setHeight(dp(8f)).build())
                            .addContent(tideRow(highs.getOrNull(1), invert = false))
                            .build()
                    )
                    .addContent(Spacer.Builder().setWidth(dp(10f)).build())
                    .addContent(dividerV)
                    .addContent(Spacer.Builder().setWidth(dp(10f)).build())
                    .addContent(
                        Column.Builder()
                            .addContent(tideRow(lows.getOrNull(0), invert = true))
                            .addContent(Spacer.Builder().setHeight(dp(8f)).build())
                            .addContent(tideRow(lows.getOrNull(1), invert = true))
                            .build()
                    )
                    .build()
            )
            .addContent(Spacer.Builder().setHeight(dp(12f)).build())
            .addContent(heartRateSection)
            .build()
    }

    private fun tideRow(ev: TideEvent?, invert: Boolean): LayoutElement {
        val label = if (invert) "간조" else "만조"
        val color = if (invert) TileColors.AccentBlue else TileColors.AccentRed
        return tideCell(label, ev?.time, ev?.levelCm, ev?.deltaCm, color)
    }

    private fun tideCell(
        label: String,
        time: String?,
        level: Int?,
        deltaCm: Int?,
        colorInt: Int
    ): LayoutElement {
        val arrow = when {
            deltaCm == null -> ""
            deltaCm > 0 -> "🔺+${deltaCm}"
            deltaCm < 0 -> "🔻${deltaCm}"
            else -> ""
        }
        val bg = withAlpha(colorInt, 0.18f)

        val badge = Box.Builder()
            .setModifiers(
                Modifiers.Builder()
                    .setBackground(Background.Builder().setColor(argb(colorInt)).build())
                    .build()
            )
            .addContent(
                Text.Builder()
                    .setText(label)
                    .setFontStyle(
                        FontStyle.Builder()
                            .setSize(sp(11f))
                            .setColor(argb(TileColors.TextPrimary))
                            .build()
                    )
                    .build()
            )
            .build()

        val dotted = Box.Builder()
            .setHeight(dp(1f))
            .setWidth(dp(44f))
            .setModifiers(
                Modifiers.Builder()
                    .setBackground(Background.Builder().setColor(argb(0x44FFFFFF.toInt())).build())
                    .build()
            )
            .build()

        val timeText = Text.Builder()
            .setText(time ?: "--:--")
            .setFontStyle(
                FontStyle.Builder()
                    .setSize(sp(15f))
                    .setColor(argb(TileColors.TextPrimary))
                    .build()
            )
            .build()

        val levelShown = if (level == null || level == 0) "-" else level.toString()
        val levelText = Text.Builder()
            .setText("($levelShown)")
            .setFontStyle(
                FontStyle.Builder()
                    .setSize(sp(12f))
                    .setColor(argb(TileColors.TextSecondary))
                    .build()
            )
            .build()

        val deltaColor = if ((deltaCm ?: 0) >= 0) TileColors.AccentRed else TileColors.AccentBlue
        val deltaText = Text.Builder()
            .setText(arrow)
            .setFontStyle(
                FontStyle.Builder()
                    .setSize(sp(12f))
                    .setColor(argb(deltaColor))
                    .build()
            )
            .build()

        return Column.Builder()
            .setModifiers(
                Modifiers.Builder()
                    .setBackground(Background.Builder().setColor(argb(bg)).build())
                    .build()
            )
            .addContent(badge)
            .addContent(Spacer.Builder().setHeight(dp(6f)).build())
            .addContent(dotted)
            .addContent(Spacer.Builder().setHeight(dp(6f)).build())
            .addContent(timeText)
            .addContent(levelText)
            .addContent(deltaText)
            .addContent(Spacer.Builder().setHeight(dp(6f)).build())
            .build()
    }

    private fun withAlpha(colorInt: Int, alpha: Float): Int {
        val a = ((alpha.coerceIn(0f, 1f)) * 255).toInt() and 0xFF
        return (a shl 24) or (colorInt and 0x00FFFFFF)
    }

    private fun fallbackEvents(): List<TideEvent> = listOf(
        TideEvent(trend = "만조", time = "--:--", levelCm = 0, deltaCm = 0),
        TideEvent(trend = "간조", time = "--:--", levelCm = 0, deltaCm = 0),
        TideEvent(trend = "만조", time = "--:--", levelCm = 0, deltaCm = 0),
        TideEvent(trend = "간조", time = "--:--", levelCm = 0, deltaCm = 0),
    )
}