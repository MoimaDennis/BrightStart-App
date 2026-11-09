package com.brightstart.app
import android.os.Bundle
import android.util.Log
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import android.content.res.ColorStateList
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch

// Admin only: approve or reject new accounts (same as the website's Users page)
class AdminUsersActivity : AppCompatActivity() {
    private lateinit var box: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_module)
        findViewById<TextView>(R.id.tvTitle).text = "User approvals"
        box = findViewById(R.id.box)
        load()
    }

    private fun load() {
        lifecycleScope.launch {
            try {
                val users = BsApi.api.adminUsers().data
                box.removeAllViews()
                users.forEach { addCard(it) }
            } catch (e: Exception) {
                Log.e("AdminUsers", "Load failed", e)
                Toast.makeText(this@AdminUsersActivity, "Cannot reach server", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun addCard(u: AdminUserDto) {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundResource(R.drawable.bs_card)
            setPadding(40, 30, 40, 30)
        }
        card.addView(TextView(this).apply {
            text = "${u.FullName}\n${u.Email}\nRole: ${u.Role} • " + if (u.Verified == 1) "Approved" else "Pending approval"
            setTextColor(getColor(R.color.bs_text))
        })
        if (u.Verified != 1) {
            card.addView(button("✅ Approve") { BsApi.api.approveUser(AdminIdBody(u.AdminID)) })
            card.addView(button("❌ Reject") { BsApi.api.rejectUser(AdminIdBody(u.AdminID)) })
        }
        box.addView(card, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            .apply { setMargins(0, 0, 0, 24) })
    }

    private fun button(label: String, call: suspend () -> SimpleResponse) = MaterialButton(this).apply {
        text = label; cornerRadius = 100
        backgroundTintList = ColorStateList.valueOf(getColor(R.color.bs_accent))
        setTextColor(getColor(R.color.bs_on_accent))
        setOnClickListener {
            lifecycleScope.launch {
                try {
                    Toast.makeText(this@AdminUsersActivity, call().message ?: "Done", Toast.LENGTH_SHORT).show()
                    load()
                } catch (e: Exception) { Log.e("AdminUsers", "Action failed", e) }
            }
        }
    }
}