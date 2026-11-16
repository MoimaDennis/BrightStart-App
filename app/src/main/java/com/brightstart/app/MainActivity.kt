package com.brightstart.app
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Html
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import java.util.Locale

class MainActivity : AppCompatActivity() {
    private class Slide(val tag: String, val line: String, val accent: String, val sub: String, val img: Int,
                        val b1: Pair<String, () -> Unit>?, val b2: Pair<String, () -> Unit>?, val stats: List<Pair<String, String>>)

    private val handler = Handler(Looper.getMainLooper())
    private var cur = 0
    private var count = 0
    private val tick = object : Runnable {
        override fun run() { if (count > 1) { show((cur + 1) % count); handler.postDelayed(this, 8000) } }
    }

    private fun open(c: Class<*>) = startActivity(Intent(this, c))
    private fun module(title: String, ep: String) =
        startActivity(Intent(this, ModuleActivity::class.java).putExtra("title", title).putExtra("endpoint", ep))

    private fun logout() {
        lifecycleScope.launch {
            try { BsApi.api.logout() } catch (e: Exception) { Log.e("MainActivity", "Logout call failed", e) }
            BsApi.cookies.clear()
            Session.clearUser(this@MainActivity)
            startActivity(Intent(this@MainActivity, AuthActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val hex = String.format("#%06X", 0xFFFFFF and getColor(R.color.bs_accent))
        findViewById<TextView>(R.id.tvLogo).text = Html.fromHtml("Bright<i><font color='$hex'>Start</font></i>", Html.FROM_HTML_MODE_LEGACY)
        val menu = findViewById<View>(R.id.menu)
        findViewById<View>(R.id.btnMenu).setOnClickListener { menu.visibility = if (menu.visibility == View.VISIBLE) View.GONE else View.VISIBLE }
        findViewById<View>(R.id.lnkSettings).setOnClickListener { open(SettingsActivity::class.java) }
        findViewById<View>(R.id.lnkHelp).setOnClickListener { open(HelpActivity::class.java) }
        findViewById<View>(R.id.lnkLogout).setOnClickListener { logout() }
    }

    // Reload stats + saved settings + role every time the screen shows
    override fun onResume() {
        super.onResume()
        lifecycleScope.launch {
            try {
                val s = BsApi.api.stats()
                val resp = BsApi.api.getSettings()
                Session.saveUser(this@MainActivity, resp.profile?.FullName, resp.profile?.Email, resp.profile?.Role)
                try {
                    Session.setHelperPrograms(this@MainActivity,
                        if (Session.isAdmin(this@MainActivity)) emptyList() else apiData("program_helpers.php").map { it.asJsonObject.i("ProgramID") })
                } catch (e: Exception) { Session.setHelperPrograms(this@MainActivity, emptyList()) }
                build(s, resp.settings)
            } catch (e: Exception) { Log.e("MainActivity", "Load failed", e) }
        }
    }

    override fun onPause() { super.onPause(); handler.removeCallbacks(tick) }

    private fun build(s: StatsResponse, set: SettingsDto?) {
        val admin = Session.isAdmin(this)
        val large = set?.text_size == "large"
        val name = Session.name(this); val role = Session.role(this)
        findViewById<TextView>(R.id.tvTopUser).text = "Logged in as: $name | Role: $role"
        findViewById<TextView>(R.id.tvBannerName).text = "👤 $name"
        findViewById<TextView>(R.id.tvBadge).text = role
        buildMenu(admin)
        findViewById<View>(R.id.sliderBox).visibility = if (set?.show_slider == 1) View.VISIBLE else View.GONE
        buildSlides(s, admin)
        buildStats(s, admin, large)
        buildCards(s, admin, large)
    }

    private fun buildMenu(admin: Boolean) {
        val menu = findViewById<LinearLayout>(R.id.menu); menu.removeAllViews()
        fun item(label: String, color: Int = R.color.bs_muted, go: () -> Unit) {
            menu.addView(TextView(this).apply {
                text = label; textSize = 15f; setTextColor(getColor(color)); typeface = Ui.body(this@MainActivity)
                setPadding(Ui.dp(this@MainActivity, 14), Ui.dp(this@MainActivity, 12), Ui.dp(this@MainActivity, 14), Ui.dp(this@MainActivity, 12))
                setOnClickListener { menu.visibility = View.GONE; go() }
            })
        }
        item("🏠 Dashboard") { }
        if (admin) {
            item("Students") { module("Students", "get_students.php") }
            item("➕ Register Student") { open(AddStudentActivity::class.java) }
            item("➕ Enrol Student") { open(EnrolActivity::class.java) }
            item("Manage Enrolments") { module("Enrolments", "get_enrolments.php") }
            item("Volunteers") { module("Volunteers", "get_volunteers.php") }
            item("Programs (approve / close)") { open(ProgramsAdminActivity::class.java) }
            item("Sessions") { module("Sessions", "get_sessions.php") }
        } else {
            item("Programs") { module("Programs", "get_programs.php") }
        }
        if (admin || Session.isHelper(this)) item("➕ Add Session") { open(AddSessionActivity::class.java) }
        item("📋 Log Attendance") { open(AttendanceActivity::class.java) }
        if (admin || Session.isHelper(this)) item("📈 Record Performance") { open(RecordPerformanceActivity::class.java) }
        if (admin) {
            item("Performance") { module("Performance", "get_performance.php") }
            item("Donation History") { module("Donations", "get_donations.php") }
            item("📊 Reports") { open(ReportsActivity::class.java) }
            item("⚙️ Users") { open(AdminUsersActivity::class.java) }
        } else {
            item("💚 Donate") { open(DonateActivity::class.java) }
        }
        item("Logout", R.color.bs_logout) { logout() }
    }

    private fun buildSlides(s: StatsResponse, admin: Boolean) {
        val slides = listOf(
            Slide("EDUCATION SUPPORT SYSTEM", "Empowering\nEvery\n", "Learner",
                "Centralised management for students, volunteers, and programs across South Africa's communities.", R.drawable.slide1,
                if (admin) "Register Student" to { open(AddStudentActivity::class.java) } else "View Programs" to { module("Programs", "get_programs.php") },
                if (admin) "View All Students" to { module("Students", "get_students.php") } else "Donate Now" to { open(DonateActivity::class.java) },
                listOf(s.students.toString() to "Students", s.programs.toString() to "Programs", s.enrolments.toString() to "Enrolments")),
            Slide("VOLUNTEER MANAGEMENT", "Dedicated\nVolunteers,\n", "Real Impact",
                "Schedule tutoring sessions, assign volunteers, and track attendance all in one place.", R.drawable.slide2,
                if (admin) "View Sessions" to { module("Sessions", "get_sessions.php") } else "View Programs" to { module("Programs", "get_programs.php") },
                if (admin) "Manage Volunteers" to { module("Volunteers", "get_volunteers.php") } else "Log Attendance" to { open(AttendanceActivity::class.java) },
                if (admin) listOf(s.volunteers.toString() to "Volunteers", s.sessions.toString() to "Sessions", "100%" to "Commitment") else listOf(s.sessions.toString() to "Sessions", "100%" to "Commitment")),
            Slide("DONATION MANAGEMENT", "Funding\nThe Future\n", "Together",
                "Track every donation, generate instant receipts, and produce comprehensive impact reports.", R.drawable.slide3,
                if (admin) "Donation History" to { module("Donations", "get_donations.php") } else "Donate Now" to { open(DonateActivity::class.java) },
                if (admin) null else "View Programs" to { module("Programs", "get_programs.php") },
                // money is only shown to admins
                if (admin) listOf("R " + "%,.2f".format(Locale.US, s.total_donations_amount) to "Total Donated", s.donations.toString() to "Donations") else emptyList())
        )
        val flipper = findViewById<ViewFlipper>(R.id.flipper)
        flipper.stopFlipping(); flipper.removeAllViews()
        val white70 = Color.parseColor("#B3FFFFFF"); val white60 = Color.parseColor("#99FFFFFF")
        val lime = Color.parseColor("#B8E04A")
        slides.forEach { sl ->
            val root = FrameLayout(this)
            root.addView(ImageView(this).apply { scaleType = ImageView.ScaleType.CENTER_CROP; setImageResource(sl.img) },
                FrameLayout.LayoutParams(-1, -1))
            root.addView(View(this).apply { setBackgroundResource(R.drawable.bs_overlay) }, FrameLayout.LayoutParams(-1, -1))
            val col = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER_VERTICAL; setPadding(Ui.dp(context, 24), 0, Ui.dp(context, 24), 0) }
            col.addView(Ui.text(this, sl.tag, 11f, bold = true).apply {
                setTextColor(white70); setBackgroundResource(R.drawable.bs_tag); letterSpacing = 0.12f
                setPadding(Ui.dp(context, 12), Ui.dp(context, 4), Ui.dp(context, 12), Ui.dp(context, 4))
            }, LinearLayout.LayoutParams(-2, -2))
            val title = SpannableString(sl.line + sl.accent).apply {
                setSpan(ForegroundColorSpan(lime), sl.line.length, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
            col.addView(Ui.text(this, title, 34f, R.color.white, serif = true),
                LinearLayout.LayoutParams(-2, -2).apply { topMargin = Ui.dp(this@MainActivity, 14) })
            col.addView(Ui.text(this, sl.sub, 14f, R.color.white).apply { setTextColor(Color.parseColor("#D9FFFFFF")) },
                LinearLayout.LayoutParams(-2, -2).apply { topMargin = Ui.dp(this@MainActivity, 12); bottomMargin = Ui.dp(this@MainActivity, 18) })
            val btns = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            sl.b1?.let { b -> btns.addView(Ui.pill(this, b.first) { b.second() }, LinearLayout.LayoutParams(0, -2, 1f).apply { marginEnd = Ui.dp(this@MainActivity, 8) }) }
            sl.b2?.let { b ->
                btns.addView(Ui.pill(this, b.first, outline = true) { b.second() }.apply {
                    strokeColor = ColorStateList.valueOf(Color.parseColor("#80FFFFFF")); setTextColor(Color.WHITE)
                }, LinearLayout.LayoutParams(0, -2, 1f))
            }
            col.addView(btns, LinearLayout.LayoutParams(-1, -2))
            if (sl.stats.isNotEmpty()) {
                val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setPadding(0, Ui.dp(context, 16), 0, 0) }
                sl.stats.forEach { (num, lbl) ->
                    val c = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
                    c.addView(Ui.text(this, num, 22f, serif = true).apply { setTextColor(lime) })
                    c.addView(Ui.text(this, lbl.uppercase(), 11f).apply { setTextColor(white60); letterSpacing = 0.08f })
                    row.addView(c, LinearLayout.LayoutParams(-2, -2).apply { marginEnd = Ui.dp(this@MainActivity, 28) })
                }
                col.addView(row, LinearLayout.LayoutParams(-1, -2).apply { topMargin = Ui.dp(this@MainActivity, 18) })
            }
            root.addView(col, FrameLayout.LayoutParams(-1, -1))
            flipper.addView(root)
        }
        count = slides.size
        val dots = findViewById<LinearLayout>(R.id.dots); dots.removeAllViews()
        repeat(count) { dots.addView(View(this), LinearLayout.LayoutParams(Ui.dp(this, 32), Ui.dp(this, 3)).apply { marginStart = 5; marginEnd = 5 }) }
        show(0)
        handler.removeCallbacks(tick); handler.postDelayed(tick, 8000)
    }

    private fun show(i: Int) {
        cur = i
        findViewById<ViewFlipper>(R.id.flipper).displayedChild = i
        findViewById<TextView>(R.id.tvCounter).text = "%02d / %02d".format(i + 1, count)
        val dots = findViewById<LinearLayout>(R.id.dots)
        for (k in 0 until dots.childCount) {
            val d = dots.getChildAt(k)
            d.setBackgroundColor(if (k == i) Color.parseColor("#B8E04A") else Color.parseColor("#59FFFFFF"))
            d.layoutParams = (d.layoutParams as LinearLayout.LayoutParams).apply { width = Ui.dp(this@MainActivity, if (k == i) 56 else 32) }
        }
    }

    private fun buildStats(s: StatsResponse, admin: Boolean, large: Boolean) {
        val grid = findViewById<GridLayout>(R.id.grid); grid.removeAllViews()
        val items = mutableListOf(
            Triple("👩‍🎓", s.students.toString(), "Students Registered"),
            Triple("📚", s.programs.toString(), "Programs Running"),
            Triple("🗓️", s.sessions.toString(), "Sessions Scheduled"))
        if (admin) items.add(1, Triple("🙋", s.volunteers.toString(), "Volunteers"))   // volunteers: admin only
        // donation money is for admins only
        if (admin) items.add(Triple("💰", "R " + "%,.2f".format(Locale.US, s.total_donations_amount), "Total Donations"))
        items.add(Triple("📋", s.enrolments.toString(), "Enrolments"))
        items.forEach { (icon, num, label) ->
            val v = layoutInflater.inflate(R.layout.item_stat, grid, false)
            v.findViewById<TextView>(R.id.tvIcon).text = icon
            v.findViewById<TextView>(R.id.tvNum).apply { text = num; textSize = (if (large) 34f else 28f) - (if (num.length > 8) 8f else 0f) }
            v.findViewById<TextView>(R.id.tvLabel).apply { text = label; textSize = if (large) 16f else 13f }
            grid.addView(v)
        }
    }

    private fun buildCards(s: StatsResponse, admin: Boolean, large: Boolean) {
        val cards = findViewById<LinearLayout>(R.id.cards); cards.removeAllViews()
        val big = if (large) 1.15f else 1f
        fun listCard(title: String, rows: List<Pair<String, String>>, empty: String, btn: String, go: () -> Unit) {
            val card = Ui.card(this)
            card.addView(Ui.text(this, title, 20f * big, serif = true))
            if (rows.isEmpty()) card.addView(Ui.text(this, empty, 14f * big, R.color.bs_muted).apply { setPadding(0, Ui.dp(context, 12), 0, 0) })
            rows.forEach { (a, b) ->
                card.addView(Ui.text(this, a, 15f * big, bold = true).apply { setPadding(0, Ui.dp(context, 12), 0, 0) })
                card.addView(Ui.text(this, b, 13f * big, R.color.bs_muted))
            }
            card.addView(Ui.pill(this, btn) { go() }, LinearLayout.LayoutParams(-2, -2).apply { topMargin = Ui.dp(this@MainActivity, 14) })
            cards.addView(card)
        }
        if (admin) {
            listCard("👩‍🎓 Recent Students",
                (s.recent_students ?: emptyList()).map { it.s("FirstName") + " " + it.s("LastName") to it.s("CommunityArea") + " • Enrolled " + Ui.date(it.s("EnrolmentDate")) },
                "No students yet.", "View All →") { module("Students", "get_students.php") }
        }
        // people's names (volunteers) are shown to admins only
        listCard("🗓️ Upcoming Sessions",
            (s.upcoming_sessions ?: emptyList()).map { it.s("ProgramName") to (if (admin) it.s("VolName") + " • " else "") + Ui.date(it.s("SessionDate")) },
            "No sessions scheduled.", "View Programs →") { if (admin) module("Sessions", "get_sessions.php") else module("Programs", "get_programs.php") }
        if (admin) {
            listCard("💰 Recent Donations",
                (s.recent_donations ?: emptyList()).map { it.s("DonorName") to "R " + it.s("DonationAmount") + " • " + Ui.date(it.s("DonationDate")) + " • " + it.s("DonationType") },
                "No donations yet.", "View All") { module("Donations", "get_donations.php") }
        } else {
            val card = Ui.card(this).apply { setBackgroundResource(R.drawable.bs_support) }
            card.addView(Ui.text(this, "💚 Support Bright Start", 20f * big, R.color.white, serif = true))
            card.addView(Ui.text(this, "Every donation helps a child access quality education. Make a contribution today.", 14f * big, R.color.white)
                .apply { setPadding(0, Ui.dp(context, 10), 0, 0) })
            card.addView(Ui.pill(this, "Donate Now →") { open(DonateActivity::class.java) }.apply {
                backgroundTintList = ColorStateList.valueOf(Color.parseColor("#B8E04A")); setTextColor(Color.parseColor("#0F2419"))
            }, LinearLayout.LayoutParams(-2, -2).apply { topMargin = Ui.dp(this@MainActivity, 14) })
            cards.addView(card)
        }
    }
}
