package com.nexora.desktop.viewmodel.common

import com.nexora.desktop.ui.components.toDecimalOrNull
import com.nexora.desktop.util.Dates
import com.nexora.desktop.util.s
import java.math.BigDecimal
import java.time.Clock
import java.time.LocalDate

/**
 * Client-side input checks that mirror the API constraints (05 section 1.4), so the user sees a mistake before
 * submitting. They only check what was typed; the API remains the authority. Each returns an error text or null.
 */
object Checks {

    fun required(text: String): String? = if (text.isBlank()) s("validation.required") else null

    /** `Money+` / `Kg+`: present and greater than zero. */
    fun positive(text: String): String? {
        if (text.isBlank()) return s("validation.required")
        val value = text.toDecimalOrNull() ?: return "Invalid numeric format (entered: '$text')."
        return if (value.signum() <= 0) s("validation.positive") else null
    }

    /** `Money0` / `Kg0`: present and zero or more. */
    fun zeroOrMore(text: String): String? {
        if (text.isBlank()) return s("validation.required")
        val value = text.toDecimalOrNull() ?: return "Invalid numeric format (entered: '$text')."
        return if (value.signum() < 0) s("validation.positive") else null
    }

    /** At most [limit] (the remaining / outstanding figure the server sent). */
    fun atMost(text: String, limit: BigDecimal?, message: String): String? {
        val value = text.toDecimalOrNull() ?: return null
        return if (limit != null && value > limit) message else null
    }

    /** A business date: valid, by default not after today, optionally not before [notBefore]. */
    fun date(text: String, clock: Clock, notAfterToday: Boolean = true, notBefore: LocalDate? = null): String? {
        val date = Dates.parseInput(text, clock) ?: return s("validation.date")
        if (notAfterToday && date.isAfter(Dates.today(clock))) return s("validation.notFuture")
        if (notBefore != null && date.isBefore(notBefore)) return s("validation.notBefore", Dates.format(notBefore))
        return null
    }

    /** Validates an Indian mobile number (strictly 10 digits, or 10-12 digits with +). */
    fun phone(text: String, required: Boolean = false): String? {
        if (text.isBlank()) return if (required) s("validation.required") else null
        val trimmed = text.trim()
        val isValid = if (trimmed.startsWith("+")) {
            val rest = trimmed.substring(1)
            rest.all { it.isDigit() } && rest.length in 10..12
        } else {
            trimmed.all { it.isDigit() } && trimmed.length == 10
        }
        return if (!isValid) "Enter a valid 10-digit mobile number (e.g. 9840123456). Entered: '$text'." else null
    }

    /** Validates an Indian 6-digit postal PIN code. */
    fun pincode(text: String, required: Boolean = false): String? {
        if (text.isBlank()) return if (required) s("validation.required") else null
        val trimmed = text.trim()
        val isValid = trimmed.length == 6 && trimmed.all { it.isDigit() }
        return if (!isValid) "Enter a valid 6-digit postal PIN code (e.g. 631501). Entered: '$text'." else null
    }

    /** Validates a 15-character Indian GSTIN. */
    fun gstin(text: String, required: Boolean = false): String? {
        if (text.isBlank()) return if (required) s("validation.required") else null
        val trimmed = text.trim()
        val isValid = trimmed.length == 15 && trimmed.all { it.isLetterOrDigit() }
        return if (!isValid) "Enter a valid 15-character GSTIN (e.g. 33AAAAA0000A1Z5). Entered: '$text'." else null
    }

    /** Collects the non-null problems into the field-name -> message map used by the forms. */
    fun collect(vararg checks: Pair<String, String?>): Map<String, String> =
        checks.mapNotNull { (field, error) -> error?.let { field to it } }.toMap()
}

/** Exact decimal from a field that passed [Checks]; the checks run first, so this is never null there. */
fun String.decimal(): BigDecimal = toDecimalOrNull() ?: BigDecimal.ZERO
