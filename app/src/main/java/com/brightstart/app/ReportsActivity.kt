package com.brightstart.app
import android.os.Bundle
import android.util.Log
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import java.util.Locale

// Admin only: the website's Reports Centre (progress, donations, volunteers, enrolments)
class ReportsActivity : AppCompatActivity() {
    private lateinit var tabsBox: LinearLayout
    private lateinit var content: LinearLayout
    private val tabs = listOf("Student Progress", "Donations", "Volunteers", "Enrolments")

    private fun grade(s: Double) = when { s >= 80 -> "A"; s >= 70 -> "B"; s >= 60 -> "C"; s >= 50 -> "D"; else -> "F" }
    private fun money(v: Double) = "R " + "%,.2f".format(Locale.US, v)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_module)
        if (!Session.isAdmin(this)) { finish(); return }
        findViewById<TextView>(R.id.tvTitle).text = "📋 Reports Centre"
        val box = findViewById<LinearLayout>(R.id.box)
        tabsBox = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        box.addView(tabsBox); box.addView(content)
        show(0)
    }

    private fun show(i: Int) {
        tabsBox.removeAllViews()
        for (r in 0..1) {
            val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            for (k in r * 2..r * 2 + 1) {
                row.addView(Ui.pill(this, tabs[k], outline = k != i) { show(k) },
                    LinearLayout.LayoutParams(0, -2, 1f).apply { setMargins(0, 0, Ui.dp(this@ReportsActivity, 6), Ui.dp(this@ReportsActivity, 6)) })
            }
            tabsBox.addView(row)
        }
        content.removeAllViews()
        lifecycleScope.launch {
            try {
                when (i) {
                    0 -> progress(); 1 -> donations(); 2 -> volunteers(); else -> enrolments()
                }
            } catch (e: Exception) { Log.e("Reports", "Load failed", e); Toast.makeText(this@ReportsActivity, "Cannot reach server", Toast.LENGTH_LONG).show() }
        }
    }

    private fun block(title: String, lines: List<Pair<String, String>>) {
        val card = Ui.card(this)
        card.addView(Ui.text(this, title, 19f, serif = true))
        if (lines.isEmpty()) card.addView(Ui.text(this, "No data yet.", 14f, R.color.bs_muted).apply { setPadding(0, Ui.dp(context, 10), 0, 0) })
        lines.forEach { (a, b) ->
            card.addView(Ui.text(this, a, 15f, bold = true).apply { setPadding(0, Ui.dp(context, 10), 0, 0) })
            card.addView(Ui.text(this, b, 13f, R.color.bs_muted))
        }
        content.addView(card)
    }

    private suspend fun progress() {
        val perf = apiData("get_performance.php").map { it.asJsonObject }
        val att = apiData("get_attendance.php").map { it.asJsonObject }
        val lines = perf.groupBy { it.s("StudentName") }.map { (name, rows) ->
            val avg = rows.map { it.s("Score").toDoubleOrNull() ?: 0.0 }.average()
            val a = att.filter { it.s("StudentName") == name }
            val rate = if (a.isEmpty()) "no attendance" else "attendance " + (a.count { it.s("AttendanceStatus") == "Present" } * 100 / a.size) + "%"
            name to "Average %.1f%% (%s) • %d assessments • %s".format(Locale.US, avg, grade(avg), rows.size, rate)
        }
        block("Student progress", lines)
    }

    private suspend fun donations() {
        val d = apiData("get_donations.php").map { it.asJsonObject }
        val total = d.sumOf { it.s("DonationAmount").toDoubleOrNull() ?: 0.0 }
        block("Donation summary", listOf("Total donated" to money(total), "Number of donations" to d.size.toString()))
        block("By type", d.groupBy { it.s("DonationType") }.map { (t, rows) ->
            t to money(rows.sumOf { it.s("DonationAmount").toDoubleOrNull() ?: 0.0 }) + " • " + rows.size + " donations" })
    }

    private suspend fun volunteers() {
        val s = apiData("get_sessions.php").map { it.asJsonObject }
        block("Sessions per volunteer", s.groupBy { it.s("VolName") }.map { (n, rows) -> n to rows.size.toString() + " sessions" })
    }

    private suspend fun enrolments() {
        val e = apiData("get_enrolments.php").map { it.asJsonObject }
        block("Enrolments per program", e.groupBy { it.s("ProgramName") }.map { (p, rows) ->
            p to "${rows.size} total • ${rows.count { it.s("Status") == "Active" }} active • " +
                "${rows.count { it.s("Status") == "Completed" }} completed • ${rows.count { it.s("Status") == "Withdrawn" }} withdrawn" })
    }
}
