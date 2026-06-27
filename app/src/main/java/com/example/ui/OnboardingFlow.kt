package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.Saver
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.data.DashboardConfig
import com.example.data.UserProfile
import com.example.data.WidgetId
import com.example.ui.theme.LuxBlack
import com.example.ui.theme.LuxCardGray
import com.example.ui.theme.LuxDarkGray
import com.example.ui.theme.LuxGoldChange
import com.example.ui.theme.LuxGoldLight
import com.example.ui.theme.LuxIvory
import com.example.ui.theme.LuxMuted
import com.example.ui.theme.Typography

/* ─────────────────────── Entry models ─────────────────────── */

data class AccountEntry(
    val name: String,
    val type: String,
    val balance: Double,
    val isPrimary: Boolean,
    val creditLimit: Double = 0.0
)

data class RecurringEntry(
    val name: String,
    val amount: Double,
    val dueDay: Int,
    val category: String,
    val isSubscription: Boolean
)

/* ─────────────────────── Step constants ─────────────────────── */

private const val STEP_WELCOME = 0
private const val STEP_OBJECTIVE = 1
private const val STEP_BUDGET_MODE = 2
private const val STEP_INCOME = 3
private const val STEP_FOUNDATION = 4
private const val STEP_ACCOUNTS = 5
private const val STEP_RECURRING = 6
private const val STEP_DASHBOARD = 7
private const val STEP_REVIEW = 8
private const val STEP_COUNT = 9

/**
 * Multi-step onboarding wizard for Auren Money OS.
 *
 * Eight steps:
 *   0. Welcome — brand reveal, no inputs.
 *   1. Objective + Mode — strategy.
 *   2. Income — currency, salary, payday (with type selector + working days).
 *   3. Foundation — opening balance + safety buffer.
 *   4. Accounts — add bank accounts, designate primary.
 *   5. Recurring — EMIs, rent, subscriptions (skippable).
 *   6. Customize Dashboard — per-widget toggle + drag-to-reorder.
 *   7. Review — confirm every choice, edit any row by tap.
 */
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun OnboardingFlow(
    profile: UserProfile?,
    viewModel: FinanceViewModel,
    onComplete: (
        currency: String,
        objective: String,
        mode: String,
        salary: Double,
        payday: Int,
        balance: Double,
        buffer: Double,
        hiddenWidgets: String,
        accounts: List<AccountEntry>,
        recurringItems: List<RecurringEntry>,
        widgetOrder: String
    ) -> Unit
) {
    val initialStep = (profile?.onboardingStep ?: 0).coerceIn(0, STEP_COUNT - 1)
    var step by rememberSaveable { mutableStateOf(initialStep) }

    // Core profile state
    var currency by rememberSaveable { mutableStateOf(profile?.currency ?: "₹") }
    var objective by rememberSaveable { mutableStateOf(profile?.primaryObjective ?: "Control spending") }
    var mode by rememberSaveable { mutableStateOf(profile?.appMode ?: "Strict Mode") }
    var salaryText by rememberSaveable { mutableStateOf(profile?.salaryAmount?.toInt()?.toString() ?: "60000") }
    var paydayText by rememberSaveable { mutableStateOf(profile?.salaryDate?.toString() ?: "1") }
    var balanceText by rememberSaveable { mutableStateOf("25000") }
    var bufferText by rememberSaveable { mutableStateOf(profile?.safetyBuffer?.toInt()?.toString() ?: "2000") }

    // Payday type state
    var paydayType by rememberSaveable { mutableStateOf("specific") } // "specific", "last_working", "last_day"
    var workingDays by rememberSaveable { mutableStateOf("1,2,3,4,5") } // CSV 0=Sun,1=Mon..6=Sat
    var workingDaysMode by rememberSaveable { mutableStateOf("mon_fri") } // "mon_fri", "mon_sat", "custom"

    // Dashboard config
    var dashboardConfig by rememberSaveable(
        stateSaver = Saver(
            save = { it.toCsv() },
            restore = { DashboardConfig.fromCsv(it) }
        )
    ) { mutableStateOf(DashboardConfig.fromCsv(profile?.hiddenWidgets)) }

    // Widget ordering (CSV of WidgetId keys)
    var widgetOrderCsv by rememberSaveable {
        mutableStateOf(WidgetId.values().joinToString(",") { it.key })
    }
    val orderedWidgets: List<WidgetId> = remember(widgetOrderCsv) {
        widgetOrderCsv.split(",").mapNotNull { WidgetId.fromKey(it.trim()) }
            .let { parsed ->
                // Ensure all widgets are present (add missing ones at end)
                val all = WidgetId.values().toList()
                parsed + all.filter { it !in parsed }
            }
    }

    // Accounts state — list of mutable entry holders
    var accountEntries by rememberSaveable(stateSaver = Saver(
        save = { list: List<AccountEntry> ->
            list.joinToString("|") { "${it.name}§${it.type}§${it.balance}§${it.isPrimary}§${it.creditLimit}" }
        },
        restore = { s: String ->
            if (s.isBlank()) emptyList()
            else s.split("|").mapNotNull { part ->
                val p = part.split("§")
                if (p.size >= 5) AccountEntry(p[0], p[1], p[2].toDoubleOrNull() ?: 0.0, p[3].toBoolean(), p[4].toDoubleOrNull() ?: 0.0)
                else null
            }
        }
    )) { mutableStateOf(emptyList<AccountEntry>()) }

    // Recurring state
    var recurringEntries by rememberSaveable(stateSaver = Saver(
        save = { list: List<RecurringEntry> ->
            list.joinToString("|") { "${it.name}§${it.amount}§${it.dueDay}§${it.category}§${it.isSubscription}" }
        },
        restore = { s: String ->
            if (s.isBlank()) emptyList()
            else s.split("|").mapNotNull { part ->
                val p = part.split("§")
                if (p.size >= 5) RecurringEntry(p[0], p[1].toDoubleOrNull() ?: 0.0, p[2].toIntOrNull() ?: 1, p[3], p[4].toBoolean())
                else null
            }
        }
    )) { mutableStateOf(emptyList<RecurringEntry>()) }

    val salaryValid = (salaryText.toDoubleOrNull() ?: 0.0) > 0.0
    val paydayValid = when (paydayType) {
        "specific" -> paydayText.toIntOrNull()?.let { it in 1..31 } == true
        else -> true
    }
    val balanceValid = (balanceText.toDoubleOrNull() ?: -1.0) >= 0.0
    val bufferValid = (bufferText.toDoubleOrNull() ?: -1.0) >= 0.0
    val bufferOverBalance = (bufferText.toDoubleOrNull() ?: 0.0) > (balanceText.toDoubleOrNull() ?: 0.0)
    val widgetsValid = dashboardConfig.hasAnyVisible
    val accountsValid = accountEntries.isEmpty() || accountEntries.all { it.name.isNotBlank() && it.balance >= 0.0 }

    fun resolvedPayday(): Int = when (paydayType) {
        "specific" -> paydayText.toIntOrNull()?.coerceIn(1, 31) ?: 1
        else -> 0 // sentinel: 0 = last_working / last_day
    }

    fun goNext() {
        val nextStep = (step + 1).coerceAtMost(STEP_COUNT - 1)
        viewModel.persistOnboardingProgress(
            step = nextStep,
            currency = currency,
            objective = objective,
            mode = mode,
            salary = salaryText.toDoubleOrNull(),
            payday = resolvedPayday(),
            buffer = bufferText.toDoubleOrNull(),
            hiddenWidgets = dashboardConfig.toCsv(),
            widgetOrder = widgetOrderCsv
        )
        step = nextStep
    }

    fun goBack() {
        if (step > 0) step -= 1
    }

    fun commit() {
        onComplete(
            currency,
            objective,
            mode,
            salaryText.toDoubleOrNull() ?: 60000.0,
            resolvedPayday(),
            balanceText.toDoubleOrNull() ?: 25000.0,
            bufferText.toDoubleOrNull() ?: 2000.0,
            dashboardConfig.toCsv(),
            accountEntries,
            recurringEntries,
            widgetOrderCsv
        )
    }

    BackHandler(enabled = step > 0) { goBack() }

    val ctaEnabled = when (step) {
        STEP_WELCOME -> true
        STEP_OBJECTIVE -> objective.isNotBlank()
        STEP_BUDGET_MODE -> mode.isNotBlank()
        STEP_INCOME -> salaryValid && paydayValid
        STEP_FOUNDATION -> balanceValid && bufferValid
        STEP_ACCOUNTS -> accountsValid
        STEP_RECURRING -> true
        STEP_DASHBOARD -> widgetsValid
        STEP_REVIEW -> salaryValid && paydayValid && balanceValid && bufferValid && widgetsValid && accountsValid
        else -> true
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LuxBlack)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        ProgressBar(step = step, total = STEP_COUNT)

        AnimatedContent(
            targetState = step,
            label = "onboardingStep",
            transitionSpec = {
                val forward = targetState > initialState
                val dir = if (forward) 1 else -1
                slideInHorizontally(spring(stiffness = Spring.StiffnessMediumLow)) { it * dir / 6 } +
                    fadeIn(spring()) togetherWith
                    slideOutHorizontally(spring(stiffness = Spring.StiffnessMediumLow)) { -it * dir / 6 } +
                    fadeOut(spring(stiffness = Spring.StiffnessHigh))
            },
            modifier = Modifier.weight(1f)
        ) { s ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 16.dp)
            ) {
                when (s) {
                    STEP_WELCOME -> WelcomeStep()
                    STEP_OBJECTIVE -> ObjectiveStep(
                        objective = objective,
                        onObjective = { objective = it }
                    )
                    STEP_BUDGET_MODE -> BudgetModeStep(
                        mode = mode,
                        onMode = { mode = it }
                    )
                    STEP_INCOME -> IncomeStep(
                        currency = currency,
                        salary = salaryText,
                        paydayText = paydayText,
                        paydayType = paydayType,
                        workingDays = workingDays,
                        workingDaysMode = workingDaysMode,
                        onCurrency = { currency = it },
                        onSalary = { salaryText = it },
                        onPaydayText = { paydayText = it },
                        onPaydayType = { paydayType = it },
                        onWorkingDays = { workingDays = it },
                        onWorkingDaysMode = { workingDaysMode = it }
                    )
                    STEP_FOUNDATION -> FoundationStep(
                        currency = currency,
                        balance = balanceText,
                        buffer = bufferText,
                        bufferOverBalance = bufferOverBalance,
                        onBalance = { balanceText = it },
                        onBuffer = { bufferText = it }
                    )
                    STEP_ACCOUNTS -> AccountsStep(
                        currency = currency,
                        entries = accountEntries,
                        onEntriesChange = { accountEntries = it }
                    )
                    STEP_RECURRING -> RecurringStep(
                        currency = currency,
                        entries = recurringEntries,
                        onEntriesChange = { recurringEntries = it }
                    )
                    STEP_DASHBOARD -> DashboardStep(
                        config = dashboardConfig,
                        onToggle = { widget -> dashboardConfig = dashboardConfig.toggle(widget) }
                    )
                    STEP_REVIEW -> ReviewStep(
                        currency = currency,
                        objective = objective,
                        mode = mode,
                        salary = salaryText,
                        paydayType = paydayType,
                        paydayText = paydayText,
                        workingDays = workingDays,
                        workingDaysMode = workingDaysMode,
                        balance = balanceText,
                        buffer = bufferText,
                        config = dashboardConfig,
                        accountEntries = accountEntries,
                        recurringEntries = recurringEntries,
                        onEdit = { editStep -> step = editStep }
                    )
                }
            }
        }

        BottomBar(
            step = step,
            ctaEnabled = ctaEnabled,
            recurringEmpty = recurringEntries.isEmpty(),
            onBack = ::goBack,
            onNext = ::goNext,
            onCommit = ::commit
        )
    }
}

/* ─────────────────────── Progress bar ─────────────────────── */

@Composable
private fun ProgressBar(step: Int, total: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        repeat(total) { i ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(if (i <= step) LuxGoldChange else LuxCardGray)
            )
        }
    }
}

/* ─────────────────────── Steps ─────────────────────── */

@Composable
private fun WelcomeStep() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AurenLogo(modifier = Modifier.size(140.dp))
        Spacer(Modifier.height(28.dp))
        Text(
            text = "AUREN",
            color = LuxGoldChange,
            style = Typography.titleLarge,
            letterSpacing = 6.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Money OS",
            color = LuxIvory,
            style = Typography.displaySmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(24.dp))
        Text(
            text = "Your private wealth command centre.",
            color = LuxMuted,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "We'll set up your finances in 7 quick steps. Every choice can be changed later from Settings.",
            color = LuxMuted.copy(alpha = 0.7f),
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        )
    }
}

@Composable
private fun ObjectiveStep(
    objective: String,
    onObjective: (String) -> Unit
) {
    StepHeader(
        title = "What are we building?",
        subtitle = "Choose your primary financial goal for Auren to guide you towards."
    )
    SectionLabel("Primary objective")
    listOf(
        "Control spending" to "Stay under a daily Safe-to-Spend limit.",
        "Start saving" to "Build a habit of moving money into goals every payday.",
        "Clear debt" to "Prioritize EMI / credit-card payoff with the snowball method.",
        "Build emergency fund" to "Lock away 3–6 months of expenses for safety."
    ).forEach { (k, desc) ->
        SelectableCard(
            selected = objective == k,
            label = k,
            description = desc,
            onClick = { onObjective(k) }
        )
    }
}

@Composable
private fun BudgetModeStep(
    mode: String,
    onMode: (String) -> Unit
) {
    StepHeader(
        title = "Pick your discipline mode",
        subtitle = "This shapes how strictly Auren enforces your daily spend limits."
    )
    SectionLabel("Budget mode")
    listOf(
        "Strict Mode" to "Hardest. Bills + buffer reserved first; Safe-to-Spend stays low to enforce discipline.",
        "Balanced Mode" to "Default. Spend down to a calculated daily figure while essentials stay protected.",
        "Relaxed Mode" to "Most flexible. Useful when income or expenses vary widely month to month."
    ).forEach { (k, desc) ->
        SelectableCard(
            selected = mode == k,
            label = k.replace(" Mode", ""),
            description = desc,
            onClick = { onMode(k) }
        )
    }
}

@Composable
private fun IncomeStep(
    currency: String,
    salary: String,
    paydayText: String,
    paydayType: String,
    workingDays: String,
    workingDaysMode: String,
    onCurrency: (String) -> Unit,
    onSalary: (String) -> Unit,
    onPaydayText: (String) -> Unit,
    onPaydayType: (String) -> Unit,
    onWorkingDays: (String) -> Unit,
    onWorkingDaysMode: (String) -> Unit
) {
    StepHeader(
        title = "Income rails",
        subtitle = "How much you earn and when it lands."
    )

    // Currency row
    SectionLabel("Currency")
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        listOf("₹", "$", "€", "£", "¥").forEach { sym ->
            val sel = currency == sym
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (sel) LuxGoldChange else LuxCardGray)
                    .border(
                        1.dp,
                        if (sel) LuxGoldChange else LuxCardGray.copy(alpha = 0.5f),
                        RoundedCornerShape(10.dp)
                    )
                    .clickable { onCurrency(sym) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = sym,
                    color = if (sel) LuxBlack else LuxIvory,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }
    }

    // Salary
    SectionLabel("Monthly take-home")
    OutlinedTextField(
        value = salary,
        onValueChange = { input -> if (input.all { it.isDigit() }) onSalary(input) },
        modifier = Modifier
            .fillMaxWidth()
            .testTag("onboarding_salary"),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        textStyle = TextStyle(color = LuxIvory, fontSize = 18.sp, fontWeight = FontWeight.SemiBold),
        leadingIcon = { Text(currency, color = LuxGoldChange, fontWeight = FontWeight.Bold, fontSize = 16.sp) },
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = LuxGoldChange,
            unfocusedBorderColor = LuxCardGray,
            cursorColor = LuxGoldChange
        ),
        shape = RoundedCornerShape(12.dp)
    )

    // Payday type selector
    SectionLabel("Payday schedule")
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        listOf(
            "specific" to "Specific Day",
            "last_working" to "Last Working Day",
            "last_day" to "Last Day of Month"
        ).forEach { (key, label) ->
            val sel = paydayType == key
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(50.dp))
                    .background(if (sel) LuxGoldChange else LuxCardGray)
                    .border(1.dp, if (sel) LuxGoldChange else LuxCardGray.copy(alpha = 0.5f), RoundedCornerShape(50.dp))
                    .clickable { onPaydayType(key) }
                    .padding(horizontal = 6.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    color = if (sel) LuxBlack else LuxIvory,
                    fontSize = 11.sp,
                    fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal,
                    textAlign = TextAlign.Center
                )
            }
        }
    }

    // Specific day number field
    if (paydayType == "specific") {
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = paydayText,
            onValueChange = { input ->
                if (input.length <= 2 && input.all { it.isDigit() }) onPaydayText(input)
            },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("onboarding_payday"),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            label = { Text("Day of month (1–31)", color = LuxMuted, fontSize = 12.sp) },
            textStyle = TextStyle(color = LuxIvory, fontSize = 18.sp, fontWeight = FontWeight.SemiBold),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = LuxGoldChange,
                unfocusedBorderColor = LuxCardGray,
                cursorColor = LuxGoldChange
            ),
            shape = RoundedCornerShape(12.dp)
        )
    }

    // Working days selector (shown for specific and last_working)
    if (paydayType == "specific" || paydayType == "last_working") {
        SectionLabel("Working days")
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(
                "mon_fri" to "Mon – Fri",
                "mon_sat" to "Mon – Sat",
                "custom" to "Custom"
            ).forEach { (key, label) ->
                val sel = workingDaysMode == key
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(50.dp))
                        .background(if (sel) LuxGoldChange else LuxCardGray)
                        .border(1.dp, if (sel) LuxGoldChange else LuxCardGray.copy(alpha = 0.5f), RoundedCornerShape(50.dp))
                        .clickable {
                            onWorkingDaysMode(key)
                            when (key) {
                                "mon_fri" -> onWorkingDays("1,2,3,4,5")
                                "mon_sat" -> onWorkingDays("1,2,3,4,5,6")
                                // custom: keep current
                            }
                        }
                        .padding(horizontal = 6.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        color = if (sel) LuxBlack else LuxIvory,
                        fontSize = 11.sp,
                        fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // Custom 7-checkbox row
        if (workingDaysMode == "custom") {
            Spacer(Modifier.height(12.dp))
            val selectedDays = workingDays.split(",").mapNotNull { it.trim().toIntOrNull() }.toMutableSet()
            val dayLabels = listOf("M" to 1, "T" to 2, "W" to 3, "T" to 4, "F" to 5, "S" to 6, "S" to 0)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                dayLabels.forEach { (label, dayIndex) ->
                    val sel = dayIndex in selectedDays
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (sel) LuxGoldChange else LuxCardGray)
                            .border(1.dp, if (sel) LuxGoldChange else LuxCardGray.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            .clickable {
                                val newSet = selectedDays.toMutableSet()
                                if (sel) newSet.remove(dayIndex) else newSet.add(dayIndex)
                                onWorkingDays(newSet.sorted().joinToString(","))
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            color = if (sel) LuxBlack else LuxIvory,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }

    InfoTip("Auren will refresh your Safe-to-Spend limit on your payday each month.")
}

@Composable
private fun FoundationStep(
    currency: String,
    balance: String,
    buffer: String,
    bufferOverBalance: Boolean,
    onBalance: (String) -> Unit,
    onBuffer: (String) -> Unit
) {
    StepHeader(
        title = "Where you stand",
        subtitle = "Starting balance and the safety reserve to defend."
    )
    SectionLabel("Opening balance")
    OutlinedTextField(
        value = balance,
        onValueChange = { input -> if (input.all { it.isDigit() }) onBalance(input) },
        modifier = Modifier
            .fillMaxWidth()
            .testTag("onboarding_balance"),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        textStyle = TextStyle(color = LuxIvory, fontSize = 18.sp, fontWeight = FontWeight.SemiBold),
        leadingIcon = { Text(currency, color = LuxGoldChange, fontWeight = FontWeight.Bold, fontSize = 16.sp) },
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = LuxGoldChange,
            unfocusedBorderColor = LuxCardGray,
            cursorColor = LuxGoldChange
        ),
        shape = RoundedCornerShape(12.dp)
    )
    InfoTip("Cash sitting in your main account today. We'll create a Primary Bank Account with this balance.")
    SectionLabel("Safety buffer")
    OutlinedTextField(
        value = buffer,
        onValueChange = { input -> if (input.all { it.isDigit() }) onBuffer(input) },
        modifier = Modifier
            .fillMaxWidth()
            .testTag("onboarding_buffer"),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        textStyle = TextStyle(color = LuxIvory, fontSize = 18.sp, fontWeight = FontWeight.SemiBold),
        leadingIcon = { Text(currency, color = LuxGoldChange, fontWeight = FontWeight.Bold, fontSize = 16.sp) },
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = LuxGoldChange,
            unfocusedBorderColor = LuxCardGray,
            cursorColor = LuxGoldChange
        ),
        shape = RoundedCornerShape(12.dp)
    )
    InfoTip("Money set aside on every cycle and excluded from Safe-to-Spend — a final cushion against overshooting.")
    if (bufferOverBalance) {
        Spacer(Modifier.height(8.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = LuxGoldLight.copy(alpha = 0.25f)),
            border = BorderStroke(1.dp, LuxGoldChange.copy(alpha = 0.4f)),
            shape = RoundedCornerShape(10.dp)
        ) {
            Text(
                text = "Heads up — your buffer is larger than your opening balance. You can still continue, but Safe-to-Spend will be 0 until your next payday.",
                color = LuxGoldChange,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                modifier = Modifier.padding(12.dp)
            )
        }
    }
}

@Composable
private fun AccountsStep(
    currency: String,
    entries: List<AccountEntry>,
    onEntriesChange: (List<AccountEntry>) -> Unit
) {
    StepHeader(
        title = "Your bank accounts",
        subtitle = "Add all accounts you actively use. Designate one primary."
    )

    InfoTip("Your primary account is where Safe-to-Spend and bill payments are tracked by default.")

    if (entries.isEmpty()) {
        Spacer(Modifier.height(24.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(LuxCardGray.copy(alpha = 0.3f))
                .border(1.dp, LuxCardGray, RoundedCornerShape(12.dp))
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Add your first account below",
                color = LuxMuted,
                fontSize = 13.sp,
                textAlign = TextAlign.Center
            )
        }
        Spacer(Modifier.height(10.dp))
        InfoTip("You can also add accounts from the home screen later.")
    } else {
        entries.forEachIndexed { index, entry ->
            Spacer(Modifier.height(10.dp))
            AccountEntryCard(
                currency = currency,
                entry = entry,
                onUpdate = { updated ->
                    val newList = entries.toMutableList()
                    newList[index] = updated
                    onEntriesChange(newList)
                },
                onSetPrimary = {
                    onEntriesChange(entries.mapIndexed { i, e -> e.copy(isPrimary = i == index) })
                },
                onDelete = {
                    val newList = entries.toMutableList()
                    newList.removeAt(index)
                    // Ensure there's still a primary if we deleted it
                    if (newList.isNotEmpty() && newList.none { it.isPrimary }) {
                        newList[0] = newList[0].copy(isPrimary = true)
                    }
                    onEntriesChange(newList)
                }
            )
        }
    }

    Spacer(Modifier.height(16.dp))
    OutlinedButton(
        onClick = {
            val newEntry = AccountEntry(
                name = "",
                type = "Savings",
                balance = 0.0,
                isPrimary = entries.isEmpty()
            )
            onEntriesChange(entries + newEntry)
        },
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, LuxGoldChange.copy(alpha = 0.6f)),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = LuxGoldChange),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text("+ Add Account", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
    }
}

@Composable
private fun AccountEntryCard(
    currency: String,
    entry: AccountEntry,
    onUpdate: (AccountEntry) -> Unit,
    onSetPrimary: () -> Unit,
    onDelete: () -> Unit
) {
    val accountTypes = listOf("Savings", "Current", "Wallet", "Credit Card", "Investment")

    val majorBanks = listOf(
        "HDFC Bank", "ICICI Bank", "SBI - State Bank of India", "Axis Bank", "Kotak Mahindra Bank",
        "Punjab National Bank", "Bank of Baroda", "Canara Bank", "Union Bank of India", "IndusInd Bank",
        "Yes Bank", "IDFC FIRST Bank", "Federal Bank", "South Indian Bank", "Karur Vysya Bank",
        "City Union Bank", "Bandhan Bank", "RBL Bank", "DCB Bank", "Nainital Bank",
        "AU Small Finance Bank", "Ujjivan Small Finance Bank", "Equitas Small Finance Bank",
        "ESAF Small Finance Bank", "Fincare Small Finance Bank",
        "Paytm Payments Bank", "Airtel Payments Bank", "India Post Payments Bank", "NSDL Payments Bank",
        "Citibank", "HSBC", "Standard Chartered", "Deutsche Bank", "DBS Bank"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = LuxCardGray.copy(alpha = 0.4f)),
        border = BorderStroke(
            1.dp,
            if (entry.isPrimary) LuxGoldChange.copy(alpha = 0.6f) else LuxCardGray
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(14.dp)) {
                // Bank name field with autocomplete suggestions
                var showSuggestions by remember { mutableStateOf(false) }
                val suggestions = remember(entry.name) {
                    if (entry.name.length >= 2) {
                        majorBanks.filter { it.contains(entry.name, ignoreCase = true) }.take(5)
                    } else emptyList()
                }

                OutlinedTextField(
                    value = entry.name,
                    onValueChange = {
                        onUpdate(entry.copy(name = it))
                        showSuggestions = true
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("Bank / Account name", color = LuxMuted, fontSize = 11.sp) },
                    placeholder = { Text("e.g. HDFC Savings", color = LuxMuted.copy(alpha = 0.5f), fontSize = 12.sp) },
                    textStyle = TextStyle(color = LuxIvory, fontSize = 14.sp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = LuxGoldChange,
                        unfocusedBorderColor = LuxCardGray,
                        focusedTextColor = LuxIvory,
                        unfocusedTextColor = LuxIvory,
                        cursorColor = LuxGoldChange
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
                if (showSuggestions && suggestions.isNotEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = LuxCardGray),
                        border = BorderStroke(1.dp, LuxGoldChange.copy(alpha = 0.3f)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column {
                            suggestions.forEach { bank ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            onUpdate(entry.copy(name = bank))
                                            showSuggestions = false
                                        }
                                        .padding(horizontal = 12.dp, vertical = 10.dp)
                                ) {
                                    Text(bank, color = LuxIvory, fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))

                // Type chips
                Text(
                    text = "TYPE",
                    color = LuxGoldChange,
                    fontSize = 10.sp,
                    letterSpacing = 2.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(start = 2.dp, bottom = 6.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    accountTypes.forEach { t ->
                        val sel = entry.type == t
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (sel) LuxGoldChange else LuxCardGray)
                                .border(1.dp, if (sel) LuxGoldChange else LuxCardGray.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                .clickable { onUpdate(entry.copy(type = t)) }
                                .padding(horizontal = 4.dp, vertical = 7.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = t,
                                color = if (sel) LuxBlack else LuxIvory,
                                fontSize = 10.sp,
                                fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal,
                                textAlign = TextAlign.Center,
                                maxLines = 1
                            )
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))

                // Balance field
                OutlinedTextField(
                    value = if (entry.balance == 0.0) "" else entry.balance.toLong().toString(),
                    onValueChange = { input ->
                        if (input.all { it.isDigit() }) {
                            onUpdate(entry.copy(balance = input.toDoubleOrNull() ?: 0.0))
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    label = { Text("Current balance", color = LuxMuted, fontSize = 11.sp) },
                    leadingIcon = { Text(currency, color = LuxGoldChange, fontWeight = FontWeight.Bold, fontSize = 14.sp) },
                    textStyle = TextStyle(color = LuxIvory, fontSize = 16.sp, fontWeight = FontWeight.SemiBold),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = LuxGoldChange,
                        unfocusedBorderColor = LuxCardGray,
                        cursorColor = LuxGoldChange
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(Modifier.height(10.dp))

                // Primary radio
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (entry.isPrimary) LuxGoldChange.copy(alpha = 0.12f) else Color.Transparent)
                        .border(
                            1.dp,
                            if (entry.isPrimary) LuxGoldChange.copy(alpha = 0.4f) else LuxCardGray,
                            RoundedCornerShape(8.dp)
                        )
                        .clickable { onSetPrimary() }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(if (entry.isPrimary) LuxGoldChange else Color.Transparent)
                            .border(1.5.dp, if (entry.isPrimary) LuxGoldChange else LuxMuted, CircleShape)
                    )
                    Text(
                        text = if (entry.isPrimary) "Primary account" else "Set as primary",
                        color = if (entry.isPrimary) LuxGoldChange else LuxMuted,
                        fontSize = 12.sp,
                        fontWeight = if (entry.isPrimary) FontWeight.SemiBold else FontWeight.Normal
                    )
                }
            }

            // Delete button
            IconButton(
                onClick = onDelete,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(32.dp)
                    .padding(4.dp)
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Remove account",
                    tint = LuxMuted,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun RecurringStep(
    currency: String,
    entries: List<RecurringEntry>,
    onEntriesChange: (List<RecurringEntry>) -> Unit
) {
    StepHeader(
        title = "Recurring commitments",
        subtitle = "EMIs, rent, and subscriptions you pay every month."
    )

    InfoTip("These will be added as Protected Bills and shown in your Commitment Timeline.")

    if (entries.isEmpty()) {
        Spacer(Modifier.height(24.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(LuxCardGray.copy(alpha = 0.3f))
                .border(1.dp, LuxCardGray, RoundedCornerShape(12.dp))
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No recurring items added yet.\nTap Add below or skip this step.",
                color = LuxMuted,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp
            )
        }
    } else {
        entries.forEachIndexed { index, entry ->
            Spacer(Modifier.height(10.dp))
            RecurringEntryCard(
                currency = currency,
                entry = entry,
                onUpdate = { updated ->
                    val newList = entries.toMutableList()
                    newList[index] = updated
                    onEntriesChange(newList)
                },
                onDelete = {
                    val newList = entries.toMutableList()
                    newList.removeAt(index)
                    onEntriesChange(newList)
                }
            )
        }
    }

    Spacer(Modifier.height(16.dp))
    OutlinedButton(
        onClick = {
            val newEntry = RecurringEntry(
                name = "",
                amount = 0.0,
                dueDay = 1,
                category = "EMI",
                isSubscription = false
            )
            onEntriesChange(entries + newEntry)
        },
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, LuxGoldChange.copy(alpha = 0.6f)),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = LuxGoldChange),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text("+ Add Recurring Item", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
    }
}

@Composable
private fun RecurringEntryCard(
    currency: String,
    entry: RecurringEntry,
    onUpdate: (RecurringEntry) -> Unit,
    onDelete: () -> Unit
) {
    val categories = listOf("EMI", "Rent", "Utilities", "Subscription", "Insurance", "Other")

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = LuxCardGray.copy(alpha = 0.4f)),
        border = BorderStroke(1.dp, LuxCardGray),
        shape = RoundedCornerShape(12.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(14.dp)) {
                // Name field
                OutlinedTextField(
                    value = entry.name,
                    onValueChange = { onUpdate(entry.copy(name = it)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("Name (e.g. Netflix, Home Loan EMI)", color = LuxMuted, fontSize = 11.sp) },
                    textStyle = TextStyle(color = LuxIvory, fontSize = 14.sp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = LuxGoldChange,
                        unfocusedBorderColor = LuxCardGray,
                        cursorColor = LuxGoldChange
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Amount field
                    OutlinedTextField(
                        value = if (entry.amount == 0.0) "" else entry.amount.toLong().toString(),
                        onValueChange = { input ->
                            if (input.all { it.isDigit() }) {
                                onUpdate(entry.copy(amount = input.toDoubleOrNull() ?: 0.0))
                            }
                        },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        label = { Text("Amount", color = LuxMuted, fontSize = 11.sp) },
                        leadingIcon = { Text(currency, color = LuxGoldChange, fontWeight = FontWeight.Bold, fontSize = 13.sp) },
                        textStyle = TextStyle(color = LuxIvory, fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = LuxGoldChange,
                            unfocusedBorderColor = LuxCardGray,
                            cursorColor = LuxGoldChange
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Due day field
                    OutlinedTextField(
                        value = if (entry.dueDay == 0) "" else entry.dueDay.toString(),
                        onValueChange = { input ->
                            if (input.length <= 2 && input.all { it.isDigit() }) {
                                onUpdate(entry.copy(dueDay = input.toIntOrNull() ?: 1))
                            }
                        },
                        modifier = Modifier.weight(0.6f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        label = { Text("Due day", color = LuxMuted, fontSize = 11.sp) },
                        textStyle = TextStyle(color = LuxIvory, fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = LuxGoldChange,
                            unfocusedBorderColor = LuxCardGray,
                            cursorColor = LuxGoldChange
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                Spacer(Modifier.height(10.dp))

                // Category chips
                Text(
                    text = "CATEGORY",
                    color = LuxGoldChange,
                    fontSize = 10.sp,
                    letterSpacing = 2.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(start = 2.dp, bottom = 6.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    categories.forEach { cat ->
                        val sel = entry.category == cat
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50.dp))
                                .background(if (sel) LuxGoldChange else LuxCardGray)
                                .border(1.dp, if (sel) LuxGoldChange else LuxCardGray.copy(alpha = 0.5f), RoundedCornerShape(50.dp))
                                .clickable {
                                    onUpdate(
                                        entry.copy(
                                            category = cat,
                                            isSubscription = cat == "Subscription"
                                        )
                                    )
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = cat,
                                color = if (sel) LuxBlack else LuxIvory,
                                fontSize = 11.sp,
                                fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }

            // Delete button
            IconButton(
                onClick = onDelete,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(32.dp)
                    .padding(4.dp)
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Remove item",
                    tint = LuxMuted,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun DashboardStep(
    config: DashboardConfig,
    onToggle: (WidgetId) -> Unit
) {
    StepHeader(
        title = "Tune your dashboard",
        subtitle = "Choose which widgets to show on your home screen. Monetary Matrix is always on."
    )

    // MonetaryMatrix is always visible and pinned — show it as locked
    Spacer(Modifier.height(8.dp))
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = LuxGoldChange.copy(alpha = 0.1f)),
        border = BorderStroke(1.dp, LuxGoldChange.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Lock, contentDescription = null, tint = LuxGoldChange, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(WidgetId.MonetaryMatrix.label, color = LuxIvory, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Text(WidgetId.MonetaryMatrix.description, color = LuxMuted, fontSize = 11.sp)
            }
            Text("Always on", color = LuxGoldChange, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    }

    // Remaining toggleable widgets (excluding MonetaryMatrix)
    WidgetId.values().filter { it != WidgetId.MonetaryMatrix }.forEach { w ->
        val visible = config.isVisible(w)
        Spacer(Modifier.height(8.dp))
        Card(
            modifier = Modifier.fillMaxWidth().clickable { onToggle(w) },
            colors = CardDefaults.cardColors(containerColor = LuxCardGray.copy(alpha = 0.4f)),
            border = BorderStroke(1.dp, if (visible) LuxGoldChange.copy(alpha = 0.5f) else LuxCardGray),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(w.label, color = LuxIvory, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Text(w.description, color = LuxMuted, fontSize = 11.sp, lineHeight = 14.sp)
                }
                Switch(
                    checked = visible,
                    onCheckedChange = { onToggle(w) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = LuxGoldChange,
                        checkedTrackColor = LuxGoldLight,
                        uncheckedThumbColor = LuxMuted,
                        uncheckedTrackColor = LuxCardGray
                    )
                )
            }
        }
    }

    if (!config.hasAnyVisible) {
        Spacer(Modifier.height(8.dp))
        Text(
            text = "At least one widget must stay on.",
            color = LuxGoldChange,
            fontSize = 12.sp,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
        )
    }
}

@Composable
private fun ReviewStep(
    currency: String,
    objective: String,
    mode: String,
    salary: String,
    paydayType: String,
    paydayText: String,
    workingDays: String,
    workingDaysMode: String,
    balance: String,
    buffer: String,
    config: DashboardConfig,
    accountEntries: List<AccountEntry>,
    recurringEntries: List<RecurringEntry>,
    onEdit: (Int) -> Unit
) {
    StepHeader(
        title = "Confirm and initialize",
        subtitle = "Tap any row to edit."
    )
    ReviewRow(label = "Primary objective", value = objective, onEdit = { onEdit(STEP_OBJECTIVE) })
    ReviewRow(label = "Budget mode", value = mode.replace(" Mode", ""), onEdit = { onEdit(STEP_BUDGET_MODE) })
    ReviewRow(label = "Currency", value = currency, onEdit = { onEdit(STEP_INCOME) })
    ReviewRow(label = "Monthly salary", value = "$currency$salary", onEdit = { onEdit(STEP_INCOME) })

    // Human-readable payday
    val paydayDisplay = when (paydayType) {
        "last_day" -> "Last day of month"
        "last_working" -> {
            val wdLabel = when (workingDaysMode) {
                "mon_fri" -> "Mon – Fri"
                "mon_sat" -> "Mon – Sat"
                else -> workingDays
            }
            "Last working day ($wdLabel)"
        }
        else -> "Day $paydayText"
    }
    ReviewRow(label = "Payday", value = paydayDisplay, onEdit = { onEdit(STEP_INCOME) })
    ReviewRow(label = "Opening balance", value = "$currency$balance", onEdit = { onEdit(STEP_FOUNDATION) })
    ReviewRow(label = "Safety buffer", value = "$currency$buffer", onEdit = { onEdit(STEP_FOUNDATION) })

    // Accounts summary
    val primaryAccount = accountEntries.firstOrNull { it.isPrimary }?.name ?: "None"
    val accountsDisplay = if (accountEntries.isEmpty()) "None added (can add from home)"
    else "${accountEntries.size} account${if (accountEntries.size != 1) "s" else ""} · Primary: $primaryAccount"
    ReviewRow(label = "Bank accounts", value = accountsDisplay, onEdit = { onEdit(STEP_ACCOUNTS) })

    // Recurring summary
    val recurringDisplay = if (recurringEntries.isEmpty()) "None added"
    else "${recurringEntries.size} item${if (recurringEntries.size != 1) "s" else ""}"
    ReviewRow(label = "Recurring items", value = recurringDisplay, onEdit = { onEdit(STEP_RECURRING) })

    val visibleCount = WidgetId.values().count { config.isVisible(it) }
    ReviewRow(
        label = "Dashboard widgets",
        value = "$visibleCount of ${WidgetId.values().size} on",
        onEdit = { onEdit(STEP_DASHBOARD) }
    )
}

/* ─────────────────────── Reusable components ─────────────────────── */

@Composable
private fun StepHeader(title: String, subtitle: String) {
    Spacer(Modifier.height(8.dp))
    Text(
        text = title,
        color = LuxIvory,
        style = Typography.headlineSmall,
        fontWeight = FontWeight.Bold
    )
    Spacer(Modifier.height(4.dp))
    Text(
        text = subtitle,
        color = LuxMuted,
        fontSize = 13.sp,
        lineHeight = 18.sp
    )
    Spacer(Modifier.height(20.dp))
}

@Composable
private fun SectionLabel(text: String) {
    Spacer(Modifier.height(16.dp))
    Text(
        text = text.uppercase(),
        color = LuxGoldChange,
        fontSize = 10.sp,
        letterSpacing = 2.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(start = 2.dp, bottom = 8.dp)
    )
}

@Composable
private fun SelectableCard(
    selected: Boolean,
    label: String,
    description: String,
    onClick: () -> Unit
) {
    Spacer(Modifier.height(8.dp))
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (selected) LuxGoldChange else LuxCardGray.copy(alpha = 0.4f)
        ),
        border = BorderStroke(1.dp, if (selected) LuxGoldChange else LuxCardGray),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    color = if (selected) LuxBlack else LuxIvory,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = description,
                    color = if (selected) LuxBlack.copy(alpha = 0.75f) else LuxMuted,
                    fontSize = 11.sp,
                    lineHeight = 14.sp,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            if (selected) {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .background(LuxBlack, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        tint = LuxGoldChange,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ReviewRow(label: String, value: String, onEdit: () -> Unit) {
    Spacer(Modifier.height(6.dp))
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEdit() },
        colors = CardDefaults.cardColors(containerColor = LuxCardGray.copy(alpha = 0.4f)),
        border = BorderStroke(1.dp, LuxCardGray),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label.uppercase(),
                    color = LuxMuted,
                    fontSize = 10.sp,
                    letterSpacing = 1.5.sp
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = value,
                    color = LuxIvory,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Icon(
                Icons.Default.Edit,
                contentDescription = "Edit",
                tint = LuxGoldChange.copy(alpha = 0.7f),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun InfoTip(text: String) {
    Spacer(Modifier.height(6.dp))
    Text(
        text = text,
        color = LuxMuted,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        modifier = Modifier.padding(horizontal = 2.dp)
    )
}

@Composable
private fun BottomBar(
    step: Int,
    ctaEnabled: Boolean,
    recurringEmpty: Boolean,
    onBack: () -> Unit,
    onNext: () -> Unit,
    onCommit: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(LuxDarkGray)
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (step > 0) {
            OutlinedButton(
                onClick = onBack,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, LuxGoldChange.copy(alpha = 0.5f)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = LuxGoldChange),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", modifier = Modifier.size(20.dp))
            }
        }
        Button(
            onClick = { if (step == STEP_REVIEW) onCommit() else onNext() },
            enabled = ctaEnabled,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = LuxGoldChange,
                contentColor = LuxBlack,
                disabledContainerColor = LuxGoldChange.copy(alpha = 0.35f),
                disabledContentColor = LuxBlack.copy(alpha = 0.6f)
            ),
            modifier = Modifier
                .weight(if (step == 0) 1f else 2f)
                .testTag("onboarding_cta")
        ) {
            val label = when (step) {
                STEP_WELCOME -> "Get Started"
                STEP_REVIEW -> "Initialize Console"
                STEP_RECURRING -> if (recurringEmpty) "Skip" else "Continue"
                else -> "Continue"
            }
            val icon = when (step) {
                STEP_REVIEW -> Icons.Default.CheckCircle
                else -> Icons.Default.ArrowForward
            }
            Text(label, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Spacer(Modifier.width(6.dp))
            Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
        }
    }
}
