package com.example.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Regression tests for [DashboardConfig] — the CSV-backed widget visibility model
 * introduced with v3 onboarding. Pins the AGENT.md §2 invariant: adding/removing
 * widgets must NOT require schema or call-site churn.
 */
class DashboardConfigTest {

    @Test
    fun `empty CSV means all visible (parity with pre-onboarding-v3 default)`() {
        val cfg = DashboardConfig.fromCsv("")
        WidgetId.values().forEach { w ->
            assertTrue("expected ${w.key} visible by default", cfg.isVisible(w))
        }
        assertTrue(cfg.hasAnyVisible)
    }

    @Test
    fun `null CSV is treated as all visible`() {
        val cfg = DashboardConfig.fromCsv(null)
        assertEquals(DashboardConfig.AllVisible, cfg)
    }

    @Test
    fun `toggle hides then unhides the widget`() {
        var cfg = DashboardConfig.AllVisible
        cfg = cfg.toggle(WidgetId.MonetaryMatrix)
        assertFalse(cfg.isVisible(WidgetId.MonetaryMatrix))
        cfg = cfg.toggle(WidgetId.MonetaryMatrix)
        assertTrue(cfg.isVisible(WidgetId.MonetaryMatrix))
    }

    @Test
    fun `toCsv round-trips through fromCsv`() {
        val hidden = setOf(WidgetId.MonetaryMatrix, WidgetId.BankFeedSync)
        var cfg = DashboardConfig.AllVisible
        hidden.forEach { cfg = cfg.toggle(it) }
        val restored = DashboardConfig.fromCsv(cfg.toCsv())
        hidden.forEach { assertFalse(restored.isVisible(it)) }
        WidgetId.values().filter { it !in hidden }.forEach { assertTrue(restored.isVisible(it)) }
    }

    @Test
    fun `fromCsv ignores unknown keys (forward-compatible)`() {
        // Useful for downgrade/upgrade safety: a v4 widget id stored by a newer build
        // must not crash a v3 reader.
        val cfg = DashboardConfig.fromCsv("monetary_matrix,unknown_v99_widget,bank_feed")
        assertFalse(cfg.isVisible(WidgetId.MonetaryMatrix))
        assertFalse(cfg.isVisible(WidgetId.BankFeedSync))
        // No exception, and other widgets remain visible.
        assertTrue(cfg.isVisible(WidgetId.SpendingTrendsChart))
    }

    @Test
    fun `hasAnyVisible flips to false only when every widget is hidden`() {
        var cfg = DashboardConfig.AllVisible
        WidgetId.values().forEach { cfg = cfg.toggle(it) }
        assertFalse("expected no widget visible after hiding all", cfg.hasAnyVisible)
        // Restore one
        cfg = cfg.toggle(WidgetId.MonetaryMatrix)
        assertTrue(cfg.hasAnyVisible)
    }

    @Test
    fun `fromCsv tolerates leading and trailing whitespace per key`() {
        val cfg = DashboardConfig.fromCsv("  monetary_matrix , bank_feed  ")
        assertFalse(cfg.isVisible(WidgetId.MonetaryMatrix))
        assertFalse(cfg.isVisible(WidgetId.BankFeedSync))
    }
}
