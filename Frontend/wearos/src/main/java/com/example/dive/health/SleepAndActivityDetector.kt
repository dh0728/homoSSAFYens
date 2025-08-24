package com.example.dive.health

import com.example.dive.data.model.LocationData
import com.example.dive.presentation.ui.MarineActivityMode
import java.util.Calendar

data class UserState(
    val isSleeping: Boolean,
    val activityLevel: ActivityLevel,
    val marineActivityMode: MarineActivityMode,
    val confidence: Float, // 0.0 ~ 1.0
    val detectionMethod: String
)

enum class ActivityLevel {
    SLEEPING,       // 수면
    RESTING,        // 휴식/정지
    LIGHT_ACTIVITY, // 가벼운 활동 (낚시 등)
    MODERATE,       // 보통 활동
    VIGOROUS        // 격렬한 활동
}

data class HeartRateThresholds(
    val criticalMin: Int,
    val warningMin: Int,
    val normalMin: Int
)

object SleepAndActivityDetector {

    fun detectCurrentUserState(
        marineMode: MarineActivityMode,
        currentHeartRate: Int,
        locationData: LocationData?,
        recentActivityLevel: Int,
        recentHeartRates: List<Int>,
        recentPositionData: List<Float>,
        recentMovementSpeed: Int,
        recentMovementData: List<Float>
    ): UserState {

        val isSleeping = if (marineMode != MarineActivityMode.OFF) {
            detectSleepDuringMarineActivity(
                marineMode, recentActivityLevel, recentHeartRates, recentPositionData, recentMovementData
            )
        } else {
            detectSleepNormalMode(
                recentActivityLevel, recentHeartRates, recentPositionData, recentMovementData
            )
        }

        val activityLevel = determineActivityLevel(isSleeping, recentActivityLevel)
        val confidence = 0.0f // TODO: Calculate confidence based on actual sensor data

        return UserState(
            isSleeping = isSleeping,
            activityLevel = activityLevel,
            marineActivityMode = marineMode,
            confidence = confidence,
            detectionMethod = if (marineMode != MarineActivityMode.OFF) "해양활동모드" else "일반모드"
        )
    }

    private fun determineActivityLevel(isSleeping: Boolean, recentActivityLevel: Int): ActivityLevel {
        return when {
            isSleeping -> ActivityLevel.SLEEPING
            recentActivityLevel > 100 -> ActivityLevel.VIGOROUS
            recentActivityLevel > 30 -> ActivityLevel.MODERATE
            else -> ActivityLevel.RESTING
        }
    }

    private fun detectSleepNormalMode(
        recentActivityLevel: Int,
        recentHeartRates: List<Int>,
        recentPositionData: List<Float>,
        recentMovementData: List<Float>
    ): Boolean {
        val timeBasedSleep = detectSleepByTime()
        val activityBasedSleep = detectSleepByActivity(MarineActivityMode.OFF, recentActivityLevel, recentMovementData)
        val heartRateBasedSleep = detectSleepByHeartRate(recentHeartRates)
        val positionBasedSleep = detectSleepByPosition(recentPositionData)

        val sleepConfidence = (timeBasedSleep * 0.3f + activityBasedSleep * 0.4f + heartRateBasedSleep * 0.3f + positionBasedSleep * 0.2f)
        return sleepConfidence > 0.7f
    }

    private fun detectSleepDuringMarineActivity(
        marineMode: MarineActivityMode,
        recentActivityLevel: Int,
        recentHeartRates: List<Int>,
        recentPositionData: List<Float>,
        recentMovementData: List<Float>
    ): Boolean {
        val activityBasedSleep = detectSleepByActivity(marineMode, recentActivityLevel, recentMovementData)
        val heartRateBasedSleep = detectSleepByHeartRate(recentHeartRates)
        val positionBasedSleep = detectSleepByPosition(recentPositionData)

        val sleepConfidence = when (marineMode) {
            MarineActivityMode.FISHING -> {
                (activityBasedSleep * 0.3f + heartRateBasedSleep * 0.4f + positionBasedSleep * 0.3f)
            }
            MarineActivityMode.BOATING -> {
                (activityBasedSleep * 0.2f + heartRateBasedSleep * 0.5f + positionBasedSleep * 0.3f)
            }
            else -> {
                (activityBasedSleep * 0.4f + heartRateBasedSleep * 0.4f + positionBasedSleep * 0.2f)
            }
        }
        return sleepConfidence > 0.8f
    }

    // Helper functions (now taking parameters)
    private fun detectSleepByTime(): Float {
        val currentTime = Calendar.getInstance()
        val hour = currentTime.get(Calendar.HOUR_OF_DAY)
        return if (hour >= 22 || hour <= 6) 1.0f else 0.0f // Night time
    }

    private fun detectSleepByActivity(marineMode: MarineActivityMode, recentActivityLevel: Int, recentMovementData: List<Float>): Float {
        val movementVariance = calculateMovementVariance(recentMovementData)
        return when (marineMode) {
            MarineActivityMode.FISHING -> {
                when {
                    movementVariance < 2 -> 0.7f
                    movementVariance < 5 -> 0.3f
                    else -> 0.1f
                }
            }
            MarineActivityMode.BOATING -> {
                when {
                    movementVariance < 3 -> 0.8f
                    movementVariance < 8 -> 0.4f
                    else -> 0.1f
                }
            }
            else -> {
                when {
                    movementVariance < 5 -> 0.9f
                    movementVariance < 15 -> 0.6f
                    movementVariance < 30 -> 0.3f
                    else -> 0.1f
                }
            }
        }
    }

    private fun detectSleepByHeartRate(recentHeartRates: List<Int>): Float {
        val isStableHeartRate = recentHeartRates.isNotEmpty() &&
                recentHeartRates.maxOrNull()?.minus(recentHeartRates.minOrNull() ?: 0) ?: 0 < 15
        return if (isStableHeartRate) 0.8f else 0.2f
    }

    private fun detectSleepByPosition(recentPositionData: List<Float>): Float {
        val positionStability = calculatePositionStability(recentPositionData)
        return when {
            positionStability > 0.9f -> 0.8f
            positionStability > 0.7f -> 0.6f
            positionStability > 0.5f -> 0.3f
            else -> 0.1f
        }
    }

    // Dummy implementations for now
    private fun calculatePositionStability(data: List<Float>): Float = 0.7f
    private fun calculateMovementVariance(data: List<Float>): Float = 10f

    fun getHeartRateThresholds(userState: UserState): HeartRateThresholds {
        return when {
            userState.isSleeping -> HeartRateThresholds(
                criticalMin = 35,
                warningMin = 45,
                normalMin = 50
            )
            userState.marineActivityMode == MarineActivityMode.OFF -> HeartRateThresholds(
                criticalMin = 70, //40
                warningMin = 75, //50
                normalMin = 80 //55
            )
            userState.marineActivityMode == MarineActivityMode.FISHING -> HeartRateThresholds(
                criticalMin = 42,
                warningMin = 52,
                normalMin = 60
            )
            userState.activityLevel == ActivityLevel.VIGOROUS -> HeartRateThresholds(
                criticalMin = 50,
                warningMin = 60,
                normalMin = 70
            )
            else -> HeartRateThresholds(
                criticalMin = 40,
                warningMin = 50,
                normalMin = 60
            )
        }
    }
}