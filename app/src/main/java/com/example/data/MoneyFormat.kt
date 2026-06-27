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
        if (currencySymbol == "₹") {
            val fractionDigits = if (value.absoluteValue >= 1000.0) 0 else 2
            return formatIndianGrouping(value, fractionDigits, fractionDigits > 0)
        }
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
        if (currencySymbol == "₹") {
            return "₹${formatIndianGrouping(value, 0, false)}"
        }
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
        if (currencySymbol == "₹") {
            return "₹${formatIndianGrouping(value, 2, true)}"
        }
        val locale = localeFor(currencySymbol)
        val nf = NumberFormat.getNumberInstance(locale).apply {
            minimumFractionDigits = 2
            maximumFractionDigits = 2
            isGroupingUsed = true
        }
        return "$currencySymbol${nf.format(value)}"
    }

    /**
     * Formats a double with Indian numbering system (2-2-3 grouping):
     * e.g., 100000 -> 1,00,000
     */
    private fun formatIndianGrouping(value: Double, fractionDigits: Int, forceDecimals: Boolean): String {
        val isNegative = value < 0
        val absValue = value.absoluteValue
        
        val formatStr = if (fractionDigits == 0 && !forceDecimals) {
            String.format(Locale.US, "%.0f", absValue)
        } else {
            String.format(Locale.US, "%.${fractionDigits}f", absValue)
        }
        
        val parts = formatStr.split(".")
        val intPart = parts[0]
        val decPart = if (parts.size > 1) parts[1] else ""
        
        val formattedInt = if (intPart.length <= 3) {
            intPart
        } else {
            val last3 = intPart.substring(intPart.length - 3)
            val rest = intPart.substring(0, intPart.length - 3)
            val groupedRest = StringBuilder()
            var count = 0
            for (i in rest.length - 1 downTo 0) {
                if (count > 0 && count % 2 == 0) {
                    groupedRest.insert(0, ',')
                }
                groupedRest.insert(0, rest[i])
                count++
            }
            "$groupedRest,$last3"
        }
        
        val finalDec = if (forceDecimals || (decPart.isNotEmpty() && fractionDigits > 0)) {
            val paddedDec = decPart.padEnd(fractionDigits, '0').substring(0, fractionDigits)
            if (paddedDec.isNotEmpty()) ".$paddedDec" else ""
        } else {
            ""
        }
        
        val sign = if (isNegative) "-" else ""
        return "$sign$formattedInt$finalDec"
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
