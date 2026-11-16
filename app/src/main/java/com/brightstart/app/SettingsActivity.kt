package com.brightstart.app
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.SwitchCompat
import androidx.lifecycle.lifecycleScope
import com.google.gson.Gson
import kotlinx.coroutines.launch
import retrofit2.Response

class SettingsActivity : AppCompatActivity() {
    private val TAG = "SettingsActivity"
    private lateinit var swDark: SwitchCompat
    private lateinit var swLarge: SwitchCompat
    private lateinit var swSlider: SwitchCompat

    // Reads the success flag and message, even when the server answers 400
    private fun result(r: Response<SimpleResponse>): SimpleResponse {
        val b = r.body() ?: try { Gson().fromJson(r.errorBody()?.string(), SimpleResponse::class.java) } catch (e: Exception) { null }
        return b ?: SimpleResponse(false, "Something went wrong")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        swDark = findViewById(R.id.swDark); swLarge = findViewById(R.id.swLarge); swSlider = findViewById(R.id.swSlider)
        val etName = findViewById<EditText>(R.id.etFullName)

        // Load this user's saved settings and profile from the website's API
        lifecycleScope.launch {
            try {
                val resp = BsApi.api.getSettings()
                val s = resp.settings
                if (s != null) {
                    swDark.isChecked = s.theme == "dark"
                    swLarge.isChecked = s.text_size == "large"
                    swSlider.isChecked = s.show_slider == 1
                }
                resp.profile?.let { p ->
                    etName.setText(p.FullName ?: "")
                    findViewById<TextView>(R.id.tvEmail).text = p.Email ?: ""      // your own email only
                    findViewById<TextView>(R.id.tvRole).text = p.Role ?: ""
                    findViewById<TextView>(R.id.tvProvider).text = if (p.AuthProvider == "google") "Google (SSO)" else "Email and password"
                    if (p.AuthProvider == "google") {
                        findViewById<View>(R.id.passwordForm).visibility = View.GONE
                        findViewById<View>(R.id.tvGoogleNote).visibility = View.VISIBLE
                    }
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

        // Update profile (name only, same as the website)
        findViewById<Button>(R.id.btnProfile).setOnClickListener {
            val name = etName.text.toString().trim()
            if (name.length < 2) { etName.error = "Enter your full name"; return@setOnClickListener }
            lifecycleScope.launch {
                try {
                    val r = result(BsApi.api.updateProfile(ProfileBody(name)))
                    toast(if (r.success) "Profile updated" else r.message ?: "Could not update")
                    if (r.success) Session.saveUser(this@SettingsActivity, name, Session.email(this@SettingsActivity), Session.role(this@SettingsActivity))
                } catch (e: Exception) { Log.e(TAG, "Profile failed", e); toast("Cannot reach server") }
            }
        }

        // Change password
        val etCur = findViewById<EditText>(R.id.etCurrent)
        val etNew = findViewById<EditText>(R.id.etNew)
        val etCon = findViewById<EditText>(R.id.etConfirmNew)
        findViewById<Button>(R.id.btnPassword).setOnClickListener {
            val cur = etCur.text.toString(); val nw = etNew.text.toString(); val cf = etCon.text.toString()
            if (cur.isEmpty()) { etCur.error = "Enter your current password"; return@setOnClickListener }
            if (!Validator.isValidPassword(nw)) { etNew.error = "Min 8 characters, with a letter and a number"; return@setOnClickListener }
            if (nw != cf) { etCon.error = "Passwords do not match"; return@setOnClickListener }
            lifecycleScope.launch {
                try {
                    val r = result(BsApi.api.changePassword(PasswordBody(cur, nw, cf)))
                    toast(r.message ?: "Done")
                    if (r.success) { etCur.setText(""); etNew.setText(""); etCon.setText("") }
                } catch (e: Exception) { Log.e(TAG, "Password failed", e); toast("Cannot reach server") }
            }
        }

        findViewById<Button>(R.id.btnLogout).setOnClickListener {
            lifecycleScope.launch {
                try { BsApi.api.logout() } catch (e: Exception) { Log.e(TAG, "Logout call failed", e) }
                BsApi.cookies.clear()
                Session.clearUser(this@SettingsActivity)
                Log.d(TAG, "Logged out")
                startActivity(Intent(this@SettingsActivity, AuthActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
            }
        }
    }
    private fun toast(m: String) = Toast.makeText(this, m, Toast.LENGTH_SHORT).show()
}
