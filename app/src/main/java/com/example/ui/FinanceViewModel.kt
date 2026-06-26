package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar

class FinanceViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: FinanceRepository

    private val _isProfileLoaded = MutableStateFlow(false)
    val isProfileLoaded: StateFlow<Boolean> = _isProfileLoaded.asStateFlow()

    init {
        val database = AppDatabase.getDatabase(application)
        repository = FinanceRepository(database.financeDao())
        
        // Listen to profile flow to mark it as loaded
        repository.profile
            .onEach { _isProfileLoaded.value = true }
            .launchIn(viewModelScope)
    }

    // Database UI States
    val profile: StateFlow<UserProfile?> = repository.profile
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val accounts: StateFlow<List<Account>> = repository.accounts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val transactions: StateFlow<List<Transaction>> = repository.transactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val bills: StateFlow<List<BillSubscription>> = repository.bills
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val goals: StateFlow<List<FinancialGoal>> = repository.goals
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val debts: StateFlow<List<Debt>> = repository.debts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val reviews: StateFlow<List<WeeklyReview>> = repository.reviews
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Simulated/Real API Keys (can configure via Secrets page)
    private val geminiKey: String
        get() = com.example.BuildConfig.GEMINI_API_KEY

    // Chat Message UI state
    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(
        listOf(
            ChatMessage(
                text = "Welcome to Auren Money Coach. As your private wealth console, I utilize direct data grounds from your accounts to answer queries like: 'What is my safe-to-spend limit today?' or 'Can I afford a ₹5,000 spend?'. Ask me anything.",
                isUser = false
            )
        )
    )
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    private val _isChatLoading = MutableStateFlow(false)
    val isChatLoading: StateFlow<Boolean> = _isChatLoading.asStateFlow()

    // Calculated fields based on reactive DB sources
    val totalUsableBalance: StateFlow<Double> = accounts.map { list ->
        list.filter { it.type != "Credit Card" && it.type != "Investment" }.sumOf { it.balance }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    /**
     * Single source of truth for dashboard widget visibility.
     *
     * Derived from `profile.hiddenWidgets` (CSV). Null profile ⇒ all visible (preserves
     * parity with the pre-onboarding-v3 dashboard so existing users see no change).
     * Call sites do `if (dashboardConfig.value.isVisible(WidgetId.X)) { … }`.
     */
    val dashboardConfig: StateFlow<DashboardConfig> = profile.map { prof ->
        DashboardConfig.fromCsv(prof?.hiddenWidgets)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardConfig.AllVisible)

    val totalDebtOutstanding: StateFlow<Double> = combine(debts, accounts) { dList, aList ->
        val standardDebts = dList.sumOf { it.outstandingAmount }
        val ccBalances = aList.filter { it.type == "Credit Card" }.sumOf { it.balance }
        standardDebts + ccBalances
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val upcomingBillsTotal: StateFlow<Double> = bills.map { list ->
        list.filter { !isBillPaidThisCycle(it) }.sumOf { it.amount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val daysRemainingInCycle: StateFlow<Int> = profile.map { prof ->
        val salaryDay = prof?.salaryDate ?: 1
        getDaysRemaining(salaryDay)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 1)

    val safeToSpendToday: StateFlow<Double> = combine(
        totalUsableBalance,
        profile,
        upcomingBillsTotal,
        debts,
        daysRemainingInCycle
    ) { usableBalance, prof, billsTotal, debtsList, daysRem ->
        val buffer = prof?.safetyBuffer ?: 2000.0
        val remainingMinDebts = debtsList.sumOf { it.minimumPayment } // assume due remaining
        val spendableMoney = (usableBalance - buffer - billsTotal - remainingMinDebts).coerceAtLeast(0.0)
        (spendableMoney / daysRem).coerceAtLeast(0.0)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // Check if the bill is already completed/paid in this cycle
    private fun isBillPaidThisCycle(bill: BillSubscription): Boolean {
        if (bill.lastPaidTimestamp == 0L) return false
        val lastPaidCal = Calendar.getInstance().apply { timeInMillis = bill.lastPaidTimestamp }
        val todayCal = Calendar.getInstance()
        return lastPaidCal.get(Calendar.MONTH) == todayCal.get(Calendar.MONTH) &&
                lastPaidCal.get(Calendar.YEAR) == todayCal.get(Calendar.YEAR)
    }

    private fun getDaysRemaining(salaryDay: Int): Int {
        val calendar = Calendar.getInstance()
        val todayDay = calendar.get(Calendar.DAY_OF_MONTH)
        val maxDays = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        return if (todayDay < salaryDay) {
            salaryDay - todayDay
        } else {
            val endOfThisMonth = maxDays - todayDay
            endOfThisMonth + salaryDay
        }.coerceAtLeast(1)
    }

    // User actions
    fun onboardUser(
        currency: String,
        objective: String,
        mode: String,
        salary: Double,
        payday: Int,
        currentBalance: Double,
        buffer: Double,
        hiddenWidgets: String = ""
    ) {
        viewModelScope.launch {
            // ONE transactional write — see arch critique #2 (race vs follow-up update).
            // The Review step commits everything in a single suspending block.
            val existing = repository.getProfileDirect()
            val merged = (existing ?: UserProfile()).copy(
                currency = currency,
                primaryObjective = objective,
                appMode = mode,
                salaryAmount = salary,
                salaryDate = payday.coerceIn(1, 31),
                safetyBuffer = buffer,
                isOnboarded = true,
                onboardingStep = -1,  // completed sentinel
                hiddenWidgets = hiddenWidgets
            )
            repository.saveProfile(merged)

            // Insert matching opening bank account (only on first onboarding, never on re-onboard).
            if (existing == null || !existing.isOnboarded) {
                val defaultAcc = Account(
                    name = "Primary Bank Account",
                    type = "Savings",
                    balance = currentBalance,
                    institution = "Main Institution"
                )
                repository.insertAccount(defaultAcc)
            }
        }
    }

    /**
     * Persist the in-flight onboarding cursor after every "Continue" tap so process
     * death / rotation lands the user back on the same step. We also persist any
     * partial profile fields the user has entered so far (per arch critique #1) —
     * a single `saveProfile` call, no race window.
     */
    fun persistOnboardingProgress(
        step: Int,
        currency: String? = null,
        objective: String? = null,
        mode: String? = null,
        salary: Double? = null,
        payday: Int? = null,
        buffer: Double? = null,
        hiddenWidgets: String? = null
    ) {
        viewModelScope.launch {
            val current = repository.getProfileDirect() ?: UserProfile()
            repository.saveProfile(
                current.copy(
                    onboardingStep = step,
                    currency = currency ?: current.currency,
                    primaryObjective = objective ?: current.primaryObjective,
                    appMode = mode ?: current.appMode,
                    salaryAmount = salary ?: current.salaryAmount,
                    salaryDate = (payday ?: current.salaryDate).coerceIn(1, 31),
                    safetyBuffer = buffer ?: current.safetyBuffer,
                    hiddenWidgets = hiddenWidgets ?: current.hiddenWidgets
                )
            )
        }
    }

    /** Settings → "Reset setup" — re-enter the onboarding wizard from step 0. */
    fun resetOnboarding() {
        viewModelScope.launch {
            val current = repository.getProfileDirect() ?: UserProfile()
            repository.saveProfile(current.copy(isOnboarded = false, onboardingStep = 0))
        }
    }

    /** Toggle a single dashboard widget on/off. Used by Settings → Dashboard widgets. */
    fun setWidgetVisibility(widget: WidgetId, visible: Boolean) {
        viewModelScope.launch {
            val current = repository.getProfileDirect() ?: UserProfile()
            val cfg = DashboardConfig.fromCsv(current.hiddenWidgets)
            val updated = if (visible) {
                // unhide
                DashboardConfig.fromCsv(cfg.toCsv())
                    .let { c -> if (c.isVisible(widget)) c else c.toggle(widget) }
            } else {
                if (!cfg.isVisible(widget)) cfg else cfg.toggle(widget)
            }
            repository.saveProfile(current.copy(hiddenWidgets = updated.toCsv()))
        }
    }

    fun addAccount(name: String, type: String, initBalance: Double, creditLimit: Double = 0.0, label: String = "") {
        viewModelScope.launch {
            repository.insertAccount(
                Account(
                    name = name,
                    type = type,
                    balance = initBalance,
                    creditLimit = creditLimit,
                    institution = label
                )
            )
        }
    }

    fun deleteAccount(account: Account) {
        viewModelScope.launch {
            repository.deleteAccount(account)
        }
    }

    fun addTransaction(amount: Double, type: String, accountId: Long, category: String, merchant: String, note: String = "", targetAccId: Long? = null, dateOverride: Long? = null, isRecurring: Boolean = false) {
        viewModelScope.launch {
            val trans = Transaction(
                amount = amount,
                type = type,
                date = dateOverride ?: System.currentTimeMillis(),
                accountId = accountId,
                targetAccountId = targetAccId,
                category = category,
                merchant = merchant,
                note = note,
                isRecurring = isRecurring
            )
            repository.insertTransaction(trans)

            // Auto-Save feature: any income whose category or merchant name signals "salary/income",
            // including i18n strings (Hindi/Hinglish: "वेतन", "Vetan", "tankha", etc.).
            if (type == "Income" && TransactionHeuristics.isSalaryLike(category, merchant)) {
                val prof = repository.getProfileDirect()
                if (prof != null && prof.autoSaveEnabled && prof.autoSavePercentage > 0.1 && prof.autoSaveGoalId != null) {
                    val dbGoals = repository.getGoalsDirect()
                    val targetGoal = dbGoals.find { it.id == prof.autoSaveGoalId }
                    if (targetGoal != null) {
                        val autoAmt = amount * (prof.autoSavePercentage / 100.0)
                        if (autoAmt > 0.0) {
                            val updated = targetGoal.copy(currentAmount = targetGoal.currentAmount + autoAmt)
                            repository.updateGoal(updated)

                            val autoTrans = Transaction(
                                amount = autoAmt,
                                type = "Savings",
                                date = dateOverride ?: System.currentTimeMillis(),
                                accountId = accountId,
                                targetAccountId = null,
                                category = "Savings",
                                merchant = "Auto-Save Allocation",
                                note = "Auto-moved ${prof.autoSavePercentage}% toward: ${targetGoal.name}",
                                isRecurring = false
                            )
                            repository.insertTransaction(autoTrans)
                        }
                    }
                }
            }

            // If it's a debt/EMI payment, try to find the matching debt by fuzzy name.
            if (type == "Debt" || TransactionHeuristics.isEmiLike(category, merchant)) {
                val dbDebts = repository.getDebtsDirect()
                val targetDebt = TransactionHeuristics.matchDebt(dbDebts, merchant)
                if (targetDebt != null) {
                    val remaining = (targetDebt.outstandingAmount - amount).coerceAtLeast(0.0)
                    if (remaining == 0.0) {
                        repository.deleteDebt(targetDebt)
                    } else {
                        repository.updateDebt(targetDebt.copy(outstandingAmount = remaining))
                    }
                }
            }
        }
    }

    fun deleteTransaction(transaction: Transaction) {
        viewModelScope.launch {
            repository.deleteTransaction(transaction)
        }
    }

    fun updateBill(bill: BillSubscription) {
        viewModelScope.launch {
            repository.updateBill(bill)
        }
    }

    fun addBill(name: String, amount: Double, dueDate: Int, category: String, isSub: Boolean, accountId: Long = 0) {
        viewModelScope.launch {
            repository.insertBill(
                BillSubscription(
                    name = name,
                    amount = amount,
                    dueDate = dueDate,
                    category = category,
                    isSubscription = isSub,
                    accountId = accountId
                )
            )
        }
    }

    fun payBill(bill: BillSubscription, accountId: Long) {
        viewModelScope.launch {
            val updated = bill.copy(lastPaidTimestamp = System.currentTimeMillis(), accountId = accountId)
            repository.updateBill(updated)

            // Subtract amount from account
            addTransaction(
                amount = bill.amount,
                type = "Expense",
                accountId = accountId,
                category = bill.category,
                merchant = bill.name,
                note = "Paid Recurring Bill: ${bill.name}"
            )
        }
    }

    fun deleteBill(bill: BillSubscription) {
        viewModelScope.launch {
            repository.deleteBill(bill)
        }
    }

    fun addGoal(name: String, targetAmount: Double, currentAmount: Double, targetMonths: Int, priority: String, category: String) {
        viewModelScope.launch {
            val targetCal = Calendar.getInstance().apply {
                add(Calendar.MONTH, targetMonths)
            }
            val monthly = if (targetMonths > 0) (targetAmount - currentAmount) / targetMonths else 0.0
            repository.insertGoal(
                FinancialGoal(
                    name = name,
                    targetAmount = targetAmount,
                    currentAmount = currentAmount,
                    targetDate = targetCal.timeInMillis,
                    priority = priority,
                    monthlyContribution = monthly,
                    category = category
                )
            )
        }
    }

    fun addFundsToGoal(goal: FinancialGoal, amount: Double, accountId: Long) {
        viewModelScope.launch {
            val updated = goal.copy(currentAmount = goal.currentAmount + amount)
            repository.updateGoal(updated)

            // Create Transaction
            addTransaction(
                amount = amount,
                type = "Savings",
                accountId = accountId,
                category = "Savings",
                merchant = "Goal: ${goal.name}",
                note = "Saved toward goal"
            )
        }
    }

    fun deleteGoal(goal: FinancialGoal) {
        viewModelScope.launch {
            repository.deleteGoal(goal)
        }
    }

    fun addDebt(name: String, type: String, outstanding: Double, rate: Double, minPay: Double, emi: Double, due: Int, months: Int, lender: String) {
        viewModelScope.launch {
            repository.insertDebt(
                Debt(
                    name = name,
                    type = type,
                    outstandingAmount = outstanding,
                    interestRate = rate,
                    minimumPayment = minPay,
                    emiAmount = emi,
                    dueDate = due,
                    remainingTenure = months,
                    lender = lender
                )
            )
        }
    }

    fun deleteDebt(debt: Debt) {
        viewModelScope.launch {
            repository.deleteDebt(debt)
        }
    }

    fun completeWeeklyCheckIn(wentWell: String, reason: String, categoryExceeded: String, auditOffset: Double, leak: String, action: String) {
        viewModelScope.launch {
            repository.insertReview(
                WeeklyReview(
                    timestamp = System.currentTimeMillis(),
                    positiveAchievement = wentWell,
                    overspendingReason = reason,
                    categoryExceeded = categoryExceeded,
                    adjustedBudgetAmount = auditOffset,
                    leakToMonitor = leak,
                    focusAction = action
                )
            )
        }
    }

    fun updateProfile(currency: String, objective: String, mode: String, salary: Double, buffer: Double, isCompleted: Boolean) {
        viewModelScope.launch {
            val current = repository.getProfileDirect() ?: UserProfile()
            repository.saveProfile(
                current.copy(
                    currency = currency,
                    primaryObjective = objective,
                    appMode = mode,
                    salaryAmount = salary,
                    safetyBuffer = buffer,
                    isOnboarded = isCompleted
                )
            )
        }
    }

    fun updateAutoSaveConfig(enabled: Boolean, percentage: Double, goalId: Long?) {
        viewModelScope.launch {
            val current = repository.getProfileDirect() ?: UserProfile()
            repository.saveProfile(
                current.copy(
                    autoSaveEnabled = enabled,
                    autoSavePercentage = percentage,
                    autoSaveGoalId = goalId
                )
            )
        }
    }

    private val _aiInsights = MutableStateFlow<String?>(null)
    val aiInsights: StateFlow<String?> = _aiInsights.asStateFlow()

    private val _isInsightsLoading = MutableStateFlow(false)
    val isInsightsLoading: StateFlow<Boolean> = _isInsightsLoading.asStateFlow()

    fun generateSpendingInsights() {
        viewModelScope.launch {
            _isInsightsLoading.value = true
            try {
                val txs = repository.getTransactionsDirect().filter { it.type.lowercase() == "expense" }
                val totalSpent = txs.sumOf { it.amount }
                val categoryGroups = txs.groupBy { it.category }.mapValues { it.value.sumOf { v -> v.amount } }
                
                val prof = repository.getProfileDirect()
                val targetSavings = (prof?.salaryAmount ?: 60000.0) * 0.15
                val currency = prof?.currency ?: "₹"
                
                val promptText = """
                    Review the following monthly expense transaction dataset and profile targets.
                    Total Expense: ${currency}${totalSpent}.
                    Category breakdown: ${categoryGroups.map { "${it.key}: ${currency}${it.value}" }.joinToString()}
                    Annual Net Income: ${currency}${prof?.salaryAmount ?: 60000.0}
                    Recommended target savings rate: 15% (${currency}${targetSavings}).
                    
                    Analyze their spending habits. Output precisely 3 highly pragmatic, bulleted suggestions of specific areas or categories to cut back on to save money. Be direct, professional, use elegant phrasing, and reference actual categories/numbers from the data. Keep the suggestions short and compact.
                """.trimIndent()

                val response = RetrofitClient.service.generateContent(
                    apiKey = geminiKey,
                    request = GenerateContentRequest(
                        contents = listOf(Content(parts = listOf(Part(text = promptText)))),
                        systemInstruction = Content(parts = listOf(Part(text = "You are Auren's expert AI Spending Auditor. Analyze the metrics cleanly and return 3 high-impact saving directives.")), role = "system")
                    )
                )

                val aiResult = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                if (!aiResult.isNullOrBlank()) {
                    _aiInsights.value = aiResult
                } else {
                    _aiInsights.value = getDefaultLocalInsights(categoryGroups, currency)
                }
            } catch (e: Exception) {
                val txs = repository.getTransactionsDirect().filter { it.type.lowercase() == "expense" }
                val categoryGroups = txs.groupBy { it.category }.mapValues { it.value.sumOf { v -> v.amount } }
                val prof = repository.getProfileDirect()
                val currency = prof?.currency ?: "₹"
                _aiInsights.value = getDefaultLocalInsights(categoryGroups, currency)
            } finally {
                _isInsightsLoading.value = false
            }
        }
    }

    private fun getDefaultLocalInsights(categoryGroups: Map<String, Double>, currency: String): String {
        val shoppingValue = categoryGroups["Shopping"] ?: 0.0
        val diningValue = categoryGroups["Dining"] ?: 0.0
        
        val list = mutableListOf<String>()
        list.add("• **Optimize Dining spend**: Flexible outlays on Dining out (${MoneyFormat.compact(diningValue, currency)}) represent an immediate friction point. Trim dining spend by 15% to reinforce the safety buffer.")
        list.add("• **Rationalize Discretionary Shopping**: Total shopping outlays reached ${MoneyFormat.compact(shoppingValue, currency)}. Setting a weekly limit of ${MoneyFormat.compact(shoppingValue * 0.5, currency)} would secure immediate safety cushions.")
        list.add("• **Establish Direct Separation**: Automate your savings rate first. Diverting an automatic percentage of incoming income directly to your targeted savings goals removes decision friction.")
        return list.joinToString("\n")
    }

    // AI Money Coach Interaction grounded on Room Database content.
    fun askAiCoach(userQuestion: String) {
        if (userQuestion.isBlank()) return

        // Append user question
        _chatMessages.update { list -> list + ChatMessage(text = userQuestion, isUser = true) }
        _isChatLoading.value = true

        viewModelScope.launch {
            try {
                val prof = repository.getProfileDirect()
                val accs = repository.getAccountsDirect()
                val txs = repository.getTransactionsDirect().take(15) // take 15 recent
                val bls = repository.getBillsDirect()
                val gls = repository.getGoalsDirect()
                val dbs = repository.getDebtsDirect()

                val currency = prof?.currency ?: "₹"
                val mode = prof?.appMode ?: "Strict Mode"
                val objective = prof?.primaryObjective ?: "Control spending"
                val currentSafe = safeToSpendToday.value

                // Format state details elegantly as text prompt context
                val sb = StringBuilder()
                sb.append("System Settings: Mode = $mode, Main Objective = $objective\n")
                sb.append("Core Wallet state:\n")
                sb.append("- Safe to Spend Limit TODAY: $currency$currentSafe\n")
                sb.append("- Total Usable Cash Balance: $currency${totalUsableBalance.value}\n")
                sb.append("- Total Outstanding Indebtness: $currency${totalDebtOutstanding.value}\n")
                
                sb.append("Accounts configuration:\n")
                accs.forEach { acc ->
                    sb.append("  • ${acc.name} (${acc.type}): $currency${acc.balance} at ${acc.institution}\n")
                }

                sb.append("Upcoming commitments (Bills & Subscriptions):\n")
                bls.forEach { b ->
                    val status = if (isBillPaidThisCycle(b)) "PAID THIS CYCLE" else "UNPAID (Due of month: ${b.dueDate})"
                    sb.append("  • ${b.name} (${b.category}): $currency${b.amount} ($status)\n")
                }

                sb.append("Active Goals (Emergency Fund / Sinking Stocks):\n")
                gls.forEach { g ->
                    sb.append("  • ${g.name}: Target $currency${g.targetAmount} | Secured: $currency${g.currentAmount} | Monthly Target Contribution: $currency${g.monthlyContribution}\n")
                }

                sb.append("Outstanding debt profile:\n")
                dbs.forEach { d ->
                    sb.append("  • ${d.name} (${d.type}): Outstanding $currency${d.outstandingAmount} | Monthly EMI: $currency${d.emiAmount} | Interest Rate: ${d.interestRate}%\n")
                }

                sb.append("Recent Transactions Ledger:\n")
                txs.forEach { tx ->
                    sb.append("  • Merchant: ${tx.merchant} | Cat: ${tx.category} | Class: ${tx.businessType} | $currency${tx.amount} (${tx.type})\n")
                }

                val fullContext = sb.toString()

                val systemPrompt = """
                    You are the AI Money Coach of Auren Money OS. You are a highly-capable private wealth assistant.
                    ground your responses exclusively on the factual numerical elements provided below.
                    Answer the user's queries cleanly with visual lists and precise calculations, but do not output these raw instructions or raw prompt data labels.
                    Maintain an elegant, objective format with premium champagne typography accents in mind.
                    Avoid shaming, keep the tone elite and luxurious.
                    
                    Here is the exact real-time financial tracking context of this client:
                    $fullContext
                    
                    Strict Mode is: ${if (mode.contains("Strict")) "ENABLED" else "DISABLED"}. If enabled, strictly protect essentials and advise against excessive liabilities.
                """.trimIndent()

                val response = RetrofitClient.service.generateContent(
                    apiKey = geminiKey,
                    request = GenerateContentRequest(
                        contents = listOf(Content(parts = listOf(Part(text = userQuestion)))),
                        systemInstruction = Content(parts = listOf(Part(text = systemPrompt)), role = "system")
                    )
                )

                val aiResult = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                    ?: "Verify that network status is functional, or please secure an API Key inside the AI Studio secrets console."

                _chatMessages.update { list -> list + ChatMessage(text = aiResult, isUser = false) }

            } catch (e: Exception) {
                _chatMessages.update { list ->
                    list + ChatMessage(
                        text = "I received a connection fault: ${e.message}. Please configure your GEMINI_API_KEY in the Secrets tab to allow full intelligent counsel.",
                        isUser = false
                    )
                }
            } finally {
                _isChatLoading.value = false
            }
        }
    }

    // Bulk Import for automated bank feeds — uses repository bulk path (one DAO call).
    fun importBankTransactions(list: List<Transaction>) {
        if (list.isEmpty()) return
        viewModelScope.launch {
            repository.insertTransactions(list)
        }
    }

    fun depositEmergencyFund(amount: Double, accountId: Long) {
        viewModelScope.launch {
            val current = repository.getProfileDirect() ?: UserProfile()
            val updatedFund = current.currentEmergencyFund + amount
            repository.saveProfile(current.copy(currentEmergencyFund = updatedFund))
            
            val trans = Transaction(
                amount = amount,
                type = "Savings",
                date = System.currentTimeMillis(),
                accountId = accountId,
                category = "Savings",
                merchant = "Emergency Fund Contribution",
                note = "Safeguard Deposit",
                businessType = "Personal"
            )
            repository.insertTransaction(trans)
        }
    }

}

data class ChatMessage(
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)
