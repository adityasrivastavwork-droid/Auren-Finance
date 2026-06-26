package com.example.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Regression tests for [TransactionHeuristics].
 *
 * The v1 bugs these guard against:
 *   - "salary" exact match → autosave never fired for Hindi/Hinglish users.
 *   - Trailing-space / case mismatch → debt payoff silently skipped.
 */
class TransactionHeuristicsTest {

    @Test
    fun `isSalaryLike matches English category`() {
        assertTrue(TransactionHeuristics.isSalaryLike("Salary", "ACME Corp"))
        assertTrue(TransactionHeuristics.isSalaryLike("salary", "ACME"))
        assertTrue(TransactionHeuristics.isSalaryLike("Income", "Payroll Deposit"))
    }

    @Test
    fun `isSalaryLike tolerates trailing whitespace and case`() {
        // Regression: v1 used `== "salary"` so "Salary " missed.
        assertTrue(TransactionHeuristics.isSalaryLike("  SALARY  ", "X"))
        assertTrue(TransactionHeuristics.isSalaryLike("X", "Monthly Salary "))
    }

    @Test
    fun `isSalaryLike works for Hindi and Hinglish tokens`() {
        assertTrue(TransactionHeuristics.isSalaryLike("वेतन", "HDFC"))
        assertTrue(TransactionHeuristics.isSalaryLike("तनख्वाह", "HDFC"))
        assertTrue(TransactionHeuristics.isSalaryLike("Vetan", "HDFC"))
        assertTrue(TransactionHeuristics.isSalaryLike("Tankhwah", "HDFC"))
    }

    @Test
    fun `isSalaryLike rejects unrelated tx`() {
        assertEquals(false, TransactionHeuristics.isSalaryLike("Shopping", "Amazon"))
        assertEquals(false, TransactionHeuristics.isSalaryLike("Dining", "Swiggy"))
    }

    @Test
    fun `isEmiLike matches EMI variants`() {
        assertTrue(TransactionHeuristics.isEmiLike("EMI", "HDFC Loan"))
        assertTrue(TransactionHeuristics.isEmiLike("loan", "ICICI"))
        assertTrue(TransactionHeuristics.isEmiLike("installment", "X"))
        assertTrue(TransactionHeuristics.isEmiLike("Kist", "Lender"))
    }

    @Test
    fun `matchDebt exact name match wins`() {
        val debts = listOf(
            debt(name = "HDFC Personal Loan", lender = "HDFC"),
            debt(name = "Car EMI", lender = "ICICI")
        )
        val hit = TransactionHeuristics.matchDebt(debts, "HDFC Personal Loan")
        assertNotNull(hit)
        assertEquals("HDFC Personal Loan", hit!!.name)
    }

    @Test
    fun `matchDebt tolerates trailing whitespace`() {
        // Regression: v1 used `==` so "HDFC " would silently skip payoff.
        val debts = listOf(debt(name = "HDFC", lender = "HDFC"))
        val hit = TransactionHeuristics.matchDebt(debts, "  hdfc  ")
        assertNotNull(hit)
    }

    @Test
    fun `matchDebt falls back to substring`() {
        val debts = listOf(debt(name = "HDFC Personal Loan", lender = "HDFC"))
        val hit = TransactionHeuristics.matchDebt(debts, "EMI HDFC Personal")
        assertNotNull(hit)
    }

    @Test
    fun `matchDebt returns null for empty merchant`() {
        val debts = listOf(debt(name = "HDFC", lender = "HDFC"))
        assertNull(TransactionHeuristics.matchDebt(debts, "   "))
    }

    @Test
    fun `matchDebt returns null when no candidate matches`() {
        val debts = listOf(debt(name = "HDFC", lender = "HDFC"))
        assertNull(TransactionHeuristics.matchDebt(debts, "Some Coffee Shop"))
    }

    private fun debt(name: String, lender: String) = Debt(
        name = name,
        type = "Personal Loan",
        outstandingAmount = 1000.0,
        interestRate = 10.0,
        minimumPayment = 100.0,
        emiAmount = 100.0,
        dueDate = 1,
        remainingTenure = 12,
        lender = lender
    )
}
