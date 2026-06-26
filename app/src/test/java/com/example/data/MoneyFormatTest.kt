package com.example.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Regression tests for [MoneyFormat]. The v1 codebase had `String.format("%,.0f2", x)`
 * which is invalid — either throws or appends a literal "2". These tests pin the
 * correct, locale-aware output.
 */
class MoneyFormatTest {

    @Test
    fun `compact uses Indian grouping for rupee symbol`() {
        // 1,00,000 (Indian 2-2-3) — NOT 100,000.
        assertEquals("₹1,00,000", MoneyFormat.compact(100_000.0, "₹"))
        assertEquals("₹12,34,567", MoneyFormat.compact(1_234_567.0, "₹"))
    }

    @Test
    fun `compact uses thousands grouping for USD`() {
        assertEquals("$100,000", MoneyFormat.compact(100_000.0, "$"))
        assertEquals("$1,234,567", MoneyFormat.compact(1_234_567.0, "$"))
    }

    @Test
    fun `full prints two decimals`() {
        val rupees = MoneyFormat.full(1234.5, "₹")
        assertEquals("₹1,234.50", rupees)
    }

    @Test
    fun `compact drops decimals`() {
        // 1234.5 ⇒ "₹1,235" (rounded; HALF_EVEN default is fine for headline numbers)
        val v = MoneyFormat.compact(1234.5, "₹")
        assertTrue("expected rounded compact form, got $v", v.startsWith("₹1,23") && !v.contains("."))
    }

    @Test
    fun `amount with no symbol returns just the number`() {
        val v = MoneyFormat.amount(1234.5, "₹")
        // Should not start with currency symbol — caller composes it
        assertFalse(v.contains("₹"))
        assertTrue(v.contains("1,234"))
    }

    @Test
    fun `negative values render with sign`() {
        val v = MoneyFormat.full(-2500.75, "₹")
        assertTrue("expected leading minus, got $v", v.contains("-2,500.75"))
    }
}
