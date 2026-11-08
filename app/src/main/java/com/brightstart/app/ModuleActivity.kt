package com.brightstart.app
import android.os.Bundle
import android.util.Log
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.gson.JsonParser
import kotlinx.coroutines.launch

// One screen reused for every module: loads get_*.php and shows each record as a card
class ModuleActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_module)
        val endpoint = intent.getStringExtra("endpoint") ?: return
        findViewById<TextView>(R.id.tvTitle).text = intent.getStringExtra("title")
        val box = findViewById<LinearLayout>(R.id.box)
        lifecycleScope.launch {
            try {
                val arr = JsonParser.parseString(BsApi.api.list(endpoint).string()).asJsonObject.getAsJsonArray("data")
                for (el in arr) {
                    val o = el.asJsonObject
                    val tv = TextView(this@ModuleActivity).apply {
                        text = o.entrySet().joinToString("\n") { "${it.key}: ${if (it.value.isJsonNull) "-" else it.value.asString}" }
                        setTextColor(getColor(R.color.bs_text))
                        setBackgroundResource(R.drawable.bs_card)
                        setPadding(40, 30, 40, 30)
                    }
                    box.addView(tv, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                        .apply { setMargins(0, 0, 0, 24) })
                }
            } catch (e: Exception) {
                Log.e("ModuleActivity", "Load failed", e)
                Toast.makeText(this@ModuleActivity, "Cannot reach server", Toast.LENGTH_LONG).show()
            }
        }
    }
}
