package com.brightstart.app

// Same rules as the website: email format, password 8+ characters with a letter and a number
object Validator {
    fun isValidEmail(e: String) = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$").matches(e.trim())
    fun isValidPassword(p: String) = p.length >= 8 && p.any { it.isLetter() } && p.any { it.isDigit() }
}
