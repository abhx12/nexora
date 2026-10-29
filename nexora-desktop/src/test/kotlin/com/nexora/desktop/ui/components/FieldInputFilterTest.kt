package com.nexora.desktop.ui.components

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class FieldInputFilterTest {

    @Test
    @DisplayName("Phone input accepts up to 10 digits and rejects more than 10 digits or letters")
    fun phoneInput_rejectsMoreThan10Digits() {
        // Progressive typing
        assertEquals("9", acceptPhoneInput("9"))
        assertEquals("98401", acceptPhoneInput("98401"))
        assertEquals("9840123456", acceptPhoneInput("9840123456"))

        // 11th digit must be rejected
        assertNull(acceptPhoneInput("98401234567"))
        assertNull(acceptPhoneInput("9840123456789"))

        // Letters and symbols must be rejected
        assertNull(acceptPhoneInput("98401abcde"))
        assertNull(acceptPhoneInput("98401-23456"))
        assertNull(acceptPhoneInput("98401 23456"))

        // International prefix with + and up to 12 digits
        assertEquals("+919840123456", acceptPhoneInput("+919840123456"))
        assertNull(acceptPhoneInput("+91984012345678")) // too long
        assertNull(acceptPhoneInput("+919840123abc"))   // letters
    }

    @Test
    @DisplayName("Pincode input accepts strictly 6 digits and rejects 7th digit or letters")
    fun pincodeInput_acceptsStrictly6Digits() {
        assertEquals("631501", acceptPincodeInput("631501"))
        assertEquals("63", acceptPincodeInput("63"))

        // 7th digit rejected
        assertNull(acceptPincodeInput("6315012"))

        // Letters and symbols rejected
        assertNull(acceptPincodeInput("63150A"))
        assertNull(acceptPincodeInput("631-50"))
    }

    @Test
    @DisplayName("GSTIN input accepts 15 alphanumeric uppercase characters and rejects 16th char")
    fun gstinInput_acceptsStrictly15Alphanumeric() {
        val validGstin = "33AAAAA0000A1Z5"
        assertEquals(validGstin, acceptGstinInput("33aaaaa0000a1z5")) // auto uppercase
        assertEquals("33AAAAA", acceptGstinInput("33aaaaa"))

        // 16th char rejected
        assertNull(acceptGstinInput("33AAAAA0000A1Z5X"))

        // Special symbols rejected
        assertNull(acceptGstinInput("33AAAAA0000A1Z@"))
        assertNull(acceptGstinInput("33AAAAA0000A1Z-"))
    }

    @Test
    @DisplayName("Digits input limits length to maxDigits and rejects non-digits")
    fun digitsInput_limitsLength() {
        assertEquals("123", acceptDigitsInput("123", 3))
        assertNull(acceptDigitsInput("1234", 3))
        assertNull(acceptDigitsInput("12a", 3))
    }
}
