package com.brightstart.app
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

// Admin only: enrol a student in an Active program (validated here and on the server)
class EnrolActivity : AppCompatActivity() {
    private class Opt(val id: Int, val label: String)
    private val students = mutableListOf<Opt>()
    private val programs = mutableListOf<Opt>()
    private val existing = mutableSetOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_enrol)
        if (!Session.isAdmin(this)) { finish(); return }
        val spStudent = findViewById<Spinner>(R.id.spStudent)
        val spProgram = findViewById<Spinner>(R.id.spProgram)
        val etDate = findViewById<EditText>(R.id.etDate)

        lifecycleScope.launch {
            try {
                apiData("get_students.php").map { it.asJsonObject }
                    .forEach { students.add(Opt(it.i("StudentID"), it.s("FirstName") + " " + it.s("LastName"))) }
                apiData("get_programs.php").map { it.asJsonObject }.filter { it.s("Status") == "Active" }
                    .forEach { programs.add(Opt(it.i("ProgramID"), it.s("ProgramName"))) }
                apiData("get_enrolments.php").map { it.asJsonObject }.forEach { existing.add(it.i("StudentID").toString() + "-" + it.i("ProgramID")) }
                spStudent.adapter = ArrayAdapter(this@EnrolActivity, android.R.layout.simple_spinner_dropdown_item, students.map { it.label })
                spProgram.adapter = ArrayAdapter(this@EnrolActivity, android.R.layout.simple_spinner_dropdown_item, programs.map { it.label })
            } catch (e: Exception) { Log.e("Enrol", "Load failed", e); toast("Cannot reach server") }
        }

        val dateRule = Regex("^\\d{4}-\\d{2}-\\d{2}$")
        findViewById<Button>(R.id.btnEnrol).setOnClickListener {
            val date = etDate.text.toString().trim()
            if (students.isEmpty()) { toast("Register a student first"); return@setOnClickListener }
            if (programs.isEmpty()) { toast("No Active programs. Approve a program first."); return@setOnClickListener }
            if (!dateRule.matches(date)) { etDate.error = "Use YYYY-MM-DD"; return@setOnClickListener }
            val st = students[spStudent.selectedItemPosition]; val pr = programs[spProgram.selectedItemPosition]
            if (existing.contains("${st.id}-${pr.id}")) { toast("${st.label} is already enrolled in ${pr.label}"); return@setOnClickListener }
            lifecycleScope.launch {
                try {
                    val r = BsApi.api.createEnrolment(EnrolBody(st.id, pr.id, date))
                    toast(r.message ?: "Saved")
                    if (r.success) finish()
                } catch (e: Exception) { Log.e("Enrol", "Save failed", e); toast("Could not enrol the student") }
            }
        }
    }
    private fun toast(m: String) = Toast.makeText(this, m, Toast.LENGTH_LONG).show()
}
