package com.eeos.rocatrun.presentation

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import com.eeos.rocatrun.R
import com.eeos.rocatrun.ui.CircularItemGauge
import com.eeos.rocatrun.detector.ArmGestureDetector
import com.eeos.rocatrun.receiver.SensorUpdateReceiver
import com.eeos.rocatrun.sensor.SensorManagerHelper
import com.eeos.rocatrun.service.LocationForegroundService
import com.eeos.rocatrun.util.FormatUtils
import com.eeos.rocatrun.viewmodel.BossHealthRepository
import com.eeos.rocatrun.viewmodel.GameViewModel
import com.eeos.rocatrun.viewmodel.MultiUserScreen
import com.eeos.rocatrun.viewmodel.MultiUserViewModel
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.wearable.Asset
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

class RunningActivity : ComponentActivity(), SensorEventListener {
    private val gameViewModel: GameViewModel by viewModels()
    private val multiUserViewModel: MultiUserViewModel by viewModels()
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private lateinit var sensorManager: SensorManager
    private var formatUtils = FormatUtils()
    private lateinit var sensorManagerHelper: SensorManagerHelper

    // 동작 인식
    private lateinit var armGestureDetector: ArmGestureDetector

    // GPX 관련 변수
    private val locationList = mutableListOf<Location>()
    // 기존의 단순 값 대신 (timestamp, value) 형태로 저장하여 동기화에 활용
    private val heartRateData = mutableListOf<Pair<Long, Int>>()
    private val cadenceData = mutableListOf<Pair<Long, Double>>()
    // 기존 paceList는 그대로 사용하되, 필요하면 timestamp와 함께 저장하는 방식으로 개선 가능
    private val paceList = mutableListOf<Double>()

    // 상태 변수들
    private var totalDistance by mutableDoubleStateOf(0.0)
    private var speed by mutableDoubleStateOf(0.0)

    private var elapsedTime by mutableLongStateOf(0L)
    private var averagePace by mutableDoubleStateOf(0.0)
    private var heartRate by mutableStateOf("---")
    private var averageHeartRate by mutableDoubleStateOf(0.0)
    private var heartRateSum = 0
    private var heartRateCount = 0
    private var averageCadence = 0.0
    // 걸음 센서 관련 변수
    private var initialStepCount: Int = 0
    private var lastStepCount: Int = 0
    private var lastStepTimestamp = 0L
    private var stepCount = 0  // 누적 걸음 수

    // 보폭
    private val defaultStride = 0.75

    // 절전 모드
    private lateinit var wakeLock: PowerManager.WakeLock

    private var startTime = 0L
    private var isRunning = false
    private var lastLocation: Location? = null

    private val handler = Handler(Looper.getMainLooper())

    // LaunchedEffect에서 데이터 측정 함수 실행을 위한 변수
    private var startTrackingRequested by mutableStateOf(false)

    private val updateRunnable = object : Runnable {
        override fun run() {
            if (isRunning) {
                elapsedTime = System.currentTimeMillis() - startTime
                averagePace = if (totalDistance > 0) {
                    val paceInSeconds = elapsedTime / 1000.0 / totalDistance
                    paceInSeconds / 60
                } else 0.0

                // 데이터 전송 (아이템 사용 여부에 따라 분기)
                val itemUsed = gameViewModel.itemUsedSignal.value
                Log.d("itemUsedCheck", "체크 : $itemUsed")
                if (itemUsed) {
                    sendDataToPhone(itemUsed = true)
                } else {
                    sendDataToPhone()
                }

                handler.postDelayed(this, 1000)
            }
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        armGestureDetector = ArmGestureDetector(
            context = this,
            onArmSwing = {
                gameViewModel.notifyItemUsage(this)
                Log.d("ArmDectector", "팔 휘두르기 감지")
            }

        )
        armGestureDetector.start()
        sensorManagerHelper = SensorManagerHelper(
            context = this,
            onHeartRateUpdated = { newHeartRate ->
                Log.d("심박수", "심박수: $newHeartRate")
                if (newHeartRate > 0) {
                    heartRate = newHeartRate.toString()
                    heartRateSum += newHeartRate
                    heartRateCount++
                    // 센서 이벤트 발생 시의 현재 시간으로 저장
                    heartRateData.add(Pair(System.currentTimeMillis(), newHeartRate))
                Log.d("RunningActivity", "MHeart rate updated: $heartRate")
                }
            },
            onStepDetected = {
                val currentTime = System.currentTimeMillis()
                val stepsDelta = 1

                if (lastStepTimestamp > 0) {
                    val timeDelta = currentTime - lastStepTimestamp
                    totalElapsedTime += timeDelta // 총 경과 시간 업데이트
                    updateCadence(stepsDelta, timeDelta)
                }

                stepCount += stepsDelta // 누적 걸음 수 업데이트
                lastStepTimestamp = currentTime // 마지막 걸음 시간 갱신

                Log.d("StepCounter", "MTotal steps: $stepCount")
            }
        )




        setContent {
            val gameViewModel: GameViewModel by viewModels()
            val multiUserViewModel: MultiUserViewModel by viewModels()
            RunningApp(gameViewModel, multiUserViewModel)
        }
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        observeStartTrackingState()

        // 절전 모드 방지를 위한 WakeLock 설정
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "RunningApp::Wakelock")
        wakeLock.acquire(10 * 60 * 1000L)

        // 포그라운드 서비스 시작
        startForegroundService()
        scheduleSensorUpdates()

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                for (location in locationResult.locations) {
                    if (location.accuracy < 15) {
                        updateLocation(location)
                    } else {
                        Log.d("GPS", "정확도 부족: ${location.accuracy}m, 해당 데이터 무시")
                    }
                }
            }
        }




//        requestPermissions()

        // 게임 종료 이벤트 구독: RunningActivity의 lifecycleScope를 사용하여 구독
        lifecycleScope.launch {
            multiUserViewModel.gameEndEventFlow.collect { gameEnded ->
                if (gameEnded) {
                    // 게임 종료 시 센서 및 위치 업데이트 등 정리 작업 수행
                    gameViewModel.stopFeverTimeEffects()
                    delay(300)
                    stopTracking()
                    // 결과 화면(ResultActivity)으로 전환
                    navigateToResultActivity(this@RunningActivity)
                    Log.d("RunningActivity", "게임 종료 이벤트 수신, 결과 화면으로 전환")
                }
            }
        }

        // 네트워크 에러 이벤트 구독
        lifecycleScope.launch {
            multiUserViewModel.networkErrorEventFlow.collect { networkError ->
                if (networkError) {
                    stopTrackingAndShowStats()
                    navigateToNetworkErrorScreen(this@RunningActivity)
                    Log.d("RunningActivity", "네트워크 에러 이벤트 수신, 네트워크 에러 화면으로 전환")
                }
            }
        }

    }

    // 네트워크 에러 화면으로 전환하는 함수
    private fun navigateToNetworkErrorScreen(context: Context) {
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        val vibrationEffect = VibrationEffect.createWaveform(longArrayOf(3000), intArrayOf(100), -1)
        vibrator?.vibrate(vibrationEffect)
        val intent = Intent(context, NetworkErrorActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        context.startActivity(intent)
    }


    // 결과 화면으로 전환하는 함수
    private fun navigateToResultActivity(context: Context) {
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        val vibrationEffect = VibrationEffect.createWaveform(longArrayOf(3000), intArrayOf(100), -1)
        vibrator?.vibrate(vibrationEffect)
        val intent = Intent(context, ResultActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        context.startActivity(intent)
    }

    private fun observeStartTrackingState() {
        handler.post(object : Runnable {
            override fun run() {
                if (startTrackingRequested && !isRunning) {
                    startTracking()
                    startTrackingRequested = false
                }
                handler.postDelayed(this, 500)
            }
        })
    }

    // 포그라운드 서비스 시작
    private fun startForegroundService() {
        val serviceIntent = Intent(this, LocationForegroundService::class.java)
        startForegroundService(serviceIntent)
    }

    override fun onResume() {
        super.onResume()
        sensorManagerHelper.registerSensors()
    }

    override fun onPause() {
        super.onPause()
        if (wakeLock.isHeld) {
            Log.e("WakeLock", "WakeLock 확인")
            wakeLock.release()
        }
        sensorManagerHelper.unregisterSensors()
    }

    override fun onDestroy() {
        super.onDestroy()
        sensorManagerHelper.unregisterSensors()
        armGestureDetector.stop()
        finishAndRemoveTask()
    }



    // 걸음 수랑 보폭으로 추정 거리 계산하는 함수
    private fun computeDistanceFromSteps(stepCount: Int, strideLength: Double = defaultStride): Double {
        return stepCount * strideLength
    }




    private var lastDistanceUpdate = 0.0  // 마지막 게이지 업데이트 시점
    private var segmentDistance = 0.0 // 아이템 게이지 채우기 위한 이동 거리
    // 위치 업데이트 후 거리 계산
    private fun updateLocation(location: Location) {
        lastLocation?.let {
            val distanceMoved = it.distanceTo(location) / 1000  // km 단위
            if (distanceMoved > 0.002) {  // 2m 이상 이동한 경우만 반영
                totalDistance += distanceMoved
                speed = location.speed * 3.6


                segmentDistance += distanceMoved

                val multiplier = if (gameViewModel.feverTimeActive.value) 3 else 1
                val threshold = 0.1 / multiplier
                // 현재 거리 진행률 계산
                if (segmentDistance >= threshold) {
                    // 100m 이상 이동
                    gameViewModel.handleGaugeFull(this)
                    segmentDistance -= threshold
                } else {
                    // 현재 거리 비율(0~100) 계산
                    val gaugePercentage = ((segmentDistance / threshold) * 100).toInt()
                    gameViewModel.setItemGauge(gaugePercentage)
                }
            }
        }
        lastLocation = location
        locationList.add(location)
        paceList.add(averagePace)
    }

    // 운동 시작
    private fun startTracking() {
        if (isRunning) return
        resetTrackingData()
        cadenceData.clear()
        heartRateData.clear()

        isRunning = true
        startTime = System.currentTimeMillis()
        handler.post(updateRunnable)

        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY, 1000
        )
            .setMinUpdateIntervalMillis(500)
            .setMinUpdateDistanceMeters(2.0f)
            .build()

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
        }
    }

    private fun resetTrackingData() {
        totalDistance = 0.0
        elapsedTime = 0L
        averagePace = 0.0
        speed = 0.0
        heartRateSum = 0
        heartRateCount = 0
        averageHeartRate = 0.0
        heartRate = "--"
        lastLocation = null
        initialStepCount = 0
        lastStepCount = 0
        lastStepTimestamp = 0L
        stepCount = 0

        // 새 운동 시작 시 이전 데이터 초기화
        segmentDistance = 0.0

        locationList.clear()
        heartRateData.clear()
        cadenceData.clear()
        paceList.clear()

    }

    private fun stopTracking() {
        isRunning = false
        handler.removeCallbacks(updateRunnable)
        stopService(Intent(this, LocationForegroundService::class.java))
        fusedLocationClient.removeLocationUpdates(locationCallback)

        val totalItemUsage = gameViewModel.totalItemUsageCount.value

        if (heartRateCount > 0) {
            averageHeartRate = heartRateSum.toDouble() / heartRateCount
        }
        averageCadence=calculateAverageCadence()

        Log.d("FinalStats", "Average Cadence: $averageCadence steps/min")
        sensorManager.unregisterListener(this)

        Log.d("Stats",
            "Elapsed Time: ${formatUtils.formatTime(elapsedTime)}, Distance: $totalDistance km, Avg Pace: $averagePace min/km, Avg Heart Rate: ${"%.1f".format(averageHeartRate)} bpm")

        sendFinalResultToPhone(totalItemUsage)
        createAndSendGpxFile()
        finishAndRemoveTask()


    }

    // 최종 결과를 모바일로 전송
    private fun sendFinalResultToPhone(totalItemUsage: Int) {
        val dataMapRequest = PutDataMapRequest.create("/final_result_data").apply {
            dataMap.putDouble("distance", totalDistance)
            dataMap.putLong("time", elapsedTime)
            dataMap.putDouble("averagePace", averagePace)
            dataMap.putDouble("averageHeartRate", averageHeartRate)
            dataMap.putInt("totalItemUsage", totalItemUsage)
            dataMap.putDouble("averageCadence", averageCadence)
        }.asPutDataRequest().setUrgent()
        Log.d("RunningActivity", "케이던스: $averageCadence, $stepCount")
        Log.d("Final Data 전송", "총 아이템 사용 횟수: $totalItemUsage")
        Wearable.getDataClient(this).putDataItem(dataMapRequest)
            .addOnSuccessListener { Log.d("RunningActivity", "Final result data sent successfully") }
            .addOnFailureListener { e -> Log.e("RunningActivity", "Failed to send final result data", e) }
    }

    // GPX 파일 생성 및 전송
    private fun createAndSendGpxFile() {
        val gpxString = createGpxString()
        val gpxBytes = gpxString.toByteArray(Charsets.UTF_8)
        val asset = Asset.createFromBytes(gpxBytes)

        val putDataMapReq = PutDataMapRequest.create("/gpx_data").apply {
            dataMap.putAsset("gpx_file", asset)
            dataMap.putLong("timestamp", System.currentTimeMillis())
        }

        val putDataReq = putDataMapReq.asPutDataRequest().setUrgent()
        Wearable.getDataClient(this).putDataItem(putDataReq)
            .addOnSuccessListener { Log.d("RunningActivity", "GPX data sent successfully") }
            .addOnFailureListener { e -> Log.e("RunningActivity", "Failed to send GPX data", e) }
    }

    // 헬퍼 함수: 특정 timestamp 이하의 마지막 센서 값을 찾는다.
    private fun getNearestSensorValue(data: List<Pair<Long, Int>>, targetTime: Long, default: Int): Int {
        var result = default
        for ((time, value) in data) {
            if (time <= targetTime) {
                result = value
            } else {
                break
            }
        }
        return result
    }

    private fun getNearestSensorValueDouble(data: MutableList<Pair<Long, Double>>, targetTime: Long, default: Double): Double {
        var result = default
        for ((time, value) in data) {
            if (time <= targetTime) {
                result = value
            } else {
                break
            }
        }
        return result
    }

    // GPX 파일 생성 (위치 데이터를 기준으로, 각 포인트에 가장 가까운 센서 데이터를 매칭)
    private fun createGpxString(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.KOREA)
        sdf.timeZone = TimeZone.getTimeZone("Asia/Seoul")

        val gpxBuilder = StringBuilder()
        gpxBuilder.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
        gpxBuilder.append("<gpx version=\"1.1\" creator=\"RocatRun Wear App\">\n")
        gpxBuilder.append("  <trk>\n")
        gpxBuilder.append("    <name>RocatRun Activity</name>\n")
        gpxBuilder.append("    <trkseg>\n")

        // 각 위치에 대해, 해당 위치의 timestamp와 가장 근접한 심박수와 케이던스 값을 찾는다.
        for (location in locationList) {
            val locTime = location.time
            val hr = getNearestSensorValue(heartRateData, locTime, 0)
            val cad = getNearestSensorValueDouble(cadenceData, locTime, 0.0)
            // pace는 paceList의 마지막 값(또는 0.0)으로 처리 (필요시 별도 센서 데이터 저장 방식 적용)
            val pace = if (paceList.isNotEmpty()) paceList.last() else 0.0

            gpxBuilder.append("      <trkpt lat=\"${location.latitude}\" lon=\"${location.longitude}\">\n")
            gpxBuilder.append("        <ele>${location.altitude}</ele>\n")
            gpxBuilder.append("        <extensions>\n")
            gpxBuilder.append("          <gpxtpx:TrackPointExtension>\n")
            gpxBuilder.append("            <gpxtpx:hr>$hr</gpxtpx:hr>\n")
            gpxBuilder.append("            <gpxtpx:pace>$pace</gpxtpx:pace>\n")
            gpxBuilder.append("            <gpxtpx:cad>$cad</gpxtpx:cad>\n")
            gpxBuilder.append("          </gpxtpx:TrackPointExtension>\n")
            gpxBuilder.append("        </extensions>\n")
            gpxBuilder.append("        <time>${sdf.format(Date(locTime))}</time>\n")
            gpxBuilder.append("      </trkpt>\n")
        }

        gpxBuilder.append("    </trkseg>\n")
        gpxBuilder.append("  </trk>\n")
        gpxBuilder.append("</gpx>")
        return gpxBuilder.toString()
    }

    @Composable
    fun RunningApp(gameViewModel: GameViewModel, multiUserViewModel: MultiUserViewModel) {
        val activity = LocalContext.current as? RunningActivity
        var isCountdownFinished by remember { mutableStateOf(false) }


        if (isCountdownFinished) {
            WatchAppUI(gameViewModel, multiUserViewModel)
        } else {
            CountdownScreen(onFinish = {
                isCountdownFinished = true
                activity?.startTrackingRequested = true
            })
        }
    }



    @Composable
    fun WatchAppUI(gameViewModel: GameViewModel, multiUserViewModel: MultiUserViewModel) {
        val pagerState = rememberPagerState(pageCount = { 4 })
        HorizontalPager(state = pagerState) { page ->
                when (page) {
                    0 -> CircularLayout(gameViewModel)
                    1 -> Box(modifier = Modifier.fillMaxSize()) {
                        GameScreen(gameViewModel, multiUserViewModel)
                    }
                    2 -> Box(modifier = Modifier.fillMaxSize()) {
                        MultiUserScreen(multiUserViewModel,gameViewModel)
                    }
                    3 -> ControlButtons()
                }
            }
    }

    @Composable
    fun CircularLayout(gameViewModel: GameViewModel) {
        val itemGaugeValue by gameViewModel.itemGaugeValue.collectAsState()
        val bossGaugeValue by gameViewModel.bossGaugeValue.collectAsState()
        // BossHealthRepository의 최대 체력 구독 (최초 값이 0이라면 기본값 10000 사용)
        val maxBossHealth by BossHealthRepository.maxBossHealth.collectAsState()
        val effectiveMaxBossHealth = if (maxBossHealth == 0) 10000 else maxBossHealth
        val maxGaugeValue = 100
        val isFeverTime by gameViewModel.feverTimeActive.collectAsState()

        val itemProgress by animateFloatAsState(
            targetValue = itemGaugeValue.toFloat() / maxGaugeValue,
            animationSpec = tween(durationMillis = 500)
        )
        val bossProgress by animateFloatAsState(
            targetValue = bossGaugeValue.toFloat() / effectiveMaxBossHealth,
            animationSpec = tween(durationMillis = 500)
        )

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            val spacing = maxWidth * 0.04f
            CircularItemGauge(itemProgress = itemProgress, bossProgress = bossProgress, Modifier.size(200.dp), isFeverTime)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "페이스",
                        color = Color(0xFF00FFCC),
                        fontSize = 12.sp,
                        fontFamily = FontFamily(Font(R.font.neodgm))
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = formatUtils.formatPace(averagePace),
                        color = Color(0xFFFFFFFF),
                        fontSize = 25.sp,
                        fontFamily = FontFamily(Font(R.font.neodgm))
                    )
                }
                Spacer(modifier = Modifier.height(spacing))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = formatUtils.formatTime(elapsedTime),
                        color = Color.White,
                        fontSize = 40.sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = FontFamily(Font(R.font.neodgm))
                    )
                }
                Spacer(modifier = Modifier.height(spacing * 1f))
                Row(
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "거리",
                            color = Color(0xFF36DBEB),
                            fontSize = 12.sp,
                            fontFamily = FontFamily(Font(R.font.neodgm))
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = buildAnnotatedString {
                                withStyle(style = SpanStyle(color = Color.White, fontSize = 20.sp)) {
                                    append("%.2f".format(totalDistance))
                                }
                                withStyle(style = SpanStyle(color = Color.White, fontSize = 16.sp)) {
                                    append("km")
                                }
                            },
                            fontFamily = FontFamily(Font(R.font.neodgm)),
                            textAlign = TextAlign.Center
                        )
                    }
                    Spacer(Modifier.width(6.dp))
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "심박수",
                            color = Color(0xFFF20089),
                            fontSize = 12.sp,
                            fontFamily = FontFamily(Font(R.font.neodgm))
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = buildAnnotatedString {
                                withStyle(style = SpanStyle(color = Color.White, fontSize = 20.sp)) {
                                    append(heartRate)
                                }
                                withStyle(style = SpanStyle(color = Color.White, fontSize = 16.sp)) {
                                    append("bpm")
                                }
                            },
                            fontFamily = FontFamily(Font(R.font.neodgm)),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }

    @Composable
    fun ControlButtons() {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(onClick = {
                stopTrackingAndShowStats()

            },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF00FFCC)
                )) {
                Text("종료",
                    color = Color.Black,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily(Font(R.font.neodgm)))
            }
        }
    }

    private fun stopTrackingAndShowStats() {
        gameViewModel.stopFeverTimeEffects()
        isRunning = false
        resetTrackingData()
        stopService(Intent(this, LocationForegroundService::class.java))
        finish()
        finishAndRemoveTask()
        handler.removeCallbacks(updateRunnable)
        fusedLocationClient.removeLocationUpdates(locationCallback)

        // 센서 해제
        sensorManagerHelper.unregisterSensors()
        sensorManager.unregisterListener(this)

        Log.d("RunningActivity", "Tracking stopped, sensors unregistered")
    }

    // 폰에 데이터 전송
    private fun sendDataToPhone(itemUsed: Boolean = false) {
        if (itemUsed) {
            val itemUsedRequest = PutDataMapRequest.create("/use_item").apply {
                dataMap.putBoolean("itemUsed", true)
                dataMap.putLong("timestamp", System.currentTimeMillis())
            }.asPutDataRequest().setUrgent()
            Wearable.getDataClient(this).putDataItem(itemUsedRequest)
                .addOnSuccessListener { Log.d("RunningActivity", "아이템 사용 신호 성공적으로 보냄: $itemUsed") }
                .addOnFailureListener { e -> Log.e("RunningActivity", "아이템 사용 신호 보내지 못하였음", e) }
        } else {
            val dataMapRequest = PutDataMapRequest.create("/running_data").apply {
                dataMap.putDouble("pace", averagePace)
                dataMap.putDouble("distance", totalDistance)
                dataMap.putLong("time", elapsedTime)
                dataMap.putString("heartRate", heartRate)
                dataMap.putLong("timestamp", System.currentTimeMillis())
            }.asPutDataRequest()
            Log.d("데이터 전송 함수", "데이터 형태 - Pace : $averagePace distance : $totalDistance, time : $elapsedTime, heartRate: $heartRate")
            dataMapRequest.setUrgent()
            Wearable.getDataClient(this).putDataItem(dataMapRequest)
                .addOnSuccessListener { Log.d("RunningActivity", "Data sent successfully") }
                .addOnFailureListener { e -> Log.e("RunningActivity", "Failed to send data", e) }
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        when (event?.sensor?.type) {
            Sensor.TYPE_HEART_RATE -> {
                val newHeartRate = event.values[0].toInt()
                Log.d("심박수", "심박수: $newHeartRate")
                if (newHeartRate > 0) {
                    heartRate = newHeartRate.toString()
                    heartRateSum += newHeartRate
                    heartRateCount++
                    // 센서 이벤트 발생 시의 현재 시간으로 저장
                    heartRateData.add(Pair(System.currentTimeMillis(), newHeartRate))
                    Log.d("추가", "심박수 추가")
                }
            }

            Sensor.TYPE_STEP_DETECTOR -> {

                val currentTime = System.currentTimeMillis()
                val stepsDelta = event.values[0].toInt() // 항상 1로 반환됨

                if (lastStepTimestamp > 0) {
                    val timeDelta = currentTime - lastStepTimestamp
                    totalElapsedTime += timeDelta // 총 경과 시간 업데이트
                    updateCadence(stepsDelta, timeDelta)
                }

                stepCount += stepsDelta // 누적 걸음 수 업데이트
                lastStepTimestamp = currentTime // 마지막 걸음 시간 갱신

                Log.d("StepCounter", "Changed Total steps: $stepCount")
            }
        }
    }

    // 케이던스 업데이트: 분당 걸음수 = (걸음 증가량) / (시간 간격(분))
    private fun updateCadence(stepsDelta: Int, timeDelta: Long) {
        if (timeDelta > 0) {
            // 분 단위로 변환하여 케이던스 계산
            val cadence = (stepsDelta.toFloat() / (timeDelta / 60000f)).let {
                (it * 10).roundToInt() / 10.0 // 소수점 첫째 자리까지 반올림
            }
            cadenceData.add(Pair(System.currentTimeMillis(), cadence))
            Log.d("StepCounter", "Current cadence: $cadence steps/min")
        }
    }

    // 평균 케이던스 계산
    private fun calculateAverageCadence(): Double {
        return if (stepCount > 0 && totalElapsedTime > 0) {
            // 총 걸음 수와 총 경과 시간을 기반으로 평균 케이던스 계산
            val averageCadence = stepCount / (totalElapsedTime / 60000f)
            (averageCadence * 10).roundToInt() / 10.0 // 소수점 첫째 자리까지 반올림
        } else {
            0.0
        }
    }

    // 총 경과 시간 추적 변수
    private var totalElapsedTime: Long = 0L

    // 알람 설정 (센서 업데이트용)
    private fun scheduleSensorUpdates() {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, SensorUpdateReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        alarmManager.setRepeating(
            AlarmManager.ELAPSED_REALTIME_WAKEUP,
            SystemClock.elapsedRealtime() + 60000,
            60000,
            pendingIntent
        )
        Log.d("AlarmManager", "Alarm scheduled for sensor updates.")
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
