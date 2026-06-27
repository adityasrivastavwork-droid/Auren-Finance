package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

/**
 * Multi-step onboarding wizard for Auren Money OS.
 *
 * Six steps:
 *   1. Welcome — brand reveal, no inputs.
 *   2. Objective + Mode — strategy.
 *   3. Income — currency, salary, payday.
 *   4. Foundation — opening balance + safety buffer.
 *   5. Customize Dashboard — per-widget toggle (skippable).
 *   6. Review — confirm every choice, edit any row by tap.
 *
 * Design notes (from `auren-onboarding-design` workflow + adversarial critiques):
 *   - State persists to [UserProfile] after every Continue tap (no in-memory loss on
 *     process death). `profile.onboardingStep` is the cursor.
 *   - Final commit is ONE transactional [FinanceViewModel.onboardUser] call (no race).
 *   - System back button is routed via [BackHandler] to the wizard's own back.
 *   - Widget storage is a single CSV column (not N booleans).
 *   - Every preference is editable later via [SettingsSidebar].
 */
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun OnboardingFlow(
    profile: UserProfile?,
    viewModel: FinanceViewModel,
    onComplete: (currency: String, objective: String, mode: String, salary: Double, payday: Int, balance: Double, buffer: Double, hiddenWidgets: String) -> Unit
) {
    // Resume from the persisted cursor (0..STEP_COUNT-1). -1 means completed so we'd
    // never have entered the flow; coerce defensively to 0.
    val initialStep = (profile?.onboardingStep ?: 0).coerceIn(0, STEP_COUNT - 1)
    var step: Int by rememberSaveable { mutableStateOf(initialStep) }

    // Local edit state — seeded from existing profile so resume preserves entries.
    var currency: String by rememberSaveable { mutableStateOf(profile?.currency ?: "₹") }
    var objective: String by rememberSaveable { mutableStateOf(profile?.primaryObjective ?: "Control spending") }
    var mode: String by rememberSaveable { mutableStateOf(profile?.appMode ?: "Strict Mode") }
    var salaryText: String by rememberSaveable { mutableStateOf(profile?.salaryAmount?.toInt()?.toString() ?: "60000") }
    var paydayText: String by rememberSaveable { mutableStateOf(profile?.salaryDate?.toString() ?: "1") }
    var balanceText: String by rememberSaveable { mutableStateOf("25000") }
    var bufferText: String by rememberSaveable { mutableStateOf(profile?.safetyBuffer?.toInt()?.toString() ?: "2000") }
    var dashboardConfig: DashboardConfig by rememberSaveable(
        stateSaver = androidx.compose.runtime.saveable.Saver<DashboardConfig, String>(
            save = { it.toCsv() },
            restore = { DashboardConfig.fromCsv(it) }
        )
    ) { mutableStateOf(DashboardConfig.fromCsv(profile?.hiddenWidgets)) }

    val salaryValid = (salaryText.toDoubleOrNull() ?: 0.0) > 0.0
    val paydayValid = paydayText.toIntOrNull()?.let { it in 1..31 } == true
    val balanceValid = (balanceText.toDoubleOrNull() ?: -1.0) >= 0.0
    val bufferValid = (bufferText.toDoubleOrNull() ?: -1.0) >= 0.0
    val bufferOverBalance = (bufferText.toDoubleOrNull() ?: 0.0) > (balanceText.toDoubleOrNull() ?: 0.0)
    val widgetsValid = dashboardConfig.hasAnyVisible

    fun goNext() {
        val nextStep = (step + 1).coerceAtMost(STEP_COUNT - 1)
        viewModel.persistOnboardingProgress(
            step = nextStep,
            currency = currency,
            objective = objective,
            mode = mode,
            salary = salaryText.toDoubleOrNull(),
            payday = paydayText.toIntOrNull(),
            buffer = bufferText.toDoubleOrNull(),
            hiddenWidgets = dashboardConfig.toCsv()
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
            paydayText.toIntOrNull()?.coerceIn(1, 31) ?: 1,
            balanceText.toDoubleOrNull() ?: 25000.0,
            bufferText.toDoubleOrNull() ?: 2000.0,
            dashboardConfig.toCsv()
        )
    }

    // Intercept system back gesture for steps 1..N-1; step 0 lets it bubble (app exit).
    BackHandler(enabled = step > 0) { goBack() }

    // Per-step CTA enable flag.
    val ctaEnabled = when (step) {
        STEP_WELCOME -> true
        STEP_OBJECTIVE_MODE -> objective.isNotBlank() && mode.isNotBlank()
        STEP_INCOME -> salaryValid && paydayValid
        STEP_FOUNDATION -> balanceValid && bufferValid
        STEP_DASHBOARD -> widgetsValid
        STEP_REVIEW -> salaryValid && paydayValid && balanceValid && bufferValid && widgetsValid
        else -> true
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LuxBlack)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Progress bar — quiet, single thin row of 6 segments.
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
                    STEP_OBJECTIVE_MODE -> ObjectiveModeStep(
                        objective = objective,
                        mode = mode,
                        onObjective = { objective = it },
                        onMode = { mode = it }
                    )
                    STEP_INCOME -> IncomeStep(
                        currency = currency,
                        salary = salaryText,
                        payday = paydayText,
                        onCurrency = { currency = it },
                        onSalary = { salaryText = it },
                        onPayday = { paydayText = it }
                    )
                    STEP_FOUNDATION -> FoundationStep(
                        currency = currency,
                        balance = balanceText,
                        buffer = bufferText,
                        bufferOverBalance = bufferOverBalance,
                        onBalance = { balanceText = it },
                        onBuffer = { bufferText = it }
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
                        payday = paydayText,
                        balance = balanceText,
                        buffer = bufferText,
                        config = dashboardConfig,
                        onEdit = { editStep -> step = editStep }
                    )
                }
            }
        }

        // Bottom CTA bar — Back (when applicable) + primary action.
        BottomBar(
            step = step,
            ctaEnabled = ctaEnabled,
            onBack = ::goBack,
            onNext = ::goNext,
            onCommit = ::commit
        )
    }
}

private const val STEP_WELCOME = 0
private const val STEP_OBJECTIVE_MODE = 1
private const val STEP_INCOME = 2
private const val STEP_FOUNDATION = 3
private const val STEP_DASHBOARD = 4
private const val STEP_REVIEW = 5
private const val STEP_COUNT = 6

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
            text = "We'll set up your finances in 5 quick steps. Every choice can be changed later from Settings.",
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
private fun ObjectiveModeStep(
    objective: String,
    mode: String,
    onObjective: (String) -> Unit,
    onMode: (String) -> Unit
) {
    StepHeader(
        title = "What are we building?",
        subtitle = "Your main objective and the discipline mode that fits your life."
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
    payday: String,
    onCurrency: (String) -> Unit,
    onSalary: (String) -> Unit,
    onPayday: (String) -> Unit
) {
    StepHeader(
        title = "Income rails",
        subtitle = "How much you earn and when it lands."
    )
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
    SectionLabel("Payday (day of month)")
    OutlinedTextField(
        value = payday,
        onValueChange = { input ->
            if (input.length <= 2 && input.all { it.isDigit() }) onPayday(input)
        },
        modifier = Modifier
            .fillMaxWidth()
            .testTag("onboarding_payday"),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        textStyle = TextStyle(color = LuxIvory, fontSize = 18.sp, fontWeight = FontWeight.SemiBold),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = LuxGoldChange,
            unfocusedBorderColor = LuxCardGray,
            cursorColor = LuxGoldChange
        ),
        shape = RoundedCornerShape(12.dp)
    )
    InfoTip("Auren will refresh your Safe-to-Spend limit on this day each month. For months without this date, we use the last day.")
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
private fun DashboardStep(
    config: DashboardConfig,
    onToggle: (WidgetId) -> Unit
) {
    StepHeader(
        title = "Tune your dashboard",
        subtitle = "Switch widgets on or off. You can change this anytime from Settings."
    )
    WidgetId.values().forEach { w ->
        val visible = config.isVisible(w)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onToggle(w) },
            colors = CardDefaults.cardColors(containerColor = LuxCardGray.copy(alpha = 0.4f)),
            border = BorderStroke(1.dp, if (visible) LuxGoldChange.copy(alpha = 0.5f) else LuxCardGray),
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
                        text = w.label,
                        color = LuxIvory,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = w.description,
                        color = LuxMuted,
                        fontSize = 11.sp,
                        lineHeight = 14.sp
                    )
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
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp)
        )
    }
}

@Composable
private fun ReviewStep(
    currency: String,
    objective: String,
    mode: String,
    salary: String,
    payday: String,
    balance: String,
    buffer: String,
    config: DashboardConfig,
    onEdit: (Int) -> Unit
) {
    StepHeader(
        title = "Confirm and initialize",
        subtitle = "Tap any row to edit."
    )
    ReviewRow(label = "Primary objective", value = objective, onEdit = { onEdit(STEP_OBJECTIVE_MODE) })
    ReviewRow(label = "Budget mode", value = mode.replace(" Mode", ""), onEdit = { onEdit(STEP_OBJECTIVE_MODE) })
    ReviewRow(label = "Currency", value = currency, onEdit = { onEdit(STEP_INCOME) })
    ReviewRow(label = "Monthly salary", value = "$currency$salary", onEdit = { onEdit(STEP_INCOME) })
    ReviewRow(label = "Payday", value = "Day $payday", onEdit = { onEdit(STEP_INCOME) })
    ReviewRow(label = "Opening balance", value = "$currency$balance", onEdit = { onEdit(STEP_FOUNDATION) })
    ReviewRow(label = "Safety buffer", value = "$currency$buffer", onEdit = { onEdit(STEP_FOUNDATION) })
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
                Icon(Icons.Default.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Back", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
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
            val (label, icon) = when (step) {
                STEP_WELCOME -> "Get Started" to Icons.Default.ArrowForward
                STEP_REVIEW -> "Initialize Console" to Icons.Default.CheckCircle
                else -> "Continue" to Icons.Default.ArrowForward
            }
            Text(label, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Spacer(Modifier.width(6.dp))
            Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
        }
    }
}
