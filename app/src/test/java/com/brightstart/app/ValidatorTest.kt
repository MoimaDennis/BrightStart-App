package com.brightstart.app
import org.junit.Assert.*
import org.junit.Test

class ValidatorTest {
    @Test fun validEmail() = assertTrue(Validator.isValidEmail("a@b.com"))
    @Test fun invalidEmail() = assertFalse(Validator.isValidEmail("abc"))
    @Test fun shortPassword() = assertFalse(Validator.isValidPassword("123"))
    @Test fun noDigitPassword() = assertFalse(Validator.isValidPassword("abcdefgh"))
    @Test fun goodPassword() = assertTrue(Validator.isValidPassword("Password123!"))
}