package com.brightstart.app
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.gson.JsonArray
import com.google.gson.JsonParser
import kotlinx.coroutines.launch

// Staff (and admins) mark the register: same as the website's Log Attendance page
class AttendanceActivity : AppCompatActivity() {
    private class Sess(val id: Int, val programId: Int, val label: String)
    private class Row(val studentId: Int, val attendanceId: Int, val group: RadioGroup, val notes: EditText)

    private val sessions = mutableListOf<Sess>()
    private var enrolments = JsonArray()
    private var records = JsonArray()
    private val rows = mutableListOf<Row>()
    private lateinit var box: LinearLayout
    private lateinit var tvEmpty: TextView
    private lateinit var btnSave: Button
    private val statuses = listOf("Present", "Absent", "Excused")

    private suspend fun data(path: String): JsonArray =
        JsonParser.parseString(BsApi.api.list(path).string()).asJsonObject.getAsJsonArray("data")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_attendance)
        box = findViewById(R.id.students); tvEmpty = findViewById(R.id.tvEmpty); btnSave = findViewById(R.id.btnSaveReg)
        val sp = findViewById<Spinner>(R.id.spSession)

        lifecycleScope.launch {
            try {
                val closed = data("get_programs.php").map { it.asJsonObject }.filter { it.s("Status") == "Closed" }.map { it.i("ProgramID") }.toSet()
                data("get_sessions.php").forEach {
                    val o = it.asJsonObject
                    if (o.i("ProgramID") in closed) return@forEach
                    sessions.add(Sess(o.i("SessionID"), o.i("ProgramID"),
                        "${o.s("ProgramName")} – ${Ui.date(o.s("SessionDate"))} (${o.s("Venue")})"))
                }
                enrolments = data("get_enrolments.php")
                records = data("get_attendance.php")
                if (sessions.isEmpty()) { showEmpty("No sessions scheduled yet."); return@launch }
                sp.adapter = ArrayAdapter(this@AttendanceActivity, android.R.layout.simple_spinner_dropdown_item, sessions.map { it.label })
                sp.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) = render(sessions[pos])
                    override fun onNothingSelected(p: AdapterView<*>?) {}
                }
            } catch (e: Exception) { Log.e("Attendance", "Load failed", e); toast("Cannot reach server") }
        }

        btnSave.setOnClickListener {
            val sess = sessions[sp.selectedItemPosition]
            lifecycleScope.launch {
                try {
                    var saved = 0
                    for (r in rows) {
                        val checked = r.group.checkedRadioButtonId
                        if (checked == -1) continue
                        val status = r.group.findViewById<RadioButton>(checked).text.toString()
                        val body = AttendanceBody(sess.id, r.studentId, status, r.notes.text.toString().trim())
                        if (r.attendanceId > 0) BsApi.api.updateAttendance(r.attendanceId, body) else BsApi.api.addAttendance(body)
                        saved++
                    }
                    toast(if (saved == 0) "Mark at least one student first" else "Attendance saved for $saved student(s)")
                    records = data("get_attendance.php")
                    render(sess)
                } catch (e: Exception) { Log.e("Attendance", "Save failed", e); toast("Could not save attendance") }
            }
        }
    }

    private fun showEmpty(msg: String) { tvEmpty.text = msg; tvEmpty.visibility = View.VISIBLE; btnSave.visibility = View.GONE }

    private fun render(sess: Sess) {
        box.removeAllViews(); rows.clear(); tvEmpty.visibility = View.GONE
        val students = enrolments.map { it.asJsonObject }.filter { it.i("ProgramID") == sess.programId && it.s("Status") == "Active" }
        if (students.isEmpty()) { showEmpty("No active students are enrolled in this program."); return }
        btnSave.visibility = View.VISIBLE
        val saved = records.map { it.asJsonObject }.filter { it.i("SessionID") == sess.id }.associateBy { it.i("StudentID") }

        for (st in students) {
            val sid = st.i("StudentID")
            val old = saved[sid]
            val card = Ui.card(this)
            card.addView(Ui.text(this, st.s("StudentName"), 17f, serif = true))
            val group = RadioGroup(this).apply { orientation = RadioGroup.HORIZONTAL }
            statuses.forEach { s ->
                group.addView(RadioButton(this).apply {
                    id = View.generateViewId(); text = s; setTextColor(getColor(R.color.bs_text)); typeface = Ui.body(this@AttendanceActivity)
                    isChecked = old?.s("AttendanceStatus") == s
                })
            }
            val notes = EditText(this).apply {
                hint = "Notes (optional)"; setText(old?.s("Notes") ?: "")
                setBackgroundResource(R.drawable.bs_input); setTextColor(getColor(R.color.bs_text)); setHintTextColor(getColor(R.color.bs_muted))
                setPadding(Ui.dp(context, 14), Ui.dp(context, 10), Ui.dp(context, 14), Ui.dp(context, 10)); textSize = 14f
            }
            card.addView(group); card.addView(notes)
            box.addView(card)
            rows.add(Row(sid, old?.i("AttendanceID") ?: 0, group, notes))
        }
    }
    private fun toast(m: String) = Toast.makeText(this, m, Toast.LENGTH_LONG).show()
}
