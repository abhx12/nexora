package com.smartledger.app

import org.junit.Assert.assertEquals
import org.junit.Test
import com.smartledger.app.core.util.toIndianRupee
import com.smartledger.app.core.util.toWeightString
import com.smartledger.app.core.util.toDisplayDate

/**
 * Unit tests for Phase 1 — Core utilities.
 * Run with: ./gradlew test
 */
class Phase1UnitTest {

    @Test
    fun `toWeightString formats 3 decimal places`() {
        assertEquals("324.500 kg", 324.5.toWeightString())
        assertEquals("0.000 kg", 0.0.toWeightString())
        assertEquals("42.750 kg", 42.75.toWeightString())
    }

    @Test
    fun `toDisplayDate converts ISO to Indian display format`() {
        assertEquals("01/10/2026", "2026-10-01".toDisplayDate())
        assertEquals("19/10/2026", "2026-10-19".toDisplayDate())
    }

    @Test
    fun `toDisplayDate returns original string on invalid format`() {
        assertEquals("invalid-date", "invalid-date".toDisplayDate())
    }
}
