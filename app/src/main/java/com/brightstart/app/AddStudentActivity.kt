package com.brightstart.app
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

// Admin only: register a new student (same fields as the website)
class AddStudentActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_student)
        val spGender = findViewById<Spinner>(R.id.spGender)
        spGender.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, listOf("Male", "Female", "Other"))
        val dateRule = Regex("^\\d{4}-\\d{2}-\\d{2}$")

        findViewById<Button>(R.id.btnSaveStudent).setOnClickListener {
            val first = findViewById<EditText>(R.id.etFirst).text.toString().trim()
            val last = findViewById<EditText>(R.id.etLast).text.toString().trim()
            val dob = findViewById<EditText>(R.id.etDob).text.toString().trim()
            val area = findViewById<EditText>(R.id.etArea).text.toString().trim()
            val phone = findViewById<EditText>(R.id.etPhone).text.toString().trim()
            val enrol = findViewById<EditText>(R.id.etEnrol).text.toString().trim()
            if (first.isEmpty() || last.isEmpty() || area.isEmpty() || phone.isEmpty()) { toast("Please fill in all fields"); return@setOnClickListener }
            if (!dateRule.matches(dob) || !dateRule.matches(enrol)) { toast("Dates must look like 2012-05-31"); return@setOnClickListener }
            lifecycleScope.launch {
                try {
                    val r = BsApi.api.addStudent(StudentBody(first, last, dob, spGender.selectedItem.toString(), area, phone, enrol))
                    toast(r.message ?: "Saved")
                    if (r.success) finish()
                } catch (e: Exception) { Log.e("AddStudent", "Save failed", e); toast("Could not save. Check your details and connection.") }
            }
        }
    }
    private fun toast(m: String) = Toast.makeText(this, m, Toast.LENGTH_LONG).show()
}