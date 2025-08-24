package com.example.dive.presentation.ui

import android.content.Context
import android.os.Bundle
import android.os.CountDownTimer
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.wear.compose.material.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dive.emergency.EmergencyManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class EmergencyCountdownActivity : ComponentActivity() {

    private lateinit var countDownTimer: CountDownTimer
    private lateinit var vibrator: Vibrator // Declare vibrator
    private val COUNTDOWN_MILLIS = 30000L // 5 seconds countdown

    // Use a MutableState to hold the remaining time, observed by Compose
    private var _remainingTime = mutableStateOf(COUNTDOWN_MILLIS / 1000)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator // Initialize vibrator
        startVibration() // Start vibration on create

        setContent {
            val remainingTime by remember { _remainingTime }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Red),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "긴급 호출까지",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
                )
                Text(
                    text = "$remainingTime 초",
                    fontSize =48.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )

                // Cancel Button
                Button(
                    onClick = {Log.d("EmergencyCountdown", "Call cancelled by user.")
                        stopVibration() // Stop vibration on cancel
                        countDownTimer.cancel() // Cancel the timer
                        finish() // Close the activity
                    },
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                ) {
                    Text("취소", fontSize = 20.sp)
                }
            }
        }

        countDownTimer = object : CountDownTimer(COUNTDOWN_MILLIS, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                // Update the mutableState directly
                _remainingTime.value = millisUntilFinished / 1000
            }

            override fun onFinish() {
                Log.d("EmergencyCountdown", "Countdown finished. Initiating call.")
                stopVibration() // Stop vibration on finish
                CoroutineScope(Dispatchers.IO).launch {
                    EmergencyManager.triggerEmergencySOS(this@EmergencyCountdownActivity, reason = "심박수 경고! (카운트다운)")
                }
                finish() // Close the countdown activity
            }
        }.start()
    }

    private fun startVibration() {
        val pattern = longArrayOf(0, 500, 500) // Delay, Vibrate, Pause
        vibrator.vibrate(VibrationEffect.createWaveform(pattern, 0)) // Repeat indefinitely
    }

    private fun stopVibration() {
        vibrator.cancel()
    }

    override fun onDestroy() {
        super.onDestroy()
        countDownTimer.cancel()
        stopVibration() // Ensure vibration is stopped when activity is destroyed
    }
}