package com.example.data

/**
 * Small heuristics used by [com.example.ui.FinanceViewModel] for routing transactions.
 *
 * Kept in [com.example.data] (one file, no class) so any future ViewModel / tests can
 * reuse them — satisfies AGENT.md §2 (Reuse before you create).
 */
object TransactionHeuristics {

    /** Tokens (lowercase, normalized) that indicate a "salary / income" transaction
     *  across English, Hindi (Devanagari + Latin), and Hinglish phrasing. */
    private val SALARY_TOKENS = listOf(
        "salary", "sal", "payroll", "wages", "income",
        // Hindi / Hinglish
        "वेतन", "तनख्वाह", "vetan", "tankha", "tankhwah", "tankhah",
    )

    private val EMI_TOKENS = listOf("emi", "loan", "instalment", "installment", "किस्त", "kist")

    /**
     * True if the transaction looks like a salary/payroll payment. Both fields are
     * normalized (lowercase + trim) before matching so trailing spaces and case
     * differences don't kill the trigger (regression from the v1 bug).
     */
    fun isSalaryLike(category: String, merchant: String): Boolean {
        val cat = category.trim().lowercase()
        val mer = merchant.trim().lowercase()
        return SALARY_TOKENS.any { token -> cat.contains(token) || mer.contains(token) }
    }

    /** True if the transaction looks like an EMI / loan repayment. */
    fun isEmiLike(category: String, merchant: String): Boolean {
        val cat = category.trim().lowercase()
        val mer = merchant.trim().lowercase()
        return EMI_TOKENS.any { token -> cat.contains(token) || mer.contains(token) }
    }

    /**
     * Find the debt that best matches a transaction merchant. Tries — in order:
     *   1. Exact (case-insensitive, trimmed) match on debt name or lender.
     *   2. Substring match either way: merchant contains debt name, or vice-versa.
     *
     * This replaces the v1 exact-equality match which silently skipped payoff when
     * the user typed a trailing space or slightly different casing.
     */
    fun matchDebt(debts: List<Debt>, merchant: String): Debt? {
        val needle = merchant.trim().lowercase()
        if (needle.isEmpty()) return null
        debts.firstOrNull { d ->
            d.name.trim().lowercase() == needle || d.lender.trim().lowercase() == needle
        }?.let { return it }
        return debts.firstOrNull { d ->
            val dn = d.name.trim().lowercase()
            val ln = d.lender.trim().lowercase()
            (dn.isNotEmpty() && (needle.contains(dn) || dn.contains(needle))) ||
                (ln.isNotEmpty() && (needle.contains(ln) || ln.contains(needle)))
        }
    }
}
