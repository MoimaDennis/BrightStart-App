package com.brightstart.app
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import com.google.android.material.button.MaterialButton
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.text.SimpleDateFormat
import java.util.Locale

// Small helpers so every screen looks like the website
fun JsonObject.s(k: String): String = get(k)?.takeIf { !it.isJsonNull }?.asString ?: ""
fun JsonObject.i(k: String): Int = s(k).toIntOrNull() ?: 0

// Loads {"success":true,"data":[...]} from any get_*.php endpoint
suspend fun apiData(path: String): JsonArray =
    JsonParser.parseString(BsApi.api.list(path).string()).asJsonObject.getAsJsonArray("data")

object Ui {
    fun dp(c: Context, v: Int) = (v * c.resources.displayMetrics.density).toInt()
    fun heading(c: Context): Typeface? = ResourcesCompat.getFont(c, R.font.playfair_display)
    fun body(c: Context): Typeface? = ResourcesCompat.getFont(c, R.font.dm_sans)

    fun date(s: String): String = try {
        SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(s)!!)
    } catch (e: Exception) { s }

    fun text(c: Context, t: CharSequence, size: Float = 14f, color: Int = R.color.bs_text, serif: Boolean = false, bold: Boolean = false) =
        TextView(c).apply {
            text = t; textSize = size; setTextColor(c.getColor(color))
            typeface = if (serif) heading(c) else body(c)
            if (bold && !serif) setTypeface(typeface, Typeface.BOLD)
        }

    fun card(c: Context) = LinearLayout(c).apply {
        orientation = LinearLayout.VERTICAL
        setBackgroundResource(R.drawable.bs_card)
        setPadding(dp(c, 20), dp(c, 18), dp(c, 20), dp(c, 18))
        layoutParams = LinearLayout.LayoutParams(-1, -2).apply { setMargins(0, 0, 0, dp(c, 14)) }
    }

    // Pill button: filled lime/green, or outlined
    fun pill(c: Context, label: String, outline: Boolean = false, click: () -> Unit) = MaterialButton(c).apply {
        text = label; isAllCaps = false; cornerRadius = dp(c, 50); typeface = body(c)
        if (outline) {
            backgroundTintList = ColorStateList.valueOf(Color.TRANSPARENT)
            strokeWidth = dp(c, 1); strokeColor = ColorStateList.valueOf(c.getColor(R.color.bs_accent))
            setTextColor(c.getColor(R.color.bs_accent))
        } else {
            backgroundTintList = ColorStateList.valueOf(c.getColor(R.color.bs_accent))
            setTextColor(c.getColor(R.color.bs_on_accent))
        }
        setOnClickListener { click() }
    }
}
