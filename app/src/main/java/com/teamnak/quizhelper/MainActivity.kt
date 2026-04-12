package com.teamnak.quizhelper

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val etApiKey = findViewById<EditText>(R.id.etApiKey)
        val btnSaveKey = findViewById<Button>(R.id.btnSaveKey)
        val btnOverlay = findViewById<Button>(R.id.btnOverlayPermission)
        val btnAccessibility = findViewById<Button>(R.id.btnAccessibility)
        val tvStatus = findViewById<TextView>(R.id.tvStatus)

        // Load saved key
        val prefs = getSharedPreferences("quiz_helper", Context.MODE_PRIVATE)
        val savedKey = prefs.getString("api_key", "")
        if (!savedKey.isNullOrEmpty()) {
            etApiKey.setText(savedKey)
        }

        btnSaveKey.setOnClickListener {
            val key = etApiKey.text.toString().trim()
            if (key.isEmpty()) {
                Toast.makeText(this, "Please enter your API key", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            prefs.edit().putString("api_key", key).apply()
            Toast.makeText(this, "API key saved", Toast.LENGTH_SHORT).show()
            updateStatus(tvStatus)
        }

        btnOverlay.setOnClickListener {
            if (!Settings.canDrawOverlays(this)) {
                startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")))
            } else {
                Toast.makeText(this, "Overlay permission already granted", Toast.LENGTH_SHORT).show()
            }
        }

        btnAccessibility.setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }

        updateStatus(tvStatus)
    }

    override fun onResume() {
        super.onResume()
        updateStatus(findViewById(R.id.tvStatus))
    }

    private fun updateStatus(tvStatus: TextView) {
        val prefs = getSharedPreferences("quiz_helper", Context.MODE_PRIVATE)
        val hasKey = !prefs.getString("api_key", "").isNullOrEmpty()
        val hasOverlay = Settings.canDrawOverlays(this)
        val hasAccessibility = isAccessibilityEnabled()

        val status = when {
            hasKey && hasOverlay && hasAccessibility -> "Status: Ready — open any quiz app"
            !hasKey -> "Status: Missing API key"
            !hasOverlay -> "Status: Missing overlay permission"
            !hasAccessibility -> "Status: Enable accessibility service"
            else -> "Status: Not ready"
        }
        tvStatus.text = status
    }

    private fun isAccessibilityEnabled(): Boolean {
        val am = getSystemService(ACCESSIBILITY_SERVICE) as AccessibilityManager
        val enabledServices = Settings.Secure.getString(
            contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        return enabledServices.contains(packageName)
    }
}
