package com.brightstart.app
import android.content.Context

// Remembers the theme so dark/light applies as soon as the app opens
object Session {
    private fun p(c: Context) = c.getSharedPreferences("bs", Context.MODE_PRIVATE)
    fun setDark(c: Context, v: Boolean) = p(c).edit().putBoolean("dark", v).apply()
    fun dark(c: Context) = p(c).getBoolean("dark", true)
}

