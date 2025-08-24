package com.example.dive

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class SettingsActivity : AppCompatActivity() {

    private lateinit var etEmergencyNumber: EditText
    private lateinit var btnSave: Button
    private lateinit var prefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        prefs = getSharedPreferences("emergency_settings", Context.MODE_PRIVATE)

        etEmergencyNumber = findViewById(R.id.etEmergencyNumber)
        btnSave = findViewById(R.id.btnSave)

        // Load saved number
        val savedNumber = prefs.getString("emergency_number", null)
        etEmergencyNumber.setText(savedNumber)

        btnSave.setOnClickListener {
            val newNumber = etEmergencyNumber.text.toString()
            if (newNumber.isNotBlank()) {
                // Save locally
                prefs.edit().putString("emergency_number", newNumber).apply()

                // Sync to watch

                Toast.makeText(this, "긴급 연락처가 저장되었습니다.", Toast.LENGTH_SHORT).show()
                finish() // Close activity after saving
            } else {
                Toast.makeText(this, "전화번호를 입력해주세요.", Toast.LENGTH_SHORT).show()
            }
        }
    }
}