package com.example.dive.sensors.casting

import android.content.Context
import android.hardware.*
import android.os.*
import kotlin.math.*

class CastingAnalyzer(private val context: Context) : SensorEventListener {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accel = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val gyro = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
    private val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

    // Phase 상태 정의
    private enum class Phase { IDLE, BACKSWING, TRANSITION, FORWARD, RELEASE, DONE }
    private var phase = Phase.IDLE
    private var lastPhaseTime = 0L

    // 데이터 버퍼
    private val accelData = mutableListOf<FloatArray>()
    private val gyroData = mutableListOf<FloatArray>()
    private val timestamps = mutableListOf<Long>()

    private var callback: ((CastingResult) -> Unit)? = null

    fun startCasting(onResult: (CastingResult) -> Unit) {
        callback = onResult
        accelData.clear(); gyroData.clear(); timestamps.clear()
        phase = Phase.IDLE
        lastPhaseTime = System.currentTimeMillis()

        // 센서 등록
        sensorManager.registerListener(this, accel, SensorManager.SENSOR_DELAY_GAME)
        sensorManager.registerListener(this, gyro, SensorManager.SENSOR_DELAY_GAME)
    }

    private fun stopAndAnalyze() {
        sensorManager.unregisterListener(this)
        val result = analyzeData()
        callback?.invoke(result)
    }

    override fun onSensorChanged(event: SensorEvent) {
        val now = System.currentTimeMillis()

        if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            accelData.add(event.values.clone())
            timestamps.add(now)
            detectCastingPhase(event.values, now)
        } else if (event.sensor.type == Sensor.TYPE_GYROSCOPE) {
            gyroData.add(event.values.clone())
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    // ---------------- Phase 기반 캐스팅 감지 ----------------
    private fun detectCastingPhase(accel: FloatArray, now: Long) {
        val z = accel[2] // 손목 방향 축 (워치 착용 방향에 따라 조정 필요)

        when (phase) {
            Phase.IDLE -> {
                if (z < -5f) { // 강하게 뒤로 젖힘
                    phase = Phase.BACKSWING
                    lastPhaseTime = now
                }
            }
            Phase.BACKSWING -> {
                if (abs(z) < 2f && now - lastPhaseTime > 150) {
                    // 뒤로 당겼다가 잠깐 멈춤
                    phase = Phase.TRANSITION
                    lastPhaseTime = now
                }
            }
            Phase.TRANSITION -> {
                if (z > 5f) { // 앞으로 강하게 휘두름
                    phase = Phase.FORWARD
                    lastPhaseTime = now
                    vibrate(100) // 릴리즈 알림
                }
            }
            Phase.FORWARD -> {
                if (now - lastPhaseTime > 400) {
                    phase = Phase.RELEASE
                    stopAndAnalyze()
                }
            }
            else -> {}
        }
    }

    // ---------------- 데이터 분석 ----------------
    private fun analyzeData(): CastingResult {
        if (accelData.isEmpty() || gyroData.isEmpty()) {
            return CastingResult(0.0, 1000, 0.0, 0.0, 0, "데이터 부족")
        }

        // 1) 속도 계산 (최대 가속도 → 속도 근사)
        val peakAccel = accelData.maxOf {
            sqrt(
                it[0].toDouble().pow(2.0) +
                        it[1].toDouble().pow(2.0) +
                        it[2].toDouble().pow(2.0)
            )
        }
        val velocity = peakAccel * 0.3

        // 2) 릴리즈 타이밍 (피크까지 걸린 시간)
        val peakIndex = accelData.indexOfFirst {
            sqrt(
                it[0].toDouble().pow(2.0) +
                        it[1].toDouble().pow(2.0) +
                        it[2].toDouble().pow(2.0)
            ) == peakAccel
        }
        val timingError = if (peakIndex > 0) {
            abs((timestamps[peakIndex] - timestamps[0]) - 1500) // 1.5초 기준
        } else 1000L

        // 3) 평면 안정성
        val planeStability = calculatePlaneStability()

        // 4) 부드러움
        val smoothness = calculateSmoothness()

        // 5) 점수 & 코칭
        val totalScore = calculateTotalScore(velocity, timingError, planeStability, smoothness)
        val coaching = getCoachingMessage(velocity, timingError, planeStability, smoothness, totalScore)

        return CastingResult(velocity, timingError, planeStability, smoothness, totalScore, coaching)
    }

    private fun calculatePlaneStability(): Double {
        if (gyroData.isEmpty()) return 0.0
        val yRMS = sqrt(gyroData.map { it[1].toDouble().pow(2.0) }.average())
        val zRMS = sqrt(gyroData.map { it[2].toDouble().pow(2.0) }.average())
        val xPeak = gyroData.maxOf { abs(it[0].toDouble()) }
        return if (xPeak == 0.0) 100.0 else (sqrt((yRMS * yRMS + zRMS * zRMS) / 2.0) / xPeak * 100.0)
    }

    private fun calculateSmoothness(): Double {
        if (gyroData.size < 3) return 1.0
        val jerks = mutableListOf<Double>()
        for (i in 1 until gyroData.size - 1) {
            val prev = gyroData[i - 1]; val curr = gyroData[i]; val next = gyroData[i + 1]
            val jerk = sqrt(
                (next[0].toDouble() - 2 * curr[0].toDouble() + prev[0].toDouble()).pow(2.0) +
                        (next[1].toDouble() - 2 * curr[1].toDouble() + prev[1].toDouble()).pow(2.0) +
                        (next[2].toDouble() - 2 * curr[2].toDouble() + prev[2].toDouble()).pow(2.0)
            )
            jerks.add(jerk)
        }
        return jerks.average()
    }

    private fun calculateTotalScore(v: Double, tErr: Long, plane: Double, smooth: Double): Int {
        // 속도 점수 (40)
        val speedScore = when {
            v < 6.0 -> (v / 6.0 * 20).toInt()                     // 0~6 m/s → 0~20점
            v <= 10.0 -> 20 + ((v - 6.0) / 4.0 * 15).toInt()      // 6~10 m/s → 20~35점 (스위트 스팟)
            v <= 14.0 -> {
                val potential = 35 + ((v - 10.0) / 4.0 * 5).toInt() // 10~14 m/s → 최대 40점
                if (tErr <= 70 && (plane <= 35.0 || smooth <= 0.25)) potential else 35
            }
            else -> maxOf(10, 35 - ((v - 14.0) * 5).toInt())       // 14 m/s 초과 → 페널티 (천천히 줄어듦)
        }
        val timingScore = when {
            tErr <= 30 -> 36
            tErr <= 70 -> 26
            tErr <= 100 -> 10
            else -> 0
        }
        val planeScore = if (plane <= 35.0) 10 else 0
        val smoothScore = if (smooth <= 0.25) 10 else 0

        return (speedScore + timingScore + planeScore + smoothScore).coerceIn(0, 100)
    }

    private fun getCoachingMessage(v: Double, tErr: Long, plane: Double, smooth: Double, score: Int): String {
        return when {
            v < 6.0 -> "더 힘차게! 팔꿈치를 활용해 속도를 높여보세요"
            v in 6.0..9.0 -> "좋은 속도입니다! 이 템포를 유지하세요"
            v in 9.0..12.0 && tErr <= 70 -> "고속 구간! 정확도 유지 중 - 훌륭합니다 ⭐"
            v in 9.0..12.0 && tErr > 70 -> "속도↑ 정확도↓ - 조금 더 천천히, 안정적으로!"
            v > 12.0 -> "과속! 힘을 빼고 부드럽게 휘두르세요"
            score >= 70 -> "훌륭한 캐스팅! ⭐"
            else -> "좋은 템포입니다. 안정성을 조금 더 챙겨보세요"
        }
    }

    private fun vibrate(ms: Long) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            vibrator.vibrate(ms)
        }
    }
}
