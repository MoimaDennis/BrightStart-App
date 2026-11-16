package com.brightstart.app
import android.os.Bundle
import android.util.Log
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.gson.JsonParser
import kotlinx.coroutines.launch

// One screen reused for every module: loads get_*.php and shows each record as a website-style card
class ModuleActivity : AppCompatActivity() {
    // Staff (non-admin) must never see emails or donation money
    private fun hiddenForStaff(key: String) =
        key.contains("email", true) || key.equals("DonorContact", true) || key.contains("Donation", true)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_module)
        val endpoint = intent.getStringExtra("endpoint") ?: return
        val admin = Session.isAdmin(this)
        if (!admin && endpoint != "get_programs.php") { finish(); return }   // staff: programs only
        findViewById<TextView>(R.id.tvTitle).text = intent.getStringExtra("title")
        val box = findViewById<LinearLayout>(R.id.box)
        lifecycleScope.launch {
            try {
                val arr = JsonParser.parseString(BsApi.api.list(endpoint).string()).asJsonObject.getAsJsonArray("data")
                if (arr.size() == 0) box.addView(Ui.text(this@ModuleActivity, "No records yet.", 15f, R.color.bs_muted))
                for (el in arr) {
                    val o = el.asJsonObject
                    val titleKey = listOf("FullName", "StudentName", "ProgramName", "DonorName", "VolName").firstOrNull { o.has(it) }
                    val title = when {
                        o.has("FirstName") && o.has("LastName") -> o.s("FirstName") + " " + o.s("LastName")
                        titleKey != null -> o.s(titleKey)
                        else -> ""
                    }
                    val card = Ui.card(this@ModuleActivity)
                    if (title.isNotEmpty()) card.addView(Ui.text(this@ModuleActivity, title, 19f, serif = true))
                    for ((k, v) in o.entrySet()) {
                        if (k.endsWith("ID") || k == "FirstName" || k == "LastName" || k == titleKey) continue
                        if (!admin && hiddenForStaff(k)) continue
                        var value = if (v.isJsonNull) "-" else v.asString
                        if (k == "DonationAmount") value = "R $value"
                        if (Regex("^\\d{4}-\\d{2}-\\d{2}$").matches(value)) value = Ui.date(value)
                        card.addView(Ui.text(this@ModuleActivity, k.replace(Regex("(?<=[a-z])(?=[A-Z])"), " ").uppercase(), 11f, R.color.bs_muted, bold = true)
                            .apply { setPadding(0, Ui.dp(this@ModuleActivity, 10), 0, 0); letterSpacing = 0.08f })
                        card.addView(Ui.text(this@ModuleActivity, value, 15f))
                    }
                    box.addView(card)
                }
            } catch (e: Exception) {
                Log.e("ModuleActivity", "Load failed", e)
                Toast.makeText(this@ModuleActivity, "Cannot reach server", Toast.LENGTH_LONG).show()
            }
        }
    }
}
