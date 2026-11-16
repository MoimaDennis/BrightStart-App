package com.brightstart.app
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.gson.JsonObject
import kotlinx.coroutines.launch

// Admin, or a Coordinator allocated as helper, records a score for a student enrolled in that program
class RecordPerformanceActivity : AppCompatActivity() {
    private class Opt(val id: Int, val label: String)
    private val programs = mutableListOf<Opt>()
    private var enrolments = listOf<JsonObject>()
    private val students = mutableListOf<Opt>()
    private lateinit var spStudent: Spinner

    private fun grade(s: Double) = when { s >= 80 -> "A"; s >= 70 -> "B"; s >= 60 -> "C"; s >= 50 -> "D"; else -> "F" }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_record_performance)
        val admin = Session.isAdmin(this)
        if (!admin && !Session.isHelper(this)) { finish(); return }
        val allowed = Session.helperPrograms(this)
        val spProgram = findViewById<Spinner>(R.id.spProgram); spStudent = findViewById(R.id.spStudent)
        val etDate = findViewById<EditText>(R.id.etDate); val etScore = findViewById<EditText>(R.id.etScore)
        val etComments = findViewById<EditText>(R.id.etComments)
        val tvGrade = findViewById<TextView>(R.id.tvGrade); val tvNote = findViewById<TextView>(R.id.tvNote)

        etScore.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val v = s.toString().toDoubleOrNull()
                tvGrade.text = if (v == null || v < 0 || v > 100) "Auto grade: -" else "Auto grade: " + grade(v)
            }
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
        })

        lifecycleScope.launch {
            try {
                apiData("get_programs.php").map { it.asJsonObject }
                    .filter { it.s("Status") != "Closed" && (admin || it.i("ProgramID") in allowed) }
                    .forEach { programs.add(Opt(it.i("ProgramID"), it.s("ProgramName"))) }
                enrolments = apiData("get_enrolments.php").map { it.asJsonObject }.filter { it.s("Status") == "Active" }
                spProgram.adapter = ArrayAdapter(this@RecordPerformanceActivity, android.R.layout.simple_spinner_dropdown_item, programs.map { it.label })
                spProgram.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) = loadStudents(programs[pos].id)
                    override fun onNothingSelected(p: AdapterView<*>?) {}
                }
                if (programs.isEmpty()) { tvNote.text = "No open programs available for you."; tvNote.visibility = View.VISIBLE }
            } catch (e: Exception) { Log.e("RecordPerf", "Load failed", e); toast("Cannot reach server") }
        }

        val dateRule = Regex("^\\d{4}-\\d{2}-\\d{2}$")
        findViewById<Button>(R.id.btnSavePerf).setOnClickListener {
            val date = etDate.text.toString().trim(); val score = etScore.text.toString().trim().toDoubleOrNull()
            if (programs.isEmpty()) { toast("No open program available"); return@setOnClickListener }
            if (students.isEmpty()) { toast("No active students in this program"); return@setOnClickListener }
            if (!dateRule.matches(date)) { etDate.error = "Use YYYY-MM-DD"; return@setOnClickListener }
            if (score == null || score < 0 || score > 100) { etScore.error = "Score must be 0 to 100"; return@setOnClickListener }
            val prog = programs[spProgram.selectedItemPosition]; val st = students[spStudent.selectedItemPosition]
            lifecycleScope.launch {
                try {
                    val r = BsApi.api.createPerformance(PerformanceBody(st.id, prog.id, date, score, etComments.text.toString().trim()))
                    toast(r.message ?: "Saved")
                    if (r.success) finish()
                } catch (e: Exception) { Log.e("RecordPerf", "Save failed", e); toast("Could not save performance") }
            }
        }
    }

    private fun loadStudents(programId: Int) {
        students.clear()
        enrolments.filter { it.i("ProgramID") == programId }.forEach { students.add(Opt(it.i("StudentID"), it.s("StudentName"))) }
        spStudent.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, students.map { it.label })
    }
    private fun toast(m: String) = Toast.makeText(this, m, Toast.LENGTH_LONG).show()
}
