package com.brightstart.app
import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private val modules = listOf(
        "Students" to "get_students.php", "Volunteers" to "get_volunteers.php", "Programs" to "get_programs.php",
        "Sessions" to "get_sessions.php", "Enrolments" to "get_enrolments.php", "Attendance" to "get_attendance.php",
        "Performance" to "get_performance.php", "Donations" to "get_donations.php")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val menu = findViewById<LinearLayout>(R.id.menu)
        fun btn(label: String, click: () -> Unit) {
            menu.addView(MaterialButton(this).apply {
                text = label; cornerRadius = 100
                backgroundTintList = ColorStateList.valueOf(getColor(R.color.bs_accent))
                setTextColor(getColor(R.color.bs_on_accent))
                setOnClickListener { click() }
            })
        }
        modules.forEach { (t, e) ->
            btn(t) { startActivity(Intent(this, ModuleActivity::class.java).putExtra("title", t).putExtra("endpoint", e)) }
        }
        btn("⚙️ Settings") { startActivity(Intent(this, SettingsActivity::class.java)) }
        btn("❓ Help") { startActivity(Intent(this, HelpActivity::class.java)) }
        btn("Logout") {
            lifecycleScope.launch {
                try { BsApi.api.logout() } catch (e: Exception) { Log.e("MainActivity", "Logout call failed", e) }
                BsApi.cookies.clear()
                startActivity(Intent(this@MainActivity, AuthActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
            }
        }
    }

    // Reload stats + saved settings every time the screen shows, so settings apply immediately
    override fun onResume() {
        super.onResume()
        lifecycleScope.launch {
            try {
                val s = BsApi.api.stats()
                val set = BsApi.api.getSettings().settings
                val large = set?.text_size == "large"
                findViewById<View>(R.id.flipper).visibility = if (set?.show_slider == 1) View.VISIBLE else View.GONE
                val grid = findViewById<GridLayout>(R.id.grid)
                grid.removeAllViews()
                listOf(
                    "🎓 Students" to s.students.toString(), "🤝 Volunteers" to s.volunteers.toString(),
                    "📚 Programs" to s.programs.toString(), "📅 Sessions" to s.sessions.toString(),
                    "📝 Enrolments" to s.enrolments.toString(), "💚 Donations" to "R" + s.total_donations_amount.toLong()
                ).forEach { (label, num) ->
                    val v = layoutInflater.inflate(R.layout.item_stat, grid, false)
                    v.findViewById<TextView>(R.id.tvNum).apply { text = num; textSize = if (large) 36f else 30f }
                    v.findViewById<TextView>(R.id.tvLabel).apply { text = label; textSize = if (large) 16f else 13f }
                    grid.addView(v)
                }
            } catch (e: Exception) { Log.e("MainActivity", "Load failed", e) }
        }
    }
}