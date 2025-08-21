package com.example.dive.tile

import android.util.Log
import androidx.concurrent.futures.CallbackToFutureAdapter
import androidx.wear.tiles.ActionBuilders
import androidx.wear.tiles.ColorBuilders.argb
import androidx.wear.tiles.DimensionBuilders.dp
import androidx.wear.tiles.DimensionBuilders.sp
import androidx.wear.tiles.LayoutElementBuilders.Column
import androidx.wear.tiles.LayoutElementBuilders.FontStyle
import androidx.wear.tiles.LayoutElementBuilders.Layout
import androidx.wear.tiles.LayoutElementBuilders.LayoutElement
import androidx.wear.tiles.LayoutElementBuilders.Row
import androidx.wear.tiles.LayoutElementBuilders.Spacer
import androidx.wear.tiles.LayoutElementBuilders.Text
import androidx.wear.tiles.LayoutElementBuilders.Box
import androidx.wear.tiles.ModifiersBuilders.Background
import androidx.wear.tiles.ModifiersBuilders.Clickable
import androidx.wear.tiles.ModifiersBuilders.Modifiers
import androidx.wear.tiles.RequestBuilders
import androidx.wear.tiles.ResourceBuilders
import androidx.wear.tiles.TileBuilders
import androidx.wear.tiles.TileService
import androidx.wear.tiles.TimelineBuilders
import com.example.dive.data.HealthRepository
import com.example.dive.data.WatchDataRepository
import com.example.dive.data.model.TideData
import com.example.dive.data.model.TideEvent
import com.example.dive.health.HeartRateMonitor
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

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

class MainTileService : TileService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var repository: WatchDataRepository
    private lateinit var heartRateMonitor: HeartRateMonitor

    override fun onCreate() {
        super.onCreate()
        repository = WatchDataRepository(this)
        heartRateMonitor = HeartRateMonitor(
            this,
            MutableStateFlow(com.example.dive.presentation.ui.MarineActivityMode.OFF),
            HealthRepository(this)
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    override fun onTileRequest(
        requestParams: RequestBuilders.TileRequest
    ): ListenableFuture<TileBuilders.Tile> {
        return CallbackToFutureAdapter.getFuture { completer: CallbackToFutureAdapter.Completer<TileBuilders.Tile> ->
            val job = serviceScope.launch {
                try {
                    val ds = repository.getTideData().firstOrNull()
                    val tide = ds?.data ?: placeholderTide()
                    val currentHeartRate = heartRateMonitor.latestHeartRate.value

                    val content = wrapClickable(
                        layout(
                            locationName = tide.locationName,
                            mul = tide.mul,
                            events = tide.events,
                            currentHeartRate = currentHeartRate
                        )
                    )
                    completer.set(buildTile(content))

                    // 최신 데이터 요청(지연 재요청은 서비스 트리거에 맡김)
                    runCatching { repository.requestDataFromServer("/request/refresh_all_data") }
                } catch (e: Exception) {
                    Log.e(TAG, "Tile build error", e)
                    val fallback = Text.Builder().setText("데이터 없음").build()
                    val entry = TimelineBuilders.TimelineEntry.Builder()
                        .setLayout(Layout.Builder().setRoot(fallback).build())
                        .build()
                    val timeline = TimelineBuilders.Timeline.Builder()
                        .addTimelineEntry(entry)
                        .build()
                    val tile = TileBuilders.Tile.Builder()
                        .setResourcesVersion(RESOURCES_VERSION)
                        .setTimeline(timeline)
                        .build()
                    completer.set(tile)
                }
            }
            completer.addCancellationListener({ job.cancel() }, MoreExecutors.directExecutor())
            "tile-request"
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
                                Background.Builder()
                                    .setColor(argb(TileColors.AccentYellow))
                                    .build()
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
                    .setBackground(
                        Background.Builder()
                            .setColor(argb(0x33FFFFFF.toInt()))
                            .build()
                    )
                    .build()
            )
            .build()

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

    private fun placeholderTide(): TideData =
        TideData(
            date = "",
            weekday = "",
            locationName = "로딩중",
            mul = "",
            lunar = "",
            sunrise = "",
            sunset = "",
            moonrise = "",
            moonset = "",
            events = listOf(
                TideEvent(trend = "만조", time = "--:--", levelCm = 0, deltaCm = 0),
                TideEvent(trend = "간조", time = "--:--", levelCm = 0, deltaCm = 0),
                TideEvent(trend = "만조", time = "--:--", levelCm = 0, deltaCm = 0),
                TideEvent(trend = "간조", time = "--:--", levelCm = 0, deltaCm = 0),
            )
        )
}
