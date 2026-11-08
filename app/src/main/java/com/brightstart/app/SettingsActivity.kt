package com.brightstart.app
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.SwitchCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class SettingsActivity : AppCompatActivity() {
    private val TAG = "SettingsActivity"
    private lateinit var swDark: SwitchCompat
    private lateinit var swLarge: SwitchCompat
    private lateinit var swSlider: SwitchCompat

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        swDark = findViewById(R.id.swDark); swLarge = findViewById(R.id.swLarge); swSlider = findViewById(R.id.swSlider)

        // Load this user's saved settings from the website's API
        lifecycleScope.launch {
            try {
                val s = BsApi.api.getSettings().settings
                if (s != null) {
                    swDark.isChecked = s.theme == "dark"
                    swLarge.isChecked = s.text_size == "large"
                    swSlider.isChecked = s.show_slider == 1
                }
            } catch (e: Exception) { Log.e(TAG, "Load failed", e); toast("Could not load settings") }
        }

        findViewById<Button>(R.id.btnSave).setOnClickListener {
            lifecycleScope.launch {
                try {
                    val body = SettingsBody(if (swDark.isChecked) "dark" else "light",
                        if (swLarge.isChecked) "large" else "normal", if (swSlider.isChecked) 1 else 0)
                    val r = BsApi.api.saveSettings(body)
                    toast(r.message ?: "Saved")
                    if (r.success) {
                        Session.setDark(this@SettingsActivity, swDark.isChecked)
                        AppCompatDelegate.setDefaultNightMode(
                            if (swDark.isChecked) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO)
                    }
                } catch (e: Exception) { Log.e(TAG, "Save failed", e); toast("Cannot reach server") }
            }
        }

        findViewById<Button>(R.id.btnLogout).setOnClickListener {
            lifecycleScope.launch {
                try { BsApi.api.logout() } catch (e: Exception) { Log.e(TAG, "Logout call failed", e) }
                BsApi.cookies.clear()
                Log.d(TAG, "Logged out")
                startActivity(Intent(this@SettingsActivity, AuthActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
            }
        }
    }
    private fun toast(m: String) = Toast.makeText(this, m, Toast.LENGTH_SHORT).show()
}