package com.brightstart.app
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Any logged-in user can donate through the API (add_donation.php) and gets a receipt.
// Users only ever see their own donation here, never anyone else's amounts or emails.
class DonateActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_donate)
        val etDonor = findViewById<EditText>(R.id.etDonor)
        val etContact = findViewById<EditText>(R.id.etContact)
        val etAmount = findViewById<EditText>(R.id.etAmount)
        val etNotes = findViewById<EditText>(R.id.etNotes)
        val spType = findViewById<Spinner>(R.id.spType)
        spType.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, listOf("Cash", "Bank Transfer", "In-Kind"))
        etDonor.setText(Session.name(this)); etContact.setText(Session.email(this))

        findViewById<Button>(R.id.btnDonate).setOnClickListener {
            val name = etDonor.text.toString().trim()
            val contact = etContact.text.toString().trim()
            val amount = etAmount.text.toString().trim().toDoubleOrNull() ?: 0.0
            if (name.isEmpty()) { etDonor.error = "Enter your name"; return@setOnClickListener }
            if (!Validator.isValidEmail(contact)) { etContact.error = "Enter a valid email"; return@setOnClickListener }
            if (amount <= 0) { etAmount.error = "Enter a valid amount"; return@setOnClickListener }
            val type = spType.selectedItem.toString()
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
            lifecycleScope.launch {
                try {
                    val r = BsApi.api.donate(DonationBody(name, contact, amount, today, type, etNotes.text.toString().trim()))
                    if (r.success) {
                        findViewById<TextView>(R.id.tvReceipt).text =
                            "Date: ${Ui.date(today)}\nDonor Name: $name\nContact: $contact\nType: $type\nAmount: R ${"%.2f".format(Locale.US, amount)}"
                        findViewById<View>(R.id.receipt).visibility = View.VISIBLE
                        etAmount.setText(""); etNotes.setText("")
                        toast("Thank you, $name! Your donation has been recorded.")
                    } else toast(r.message ?: "Donation failed")
                } catch (e: Exception) { Log.e("Donate", "Donate failed", e); toast("Could not send donation. Check your connection.") }
            }
        }
    }
    private fun toast(m: String) = Toast.makeText(this, m, Toast.LENGTH_LONG).show()
}
