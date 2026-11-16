package com.brightstart.app
import android.content.Context

// Remembers the theme and who is logged in (role decides which buttons show)
object Session {
    private fun p(c: Context) = c.getSharedPreferences("bs", Context.MODE_PRIVATE)
    fun setDark(c: Context, v: Boolean) = p(c).edit().putBoolean("dark", v).apply()
    fun dark(c: Context) = p(c).getBoolean("dark", true)

    fun saveUser(c: Context, name: String?, email: String?, role: String?) =
        p(c).edit().putString("name", name ?: "").putString("email", email ?: "").putString("role", role ?: "").apply()
    fun clearUser(c: Context) = p(c).edit().remove("name").remove("email").remove("role").remove("helper").apply()
    // Programs where this Coordinator was allocated as helper staff by an admin
    fun setHelperPrograms(c: Context, ids: List<Int>) = p(c).edit().putString("helper", ids.joinToString(",")).apply()
    fun helperPrograms(c: Context): Set<Int> = (p(c).getString("helper", "") ?: "").split(",").mapNotNull { it.toIntOrNull() }.toSet()
    fun isHelper(c: Context) = helperPrograms(c).isNotEmpty()
    fun name(c: Context) = p(c).getString("name", "") ?: ""
    fun email(c: Context) = p(c).getString("email", "") ?: ""
    fun role(c: Context) = p(c).getString("role", "") ?: ""
    fun isAdmin(c: Context) = role(c) == "Admin"
}
