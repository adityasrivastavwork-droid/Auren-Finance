package com.example.data

import java.text.NumberFormat
import java.util.Locale
import kotlin.math.absoluteValue

/**
 * Locale-aware money formatting for Auren.
 *
 * Per AGENT.md §2 (Reuse before you create): every screen MUST format currency through
 * [MoneyFormat.compact] or [MoneyFormat.full]. Hand-rolled `String.format("%,.0f", …)`
 * is forbidden — it produced the `"%,.0f2"` regression and ignores the Indian
 * 2-2-3 numbering convention (1,00,000 instead of 100,000).
 *
 * The currency *symbol* still comes from [com.example.data.UserProfile.currency] so we
 * keep backward compatibility with the existing `"$currency<amount>"` usages; we only
 * format the amount portion locale-aware.
 */
object MoneyFormat {

    /** "1,234.56" / "1,23,456.78" (Indian grouping when symbol is ₹). */
    fun amount(value: Double, currencySymbol: String): String {
        val locale = localeFor(currencySymbol)
        val nf = NumberFormat.getNumberInstance(locale).apply {
            minimumFractionDigits = if (value.absoluteValue >= 1000.0) 0 else 2
            maximumFractionDigits = 2
            isGroupingUsed = true
        }
        return nf.format(value)
    }

    /** "₹1,23,456" — no decimals; for headline numbers in the dashboard grid. */
    fun compact(value: Double, currencySymbol: String): String {
        val locale = localeFor(currencySymbol)
        val nf = NumberFormat.getNumberInstance(locale).apply {
            minimumFractionDigits = 0
            maximumFractionDigits = 0
            isGroupingUsed = true
        }
        return "$currencySymbol${nf.format(value)}"
    }

    /** "₹1,23,456.78" — two decimals; for precise figures like Safe-To-Spend. */
    fun full(value: Double, currencySymbol: String): String {
        val locale = localeFor(currencySymbol)
        val nf = NumberFormat.getNumberInstance(locale).apply {
            minimumFractionDigits = 2
            maximumFractionDigits = 2
            isGroupingUsed = true
        }
        return "$currencySymbol${nf.format(value)}"
    }

    /**
     * Choose a Locale based on the currency symbol the user picked in onboarding.
     * Defaults to en-IN for ₹ so grouping is 2-2-3.
     */
    private fun localeFor(currencySymbol: String): Locale = when (currencySymbol) {
        "₹" -> Locale("en", "IN")
        "$" -> Locale.US
        "€" -> Locale.GERMANY
        "£" -> Locale.UK
        "¥" -> Locale.JAPAN
        else -> Locale.getDefault()
    }
}
