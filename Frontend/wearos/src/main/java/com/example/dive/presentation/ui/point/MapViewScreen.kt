package com.example.dive.presentation.ui.point

import android.content.Context
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.OvalShape
import android.view.MotionEvent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.wear.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.LocationSearching
import androidx.compose.ui.text.style.TextAlign
import com.example.dive.presentation.FishingPointsUiMode
import com.example.dive.presentation.MainViewModel
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

@Composable
fun MapViewScreen(
    mapState: FishingPointsUiMode.MapView,
    viewModel: MainViewModel
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val map = remember { MapView(context) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> map.onResume()
                Lifecycle.Event.ON_PAUSE -> map.onPause()
                Lifecycle.Event.ON_DESTROY -> map.onDetach()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    SwipeToDismissBox(
        onDismissed = { viewModel.returnToList() }
    ) {
        isBackground ->
        if (isBackground) {
            Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colors.background))
        } else {
            Box(modifier = Modifier.fillMaxSize()) {
                AndroidView(
                    factory = { ctx ->
                        Configuration.getInstance().load(ctx, ctx.getSharedPreferences("osmdroid", Context.MODE_PRIVATE))
                        map.apply {
                            setTileSource(TileSourceFactory.MAPNIK)
                            setMultiTouchControls(false) // 멀티터치 제어 비활성화
                            setBuiltInZoomControls(false) // 내장 줌 컨트롤 비활성화

                            // 마커 추가
                            val point = mapState.detail.point
                            val fishingPoint = GeoPoint(point.lat, point.lon)

                            // 낚시 포인트 마커 (빨간색)
                            val fishingMarker = Marker(this).apply {
                                position = fishingPoint
                                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                                title = point.pointName
                                icon = createMarkerDrawable(Color.Red.toArgb(), ctx) // 빨간색 마커
                            }
                            overlays.add(fishingMarker)

                            var initialCenterPoint = fishingPoint
                            var initialZoom = 15.0

                            // 내 위치 마커 (파란색)
                            mapState.detail.phoneLocation?.let { phoneLoc ->
                                if (phoneLoc.latitude != 0.0 && phoneLoc.longitude != 0.0) {
                                    val phoneGeoPoint = GeoPoint(phoneLoc.latitude, phoneLoc.longitude)
                                    val phoneMarker = Marker(this).apply {
                                        position = phoneGeoPoint
                                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                                        title = "내 위치"
                                        icon = createMarkerDrawable(Color.Blue.toArgb(), ctx) // 파란색 마커
                                    }
                                    overlays.add(phoneMarker)

                                    // 낚시 포인트와 내 위치의 중간 지점 계산
                                    val midLat = (fishingPoint.latitude + phoneGeoPoint.latitude) / 2
                                    val midLon = (fishingPoint.longitude + phoneGeoPoint.longitude) / 2
                                    initialCenterPoint = GeoPoint(midLat, midLon)
                                    // 거리에 따른 동적 줌 레벨 계산
                                    initialZoom = getZoomLevelForDistance(point.pointDtKm)
                                }
                            }

                            controller.setCenter(initialCenterPoint) // 계산된 중간 지점으로 중심 설정
                            controller.setZoom(initialZoom) // 초기 줌 레벨 설정
                        }
                    },
                    update = { view ->
                        // 지도 업데이트 로직 필요시 구현
                    },
                    modifier = Modifier.fillMaxSize() // 화면 꽉 채우기
                )

                // 상단 정보 영역 (지도 위에 오버레이)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter),
                    onClick = { },
                    contentColor = MaterialTheme.colors.surface.copy(alpha = 0.8f)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        val point = mapState.detail.point
                        Text(text = point.pointName, style = MaterialTheme.typography.title3, softWrap = true, textAlign = TextAlign.Center, color = MaterialTheme.colors.onSurface)
                        Text(
                            text = "${point.depth.minM}~${point.depth.maxM}m · ${point.material} · ${point.targetByFish.keys.firstOrNull() ?: ""}",
                            style = MaterialTheme.typography.body2, softWrap = true, textAlign = TextAlign.Center, color = MaterialTheme.colors.onSurface
                        )
                    }
                }

                // 지도 액션 버튼 (지도 위에 오버레이)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter) // 하단 중앙 정렬
                        .padding(bottom = 18.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val point = mapState.detail.point
                    val fishingPoint = GeoPoint(point.lat, point.lon)

                    Button(onClick = { map.controller.zoomIn() }, modifier = Modifier.size(30.dp)) {
                        Icon(Icons.Default.Add, contentDescription = "확대")
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Button(onClick = { map.controller.zoomOut() }, modifier = Modifier.size(30.dp)) {
                        Icon(Icons.Default.Remove, contentDescription = "축소")
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Button(onClick = {
                        val point = mapState.detail.point
                        val fishingPoint = GeoPoint(point.lat, point.lon)
                        var targetCenterPoint = fishingPoint

                        mapState.detail.phoneLocation?.let { phoneLoc ->
                            if (phoneLoc.latitude != 0.0 && phoneLoc.longitude != 0.0) {
                                val phoneGeoPoint = GeoPoint(phoneLoc.latitude, phoneLoc.longitude)
                                val midLat = (fishingPoint.latitude + phoneGeoPoint.latitude) / 2
                                val midLon = (fishingPoint.longitude + phoneGeoPoint.longitude) / 2
                                targetCenterPoint = GeoPoint(midLat, midLon)
                            }
                        }
                        map.controller.setCenter(targetCenterPoint)
                        map.controller.setZoom(getZoomLevelForDistance(point.pointDtKm)) // 확대 수준 초기화도 동적으로
                    }, modifier = Modifier.size(30.dp)) { // 위치 초기화 버튼
                        Icon(Icons.Default.LocationSearching, contentDescription = "위치 초기화")
                    }
                }
            }
        }
    }
}

// 거리에 따른 줌 레벨 계산 헬퍼 함수
private fun getZoomLevelForDistance(distanceKm: Double): Double {
    return when {
        distanceKm < 1.0 -> 15.0
        distanceKm < 2.0 -> 14.0
        distanceKm < 4.0 -> 13.0
        else -> 12.0
    }
}

// 마커 Drawable 생성 헬퍼 함수
private fun createMarkerDrawable(color: Int, context: Context): ShapeDrawable {
    return ShapeDrawable(OvalShape()).apply {
        intrinsicHeight = 20
        intrinsicWidth = 20
        paint.color = color
        // TODO: 더 예쁜 아이콘으로 교체 고려
    }
}