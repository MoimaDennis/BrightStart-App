package com.brightstart.app
import android.os.Bundle
import android.util.Log
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import kotlinx.coroutines.launch

// Admin only: approve or close programs, and allocate a Coordinator as helper staff
class ProgramsAdminActivity : AppCompatActivity() {
    private lateinit var box: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_module)
        if (!Session.isAdmin(this)) { finish(); return }
        findViewById<TextView>(R.id.tvTitle).text = "Programs"
        box = findViewById(R.id.box)
        load()
    }

    private fun load() {
        lifecycleScope.launch {
            try {
                val programs = apiData("get_programs.php")
                val helpers = apiData("program_helpers.php")
                val coords = BsApi.api.adminUsers().data.filter { it.Role == "Coordinator" && it.Verified == 1 }
                box.removeAllViews()
                if (programs.size() == 0) box.addView(Ui.text(this@ProgramsAdminActivity, "No programs yet.", 15f, R.color.bs_muted))
                programs.forEach { render(it.asJsonObject, helpers, coords) }
            } catch (e: Exception) {
                Log.e("ProgramsAdmin", "Load failed", e); toast("Cannot reach server")
            }
        }
    }

    private fun render(p: JsonObject, helpers: JsonArray, coords: List<AdminUserDto>) {
        val id = p.i("ProgramID"); val status = p.s("Status")
        val card = Ui.card(this)
        card.addView(Ui.text(this, p.s("ProgramName"), 19f, serif = true))
        card.addView(Ui.text(this, "STATUS: ${status.uppercase()}", 12f, if (status == "Closed") R.color.bs_logout else R.color.bs_accent, bold = true)
            .apply { setPadding(0, Ui.dp(context, 6), 0, 0) })
        val end = if (p.s("EndDate").isEmpty()) "open" else Ui.date(p.s("EndDate"))
        card.addView(Ui.text(this, "${Ui.date(p.s("StartDate"))} – $end", 13f, R.color.bs_muted))
        if (p.s("Description").isNotEmpty()) card.addView(Ui.text(this, p.s("Description"), 14f).apply { setPadding(0, Ui.dp(context, 8), 0, 0) })
        val names = helpers.map { it.asJsonObject }.filter { it.i("ProgramID") == id }.map { it.s("HelperName") }
        card.addView(Ui.text(this, "Helper staff: " + if (names.isEmpty()) "none" else names.joinToString(", "), 13f, R.color.bs_muted)
            .apply { setPadding(0, Ui.dp(context, 8), 0, 0) })

        fun add(label: String, outline: Boolean = false, click: () -> Unit) =
            card.addView(Ui.pill(this, label, outline, click), LinearLayout.LayoutParams(-1, -2).apply { topMargin = Ui.dp(this@ProgramsAdminActivity, 10) })
        when (status) {
            "Pending" -> add("✅ Approve program") { setStatus(id, "Active") }
            "Active" -> add("🔒 Close program") {
                AlertDialog.Builder(this).setTitle("Close this program?").setMessage("Closed programs stop taking enrolments, sessions and attendance.")
                    .setPositiveButton("Close") { _, _ -> setStatus(id, "Closed") }.setNegativeButton("Cancel", null).show()
            }
            else -> add("↩ Reopen program") { setStatus(id, "Active") }
        }
        add("👥 Allocate helper staff", outline = true) { chooseHelper(id, helpers, coords) }
        box.addView(card)
    }

    private fun setStatus(id: Int, status: String) {
        lifecycleScope.launch {
            try { toast(BsApi.api.setProgramStatus(ProgramStatusBody(id, status)).message ?: "Done"); load() }
            catch (e: Exception) { Log.e("ProgramsAdmin", "Status failed", e); toast("Could not update the program") }
        }
    }

    private fun chooseHelper(pid: Int, helpers: JsonArray, coords: List<AdminUserDto>) {
        if (coords.isEmpty()) { toast("No approved Coordinators yet. Approve one under Users."); return }
        val assigned = helpers.map { it.asJsonObject }.filter { it.i("ProgramID") == pid }.map { it.i("AdminID") }.toSet()
        val labels = coords.map { (if (it.AdminID in assigned) "✔ " else "") + it.FullName }.toTypedArray()
        AlertDialog.Builder(this).setTitle("Tap a Coordinator to add or remove").setItems(labels) { _, i ->
            val c = coords[i]
            val action = if (c.AdminID in assigned) "remove" else "add"
            lifecycleScope.launch {
                try { toast(BsApi.api.helperAction(HelperBody(action, pid, c.AdminID)).message ?: "Done"); load() }
                catch (e: Exception) { Log.e("ProgramsAdmin", "Helper failed", e); toast("Could not update helper staff") }
            }
        }.show()
    }
    private fun toast(m: String) = Toast.makeText(this, m, Toast.LENGTH_LONG).show()
}
