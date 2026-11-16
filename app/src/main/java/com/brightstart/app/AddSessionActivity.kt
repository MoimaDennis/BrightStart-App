package com.brightstart.app
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

// Admin adds sessions for any program; a Coordinator allocated as helper adds them only for their program
class AddSessionActivity : AppCompatActivity() {
    private class Opt(val id: Int, val label: String)
    private val programs = mutableListOf<Opt>()
    private val volunteers = mutableListOf<Opt>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_session)
        val admin = Session.isAdmin(this)
        if (!admin && !Session.isHelper(this)) { finish(); return }
        val spProgram = findViewById<Spinner>(R.id.spProgram)
        val spVol = findViewById<Spinner>(R.id.spVolunteer)
        val etDate = findViewById<EditText>(R.id.etDate); val etStart = findViewById<EditText>(R.id.etStart)
        val etEnd = findViewById<EditText>(R.id.etEnd); val etVenue = findViewById<EditText>(R.id.etVenue)
        val tvNote = findViewById<TextView>(R.id.tvNote)
        val allowed = Session.helperPrograms(this)

        lifecycleScope.launch {
            try {
                apiData("get_programs.php").map { it.asJsonObject }
                    .filter { it.s("Status") != "Closed" && (admin || it.i("ProgramID") in allowed) }
                    .forEach { programs.add(Opt(it.i("ProgramID"), it.s("ProgramName"))) }
                apiData("get_volunteers.php").map { it.asJsonObject }.filter { it.s("Status") == "Active" }
                    .forEach { volunteers.add(Opt(it.i("VolunteerID"), it.s("FullName"))) }
                spProgram.adapter = ArrayAdapter(this@AddSessionActivity, android.R.layout.simple_spinner_dropdown_item, programs.map { it.label })
                spVol.adapter = ArrayAdapter(this@AddSessionActivity, android.R.layout.simple_spinner_dropdown_item, volunteers.map { it.label })
                if (programs.isEmpty()) { tvNote.text = "No open programs available for you."; tvNote.visibility = View.VISIBLE }
            } catch (e: Exception) { Log.e("AddSession", "Load failed", e); toast("Cannot reach server") }
        }

        val dateRule = Regex("^\\d{4}-\\d{2}-\\d{2}$")
        val timeRule = Regex("^([01]\\d|2[0-3]):[0-5]\\d$")
        findViewById<Button>(R.id.btnSaveSession).setOnClickListener {
            val date = etDate.text.toString().trim(); val start = etStart.text.toString().trim()
            val end = etEnd.text.toString().trim(); val venue = etVenue.text.toString().trim()
            if (programs.isEmpty() || volunteers.isEmpty()) { toast("A program and a volunteer are needed"); return@setOnClickListener }
            if (!dateRule.matches(date)) { etDate.error = "Use YYYY-MM-DD"; return@setOnClickListener }
            if (!timeRule.matches(start)) { etStart.error = "Use HH:MM, e.g. 14:00"; return@setOnClickListener }
            if (!timeRule.matches(end)) { etEnd.error = "Use HH:MM, e.g. 15:30"; return@setOnClickListener }
            if (end <= start) { etEnd.error = "End must be after start"; return@setOnClickListener }
            if (venue.isEmpty()) { etVenue.error = "Enter a venue"; return@setOnClickListener }
            val prog = programs[spProgram.selectedItemPosition]; val vol = volunteers[spVol.selectedItemPosition]
            lifecycleScope.launch {
                try {
                    val r = BsApi.api.createSession(SessionBody(prog.id, vol.id, date, "$start:00", "$end:00", venue))
                    toast(r.message ?: "Saved")
                    if (r.success) finish()
                } catch (e: Exception) { Log.e("AddSession", "Save failed", e); toast("Could not save the session") }
            }
        }
    }
    private fun toast(m: String) = Toast.makeText(this, m, Toast.LENGTH_LONG).show()
}
