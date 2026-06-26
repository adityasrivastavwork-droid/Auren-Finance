package com.example.data

/**
 * Canonical id list for every gateable widget on the home dashboard.
 *
 * Per AGENT.md §2 (Reuse before you create): adding a new widget = add ONE entry here,
 * one card in `Customize Dashboard` onboarding step, and a single `isVisible(...)` check
 * at the call site. No schema migration. No new `Boolean` column. No 9-line if-tree
 * spread across HomeScreen.
 */
enum class WidgetId(val key: String, val label: String, val description: String) {
    MonetaryMatrix(
        key = "monetary_matrix",
        label = "Monetary Matrix",
        description = "Salary · Bills · Locked Savings · Safety Reserve 2×2 grid"
    ),
    PredictiveSavings(
        key = "predictive_eom",
        label = "Predictive end-of-month",
        description = "Forecast of where your savings will land at payday"
    ),
    SpendingTrendsChart(
        key = "trends_30d",
        label = "30-day spending trends",
        description = "Interactive line chart of daily spend"
    ),
    SpendingVsSavingsChart(
        key = "spend_vs_save",
        label = "Spending vs savings",
        description = "Geometric breakdown of where your income goes"
    ),
    WeeklyCheckIn(
        key = "weekly_checkin",
        label = "Weekly check-in",
        description = "Reflect on last week's wins and overspends"
    ),
    BankFeedSync(
        key = "bank_feed",
        label = "Bank feed sync",
        description = "Bulk-import bank SMS / CSV transactions"
    ),
    RecurringBills(
        key = "bills",
        label = "Protected bills",
        description = "Upcoming bills and subscriptions queued for the cycle"
    ),
    AccountsList(
        key = "accounts",
        label = "Accounts",
        description = "Every bank, credit card, wallet, and investment account"
    ),
    CommitmentTimeline(
        key = "timeline",
        label = "Commitment timeline",
        description = "Bills + EMIs ordered by due date this cycle"
    );

    companion object {
        fun fromKey(k: String): WidgetId? = values().firstOrNull { it.key == k }
    }
}

/**
 * Read model for "which widgets should HomeScreen actually render".
 *
 * Derived ONCE in [com.example.ui.FinanceViewModel] from the user profile and exposed as
 * a `StateFlow`. Call sites do `if (config.isVisible(WidgetId.MonetaryMatrix)) { ... }`
 * — no nullable profile checks at the call site, no `!!`, and a null profile yields
 * the all-visible default (parity with the pre-onboarding-v3 dashboard).
 */
data class DashboardConfig(private val hidden: Set<WidgetId>) {
    fun isVisible(widget: WidgetId): Boolean = widget !in hidden
    fun toggle(widget: WidgetId): DashboardConfig =
        if (widget in hidden) copy(hidden = hidden - widget) else copy(hidden = hidden + widget)
    fun toCsv(): String = hidden.joinToString(",") { it.key }

    /** True when at least one widget is still visible (used by the onboarding validator). */
    val hasAnyVisible: Boolean get() = hidden.size < WidgetId.values().size

    companion object {
        val AllVisible = DashboardConfig(emptySet())
        fun fromCsv(csv: String?): DashboardConfig {
            if (csv.isNullOrBlank()) return AllVisible
            val set = csv.split(',')
                .mapNotNull { WidgetId.fromKey(it.trim()) }
                .toSet()
            return DashboardConfig(set)
        }
    }
}
