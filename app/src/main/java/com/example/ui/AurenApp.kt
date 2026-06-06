package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.*
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AurenApp(viewModel: FinanceViewModel) {
    val profile by viewModel.profile.collectAsStateWithLifecycle()
    val accounts by viewModel.accounts.collectAsStateWithLifecycle()
    val transactions by viewModel.transactions.collectAsStateWithLifecycle()
    val bills by viewModel.bills.collectAsStateWithLifecycle()
    val goals by viewModel.goals.collectAsStateWithLifecycle()
    val debts by viewModel.debts.collectAsStateWithLifecycle()
    val reviews by viewModel.reviews.collectAsStateWithLifecycle()

    val totalUsableBalance by viewModel.totalUsableBalance.collectAsStateWithLifecycle()
    val totalDebtOutstanding by viewModel.totalDebtOutstanding.collectAsStateWithLifecycle()
    val upcomingBillsTotal by viewModel.upcomingBillsTotal.collectAsStateWithLifecycle()
    val daysRemainingInCycle by viewModel.daysRemainingInCycle.collectAsStateWithLifecycle()
    val safeToSpendToday by viewModel.safeToSpendToday.collectAsStateWithLifecycle()

    var activeTab by remember { mutableStateOf("home") } // "home", "transactions", "plan", "insights", "coach"
    var showAddTransactionDialog by remember { mutableStateOf(false) }
    var showWeeklyCheckInDialog by remember { mutableStateOf(false) }
    var showAddAccountDialog by remember { mutableStateOf(false) }
    var showAddBillDialog by remember { mutableStateOf(false) }
    var showAddGoalDialog by remember { mutableStateOf(false) }
    var showAddDebtDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }

    val currency = profile?.currency ?: "₹"
    val mode = profile?.appMode ?: "Strict Mode"

    if (profile == null || !profile!!.isOnboarded) {
        OnboardingScreen(onOnboard = { cur, obj, md, sal, pay, bal, buf ->
            viewModel.onboardUser(cur, obj, md, sal, pay, bal, buf)
        })
    } else {
        Scaffold(
            bottomBar = {
                AurenBottomNavigation(activeTab = activeTab, onTabSelected = { activeTab = it })
            },
            floatingActionButton = {
                if (activeTab == "home" || activeTab == "transactions") {
                    FloatingActionButton(
                        onClick = { showAddTransactionDialog = true },
                        containerColor = LuxGoldChange,
                        contentColor = LuxBlack,
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "Add Transaction")
                    }
                }
            },
            containerColor = LuxBlack
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(LuxBlack)
            ) {
                Crossfade(targetState = activeTab, label = "tabTransition") { tab ->
                    when (tab) {
                        "home" -> HomeScreen(
                            viewModel = viewModel,
                            currency = currency,
                            mode = mode,
                            safeToSpendToday = safeToSpendToday,
                            daysRemaining = daysRemainingInCycle,
                            totalUsableBalance = totalUsableBalance,
                            totalDebt = totalDebtOutstanding,
                            accounts = accounts,
                            bills = bills,
                            onAddAccountClick = { showAddAccountDialog = true },
                            onCheckInClick = { showWeeklyCheckInDialog = true },
                            onSettingsClick = { showSettingsDialog = true }
                        )
                        "transactions" -> TransactionsScreen(
                            viewModel = viewModel,
                            transactions = transactions,
                            accounts = accounts,
                            currency = currency
                        )
                        "plan" -> PlanScreen(
                            viewModel = viewModel,
                            bills = bills,
                            goals = goals,
                            debts = debts,
                            accounts = accounts,
                            currency = currency,
                            mode = mode,
                            totalSalary = profile?.salaryAmount ?: 60000.0,
                            onAddBillClick = { showAddBillDialog = true },
                            onAddGoalClick = { showAddGoalDialog = true },
                            onAddDebtClick = { showAddDebtDialog = true }
                        )
                        "insights" -> InsightsScreen(
                            viewModel = viewModel,
                            transactions = transactions,
                            debts = debts,
                            profile = profile,
                            currency = currency,
                            totalUsableBalance = totalUsableBalance,
                            totalDebt = totalDebtOutstanding
                        )
                        "coach" -> CoachScreen(
                            viewModel = viewModel,
                            currency = currency
                        )
                    }
                }
            }
        }

        // Action Dialogs
        if (showAddTransactionDialog) {
            AddTransactionDialog(
                accounts = accounts,
                currency = currency,
                onDismiss = { showAddTransactionDialog = false },
                onAdd = { amt, type, accId, cat, rch, note, targetId ->
                    viewModel.addTransaction(amt, type, accId, cat, rch, note, targetId)
                    showAddTransactionDialog = false
                }
            )
        }

        if (showAddAccountDialog) {
            AddAccountDialog(
                currency = currency,
                onDismiss = { showAddAccountDialog = false },
                onAdd = { name, type, bal, limit, label ->
                    viewModel.addAccount(name, type, bal, limit, label)
                    showAddAccountDialog = false
                }
            )
        }

        if (showAddBillDialog) {
            AddBillDialog(
                onDismiss = { showAddBillDialog = false },
                onAdd = { name, amt, due, cat, sub ->
                    viewModel.addBill(name, amt, due, cat, sub)
                    showAddBillDialog = false
                }
            )
        }

        if (showAddGoalDialog) {
            AddGoalDialog(
                onDismiss = { showAddGoalDialog = false },
                onAdd = { name, target, current, mos, priority, cat ->
                    viewModel.addGoal(name, target, current, mos, priority, cat)
                    showAddGoalDialog = false
                }
            )
        }

        if (showAddDebtDialog) {
            AddDebtDialog(
                onDismiss = { showAddDebtDialog = false },
                onAdd = { name, type, out, rate, min, emi, due, m ->
                    viewModel.addDebt(name, type, out, rate, min, emi, due, m, name)
                    showAddDebtDialog = false
                }
            )
        }

        if (showWeeklyCheckInDialog) {
            WeeklyCheckInDialog(
                reviews = reviews,
                onDismiss = { showWeeklyCheckInDialog = false },
                onSave = { wentWell, reason, cat, offset, leak, act ->
                    viewModel.completeWeeklyCheckIn(wentWell, reason, cat, offset, leak, act)
                    showWeeklyCheckInDialog = false
                }
            )
        }

        if (showSettingsDialog) {
            SettingsDialog(
                profile = profile,
                onDismiss = { showSettingsDialog = false },
                onSave = { cur, obj, md, sal, buf ->
                    viewModel.updateProfile(cur, obj, md, sal, buf, true)
                    showSettingsDialog = false
                }
            )
        }
    }
}

// ---------------- CUSTOM UI NAVIGATION BAR ----------------
@Composable
fun AurenBottomNavigation(activeTab: String, onTabSelected: (String) -> Unit) {
    Surface(
        color = LuxDarkGray,
        border = BorderStroke(1.dp, Color(0xFFCAC4D0).copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            NavigationItem(
                label = "Home",
                isSelected = activeTab == "home",
                icon = Icons.Default.Dashboard,
                onClick = { onTabSelected("home") }
            )
            NavigationItem(
                label = "Ledger",
                isSelected = activeTab == "transactions",
                icon = Icons.Default.AccountBalanceWallet,
                onClick = { onTabSelected("transactions") }
            )
            NavigationItem(
                label = "Plan",
                isSelected = activeTab == "plan",
                icon = Icons.Default.TrendingUp,
                onClick = { onTabSelected("plan") }
            )
            NavigationItem(
                label = "Insights",
                isSelected = activeTab == "insights",
                icon = Icons.Default.Analytics,
                onClick = { onTabSelected("insights") }
            )
            NavigationItem(
                label = "Coach",
                isSelected = activeTab == "coach",
                icon = Icons.Default.AutoAwesome,
                onClick = { onTabSelected("coach") }
            )
        }
    }
}

@Composable
fun NavigationItem(label: String, isSelected: Boolean, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(if (isSelected) Color(0xFFE8DEF8) else Color.Transparent)
                .padding(horizontal = 16.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isSelected) Color(0xFF1D192B) else LuxMuted,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            color = if (isSelected) Color(0xFF1D192B) else LuxMuted,
            fontSize = 11.sp,
            fontFamily = FontFamily.SansSerif,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

// ---------------- ONBOARDING SCREEN ----------------
@Composable
fun OnboardingScreen(onOnboard: (String, String, String, Double, Int, Double, Double) -> Unit) {
    var step by remember { mutableStateOf(1) }

    var selectedObjective by remember { mutableStateOf("Control spending") }
    var selectedCurrency by remember { mutableStateOf("₹") }
    var selectedMode by remember { mutableStateOf("Strict Mode") }
    var salary by remember { mutableStateOf("60000") }
    var payday by remember { mutableStateOf("1") }
    var initialBalance by remember { mutableStateOf("25000") }
    var bufferAmount by remember { mutableStateOf("2000") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LuxBlack)
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "AUREN",
            style = Typography.labelLarge,
            color = LuxGoldChange,
            letterSpacing = 6.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Text(
            text = "MONEY OS",
            style = Typography.displayLarge,
            color = LuxIvory
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Private wealth command centre.",
            color = LuxMuted,
            textAlign = TextAlign.Center,
            style = Typography.bodyLarge
        )

        Spacer(modifier = Modifier.height(48.dp))

        when (step) {
            1 -> {
                // Objectives
                Text(
                    text = "Select Primary Objective",
                    color = LuxIvory,
                    style = Typography.titleLarge,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                val objectives = listOf(
                    "Control spending",
                    "Start saving",
                    "Clear debt",
                    "Build emergency fund"
                )
                objectives.forEach { obj ->
                    val isSelected = selectedObjective == obj
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                            .clickable { selectedObjective = obj },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) LuxCardGray else LuxDarkGray
                        ),
                        border = BorderStroke(
                            width = 1.dp,
                            color = if (isSelected) LuxGoldChange else LuxCardGray
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                contentDescription = null,
                                tint = if (isSelected) LuxGoldChange else LuxMuted
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(text = obj, color = LuxIvory, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
            2 -> {
                // App Mode & Currency
                Text(
                    text = "Operational Settings",
                    color = LuxIvory,
                    style = Typography.titleLarge,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Text(
                    text = "System Protection Mode",
                    color = LuxGoldChange,
                    style = Typography.labelLarge,
                    modifier = Modifier.align(Alignment.Start).padding(vertical = 8.dp)
                )
                val modes = listOf("Strict Mode", "Balanced Mode", "Relaxed Mode")
                modes.forEach { md ->
                    val isSelected = selectedMode == md
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                            .clickable { selectedMode = md },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) LuxCardGray else LuxDarkGray
                        ),
                        border = BorderStroke(
                            width = 1.dp,
                            color = if (isSelected) LuxGoldChange else LuxCardGray
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(text = md, color = LuxIvory, fontWeight = FontWeight.Bold)
                            val desc = when (md) {
                                "Strict Mode" -> "Protects EMIs & bills first. Daily limit dynamically drops on overspend. Strong warnings."
                                "Balanced Mode" -> "Medium safety control. Flexible categories can borrow buffer from each other."
                                else -> "Tracking & awareness focus. Soft guidance notifications with complete freedom."
                            }
                            Text(text = desc, color = LuxMuted, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Preferred Currency Symbol",
                    color = LuxGoldChange,
                    style = Typography.labelLarge,
                    modifier = Modifier.align(Alignment.Start).padding(vertical = 8.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val currencies = listOf("₹", "$", "€", "£")
                    currencies.forEach { cur ->
                        val isSelected = selectedCurrency == cur
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 4.dp)
                                .clickable { selectedCurrency = cur },
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) LuxCardGray else LuxDarkGray
                            ),
                            border = BorderStroke(1.dp, if (isSelected) LuxGoldChange else LuxCardGray)
                        ) {
                            Box(modifier = Modifier.padding(12.dp), contentAlignment = Alignment.Center) {
                                Text(
                                    text = cur,
                                    fontSize = 18.sp,
                                    color = if (isSelected) LuxGoldChange else LuxIvory,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }
            3 -> {
                // Cash Profile Inputs
                Text(
                    text = "Financial Foundation",
                    color = LuxIvory,
                    style = Typography.titleLarge,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                OutlinedTextField(
                    value = salary,
                    onValueChange = { salary = it },
                    label = { Text("Monthly Net Income") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = LuxGoldChange,
                        unfocusedBorderColor = LuxCardGray,
                        focusedLabelColor = LuxGoldChange,
                        unfocusedLabelColor = LuxMuted,
                        focusedTextColor = LuxIvory,
                        unfocusedTextColor = LuxIvory
                    ),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                )

                OutlinedTextField(
                    value = payday,
                    onValueChange = { payday = it },
                    label = { Text("Payday (Day of Month: 1-31)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = LuxGoldChange,
                        unfocusedBorderColor = LuxCardGray,
                        focusedLabelColor = LuxGoldChange,
                        unfocusedLabelColor = LuxMuted,
                        focusedTextColor = LuxIvory,
                        unfocusedTextColor = LuxIvory
                    ),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                )

                OutlinedTextField(
                    value = initialBalance,
                    onValueChange = { initialBalance = it },
                    label = { Text("Main Bank Balance Setup") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = LuxGoldChange,
                        unfocusedBorderColor = LuxCardGray,
                        focusedLabelColor = LuxGoldChange,
                        unfocusedLabelColor = LuxMuted,
                        focusedTextColor = LuxIvory,
                        unfocusedTextColor = LuxIvory
                    ),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                )

                OutlinedTextField(
                    value = bufferAmount,
                    onValueChange = { bufferAmount = it },
                    label = { Text("Safety Buffer Safeguard Amount") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = LuxGoldChange,
                        unfocusedBorderColor = LuxCardGray,
                        focusedLabelColor = LuxGoldChange,
                        unfocusedLabelColor = LuxMuted,
                        focusedTextColor = LuxIvory,
                        unfocusedTextColor = LuxIvory
                    ),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            if (step > 1) {
                OutlinedButton(
                    onClick = { step-- },
                    border = BorderStroke(1.dp, LuxGoldChange),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = LuxGoldChange),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f).padding(end = 8.dp)
                ) {
                    Text("Back")
                }
            }

            Button(
                onClick = {
                    if (step < 3) {
                        step++
                    } else {
                        onOnboard(
                            selectedCurrency,
                            selectedObjective,
                            selectedMode,
                            salary.toDoubleOrNull() ?: 60000.0,
                            payday.toIntOrNull() ?: 1,
                            initialBalance.toDoubleOrNull() ?: 25000.0,
                            bufferAmount.toDoubleOrNull() ?: 2000.0
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = LuxGoldChange, contentColor = LuxBlack),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.weight(1f).padding(start = if (step > 1) 8.dp else 0.dp)
            ) {
                Text(if (step == 3) "Initialize Console" else "Continue")
            }
        }
    }
}

// ---------------- HOME VIEW TAB ----------------
@Composable
fun HomeScreen(
    viewModel: FinanceViewModel,
    currency: String,
    mode: String,
    safeToSpendToday: Double,
    daysRemaining: Int,
    totalUsableBalance: Double,
    totalDebt: Double,
    accounts: List<Account>,
    bills: List<BillSubscription>,
    onAddAccountClick: () -> Unit,
    onCheckInClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Luxury Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(text = "COMMAND CONSOLE", style = Typography.labelLarge, color = LuxGoldChange, letterSpacing = 3.sp)
                Text(text = "Auren Money OS", style = Typography.headlineMedium, color = LuxIvory)
            }
            IconButton(onClick = onSettingsClick) {
                Icon(imageVector = Icons.Default.Settings, contentDescription = "Settings", tint = LuxGoldChange)
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Safe To Spend display
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = LuxGoldLight),
            border = BorderStroke(1.dp, Color(0xFFCAC4D0).copy(alpha = 0.4f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Rotated geometric badge from design template
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(color = LuxGoldChange, shape = RoundedCornerShape(14.dp))
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = LuxBlack,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "SAFE TO SPEND TODAY",
                    style = Typography.labelLarge,
                    color = Color(0xFF21005D), // Dark indigo text color
                    letterSpacing = 2.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "$currency${String.format("%,.2f", safeToSpendToday)}",
                    style = Typography.displayLarge,
                    color = Color(0xFF21005D), // Dark indigo text color
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))

                Divider(color = LuxCardGray)

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "AVAILABLE BUFFER", fontSize = 11.sp, color = LuxMuted)
                        Text(text = "$currency${String.format("%,.0f", totalUsableBalance)}", fontWeight = FontWeight.Bold, color = LuxIvory)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "TENURE REMAINING", fontSize = 11.sp, color = LuxMuted)
                        Text(text = "$daysRemaining days left", fontWeight = FontWeight.Bold, color = LuxIvory)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Action Deck
        Text(text = "DAILY ASSIGNMENTS", style = Typography.labelLarge, color = LuxGoldChange, letterSpacing = 2.sp)
        Spacer(modifier = Modifier.height(10.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onCheckInClick() },
            colors = CardDefaults.cardColors(containerColor = LuxCardGray),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, LuxGoldChange.copy(alpha = 0.3f))
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.ThumbUp,
                    contentDescription = null,
                    tint = LuxGoldChange,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1.0f)) {
                    Text(text = "Complete Weekly Money Check-In", color = LuxIvory, fontWeight = FontWeight.Bold)
                    Text(text = "Observe parameters, identify spend leaks, & restore OS limits.", fontSize = 12.sp, color = LuxMuted)
                }
                Icon(imageVector = Icons.Default.ChevronRight, contentDescription = null, tint = LuxMuted)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Accounts list
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "WEALTH VAULTS", style = Typography.labelLarge, color = LuxGoldChange, letterSpacing = 2.sp)
            IconButton(onClick = onAddAccountClick) {
                Icon(imageVector = Icons.Default.AddCircleOutline, contentDescription = "Add account", tint = LuxGoldChange)
            }
        }
        Spacer(modifier = Modifier.height(10.dp))

        if (accounts.isEmpty()) {
            Text(text = "No vaults defined. Link manual cash assets to track safe-to-spend limits.", color = LuxMuted, style = Typography.bodyLarge, modifier = Modifier.padding(vertical = 8.dp))
        } else {
            accounts.forEach { acc ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    colors = CardDefaults.cardColors(containerColor = LuxDarkGray),
                    border = BorderStroke(1.dp, LuxCardGray)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val accIcon = when (acc.type) {
                                "Savings", "Salary" -> Icons.Default.Savings
                                "Credit Card" -> Icons.Default.CreditCard
                                "Cash" -> Icons.Default.Payments
                                else -> Icons.Default.AccountBalance
                            }
                            Icon(imageVector = accIcon, contentDescription = null, tint = LuxGoldChange)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(text = acc.name, color = LuxIvory, fontWeight = FontWeight.Bold)
                                Text(text = acc.type, color = LuxMuted, fontSize = 12.sp)
                            }
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "$currency${String.format("%,.2f", acc.balance)}",
                                color = if (acc.type == "Credit Card") LuxError else LuxGoldLight,
                                fontWeight = FontWeight.Bold
                            )
                            if (acc.creditLimit > 0.0) {
                                Text(text = "Limit: $currency${String.format("%,.0f", acc.creditLimit)}", color = LuxMuted, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Basic timeline indicator
        Text(text = "COMMITMENT TIMELINE", style = Typography.labelLarge, color = LuxGoldChange, letterSpacing = 2.sp)
        Spacer(modifier = Modifier.height(10.dp))
        val unpaidBills = bills.filter { b ->
            val timestamp = b.lastPaidTimestamp
            if (timestamp == 0L) true else {
                val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
                val today = Calendar.getInstance()
                cal.get(Calendar.MONTH) != today.get(Calendar.MONTH)
            }
        }
        if (unpaidBills.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = LuxDarkGray)
            ) {
                Box(modifier = Modifier.padding(16.dp), contentAlignment = Alignment.Center) {
                    Text(text = "All commitments secured or no recurring items planned this cycle.", color = LuxGreen, textAlign = TextAlign.Center, fontSize = 13.sp)
                }
            }
        } else {
            unpaidBills.forEach { bill ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.CalendarToday, contentDescription = null, tint = LuxMuted, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Due on Day ${bill.dueDate}: ${bill.name}", color = LuxIvory, fontSize = 14.sp)
                    }
                    Text(text = "$currency${bill.amount}", color = LuxError, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ---------------- TRANSACTIONS TAB ----------------
@Composable
fun TransactionsScreen(
    viewModel: FinanceViewModel,
    transactions: List<Transaction>,
    accounts: List<Account>,
    currency: String
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("All") } // "All", "Income", "Expense", "Savings"

    val filteredTransactions = transactions.filter { tx ->
        val matchesSearch = tx.merchant.lowercase().contains(searchQuery.lowercase()) ||
                tx.category.lowercase().contains(searchQuery.lowercase()) ||
                tx.note.lowercase().contains(searchQuery.lowercase())

        val matchesFilter = when (selectedFilter) {
            "Income" -> tx.type == "Income"
            "Expense" -> tx.type == "Expense"
            "Savings" -> tx.type == "Savings"
            else -> true
        }

        matchesSearch && matchesFilter
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(text = "TRANSACTION LEDGER", style = Typography.labelLarge, color = LuxGoldChange, letterSpacing = 3.sp)
        Text(text = "Frictionless Record-Keeping", style = Typography.headlineMedium, color = LuxIvory)

        Spacer(modifier = Modifier.height(16.dp))

        // Search Field
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search by merchant, category or item name...", color = LuxMuted) },
            leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = LuxMuted) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = LuxGoldChange,
                unfocusedBorderColor = LuxCardGray,
                focusedTextColor = LuxIvory,
                unfocusedTextColor = LuxIvory
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Filters UI
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val filters = listOf("All", "Income", "Expense", "Savings")
            filters.forEach { filter ->
                val isSelected = selectedFilter == filter
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) LuxGoldChange else LuxDarkGray)
                        .clickable { selectedFilter = filter }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = filter,
                        color = if (isSelected) LuxBlack else LuxIvory,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Ledger list
        if (filteredTransactions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1.0f),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "Zero ledger records match search criteria.", color = LuxMuted, textAlign = TextAlign.Center)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1.0f)
            ) {
                items(filteredTransactions) { tx ->
                    val dateStr = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(tx.date))
                    val sourceAccount = accounts.find { it.id == tx.accountId }?.name ?: "Vault"

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 5.dp),
                        colors = CardDefaults.cardColors(containerColor = LuxDarkGray),
                        border = BorderStroke(1.dp, LuxCardGray)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = tx.merchant.uppercase(),
                                        color = LuxIvory,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(LuxCardGray)
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(text = tx.category, color = LuxGoldChange, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                                Text(text = "Paid out of: $sourceAccount • $dateStr", color = LuxMuted, fontSize = 11.sp)
                                if (tx.note.isNotBlank()) {
                                    Text(text = tx.note, color = LuxGoldLight, fontSize = 12.sp, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic, modifier = Modifier.padding(top = 4.dp))
                                }
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                val valuePrefix = when (tx.type) {
                                    "Income", "Refund" -> "+"
                                    else -> "-"
                                }
                                val valueColor = when (tx.type) {
                                    "Income", "Refund" -> LuxGreen
                                    "Savings" -> LuxIceBlue
                                    else -> LuxIvory
                                }
                                Text(
                                    text = "$valuePrefix$currency${String.format("%,.2f", tx.amount)}",
                                    color = valueColor,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    modifier = Modifier.padding(end = 8.dp)
                                )
                                IconButton(onClick = { viewModel.deleteTransaction(tx) }) {
                                    Icon(imageVector = Icons.Default.DeleteOutline, contentDescription = "Delete item", tint = LuxError.copy(alpha = 0.7f), modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ---------------- PLAN SCREEN TAB ----------------
@Composable
fun PlanScreen(
    viewModel: FinanceViewModel,
    bills: List<BillSubscription>,
    goals: List<FinancialGoal>,
    debts: List<Debt>,
    accounts: List<Account>,
    currency: String,
    mode: String,
    totalSalary: Double,
    onAddBillClick: () -> Unit,
    onAddGoalClick: () -> Unit,
    onAddDebtClick: () -> Unit
) {
    var subTab by remember { mutableStateOf("budget") } // "budget", "bills", "debts", "goals"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(text = "WEALTH STRATEGIZER", style = Typography.labelLarge, color = LuxGoldChange, letterSpacing = 3.sp)
        Text(text = "Long-term Planning Hub", style = Typography.headlineMedium, color = LuxIvory)

        Spacer(modifier = Modifier.height(16.dp))

        // Horizontal Category Tabs
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val tabs = listOf(
                "budget" to "Budget Allocator",
                "bills" to "Bills & Subs",
                "debts" to "Debt & EMI",
                "goals" to "Savings Goals"
            )
            tabs.forEach { (key, label) ->
                val isSelected = subTab == key
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) LuxGoldChange else LuxDarkGray)
                        .clickable { subTab = key }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(text = label, color = if (isSelected) LuxBlack else LuxIvory, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1.0f)
        ) {
            when (subTab) {
                "budget" -> BudgetSubView(currency = currency, salary = totalSalary)
                "bills" -> BillsSubView(bills = bills, accounts = accounts, currency = currency, onAddClick = onAddBillClick, onPay = { b, accId -> viewModel.payBill(b, accId) }, onDelete = { b -> viewModel.deleteBill(b) })
                "debts" -> DebtsSubView(debts = debts, currency = currency, onAddClick = onAddDebtClick, onDelete = { d -> viewModel.deleteDebt(d) })
                "goals" -> GoalsSubView(goals = goals, accounts = accounts, currency = currency, onAddClick = onAddGoalClick, onAddFunds = { g, bal, accId -> viewModel.addFundsToGoal(g, bal, accId) }, onDelete = { g -> viewModel.deleteGoal(g) })
            }
        }
    }
}

@Composable
fun BudgetSubView(currency: String, salary: Double) {
    val items = listOf(
        Triple("Essentials (Rent, Utilities)", 0.40, LuxGoldLight),
        Triple("Minimum Debts & EMIs", 0.20, LuxGoldChange),
        Triple("Protected Savings Goals", 0.15, LuxIceBlue),
        Triple("Flexible Wants & Dining", 0.15, LuxMuted),
        Triple("Unallocated Cash Buffer", 0.10, LuxIvory)
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Text(text = "MONTHLY BASELINE RESOLUTIVE ALLOCATION", style = Typography.labelLarge, color = LuxGoldChange, fontSize = 11.sp, modifier = Modifier.padding(bottom = 8.dp))
        Text(text = "Optimal cash separation models computed automatically derived from net income of $currency${String.format("%,.0f", salary)}.", color = LuxMuted, fontSize = 13.sp)

        Spacer(modifier = Modifier.height(20.dp))

        items.forEach { (name, percent, color) ->
            val targetAmount = salary * percent
            Column(modifier = Modifier.padding(vertical = 10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = name, color = LuxIvory, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text(text = "$currency${String.format("%,.0f", targetAmount)} (${(percent * 100).toInt()}%)", color = color, fontWeight = FontWeight.SemiBold)
                }
                Spacer(modifier = Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { percent.toFloat() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = color,
                    trackColor = LuxCardGray,
                )
            }
        }
    }
}

@Composable
fun BillsSubView(
    bills: List<BillSubscription>,
    accounts: List<Account>,
    currency: String,
    onAddClick: () -> Unit,
    onPay: (BillSubscription, Long) -> Unit,
    onDelete: (BillSubscription) -> Unit
) {
    var showPaySelectorDialog by remember { mutableStateOf<BillSubscription?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "Recurring Obligations", color = LuxGoldChange, style = Typography.labelLarge)
            Button(
                onClick = onAddClick,
                colors = ButtonDefaults.buttonColors(containerColor = LuxGoldChange, contentColor = LuxBlack),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("+ Bill Item", fontSize = 11.sp)
            }
        }
        Spacer(modifier = Modifier.height(12.dp))

        if (bills.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = "No recurring bills or subscriptions scheduled. Add critical items to enable Safe To Spend protection.", color = LuxMuted, textAlign = TextAlign.Center)
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(bills) { b ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 5.dp),
                        colors = CardDefaults.cardColors(containerColor = LuxDarkGray),
                        border = BorderStroke(1.dp, LuxCardGray)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = b.name.uppercase(), color = LuxIvory, fontWeight = FontWeight.Bold)
                                Text(text = "Category: ${b.category} • Due monthly: Day ${b.dueDate}", color = LuxMuted, fontSize = 12.sp)
                                if (b.isSubscription) {
                                    Text(text = "Subscription active", color = LuxIceBlue, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(text = "$currency${b.amount}", color = LuxError, fontWeight = FontWeight.Bold)
                                Row(modifier = Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    IconButton(onClick = { onDelete(b) }) {
                                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete bill", tint = LuxMuted, modifier = Modifier.size(18.dp))
                                    }
                                    Button(
                                        onClick = { showPaySelectorDialog = b },
                                        colors = ButtonDefaults.buttonColors(containerColor = LuxGreen, contentColor = LuxIvory),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text("Mark Paid", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (showPaySelectorDialog != null) {
            PayBillSourceSelectionDialog(
                billName = showPaySelectorDialog!!.name,
                billAmount = showPaySelectorDialog!!.amount,
                accounts = accounts,
                currency = currency,
                onDismiss = { showPaySelectorDialog = null },
                onPaySelect = { accId ->
                    onPay(showPaySelectorDialog!!, accId)
                    showPaySelectorDialog = null
                }
            )
        }
    }
}

@Composable
fun PayBillSourceSelectionDialog(
    billName: String,
    billAmount: Double,
    accounts: List<Account>,
    currency: String,
    onDismiss: () -> Unit,
    onPaySelect: (Long) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = LuxDarkGray),
            border = BorderStroke(1.dp, LuxGoldChange)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Text(text = "FUND SOURCE SELECTION", style = Typography.labelLarge, color = LuxGoldChange)
                Text(text = "Pay $billName ($currency$billAmount)", style = Typography.titleLarge, color = LuxIvory, modifier = Modifier.padding(bottom = 16.dp))

                if (accounts.isEmpty()) {
                    Text(text = "Please configure an account profile in Home tab before paying bills.", color = LuxError, fontSize = 12.sp)
                } else {
                    accounts.filter { it.type != "Credit Card" }.forEach { acc ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 5.dp)
                                .clickable { onPaySelect(acc.id) },
                            colors = CardDefaults.cardColors(containerColor = LuxCardGray)
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = acc.name, color = LuxIvory, fontWeight = FontWeight.Bold)
                                Text(text = "$currency${acc.balance}", color = LuxGoldLight)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                OutlinedButton(
                    onClick = onDismiss,
                    border = BorderStroke(1.dp, LuxMuted),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = LuxMuted),
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Cancel")
                }
            }
        }
    }
}

@Composable
fun DebtsSubView(
    debts: List<Debt>,
    currency: String,
    onAddClick: () -> Unit,
    onDelete: (Debt) -> Unit
) {
    var simulatorDebtAmount by remember { mutableStateOf("50000") }
    var extraPaymentInput by remember { mutableStateOf("2000") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "LIABILITY ENGINES", color = LuxGoldChange, style = Typography.labelLarge)
            Button(
                onClick = onAddClick,
                colors = ButtonDefaults.buttonColors(containerColor = LuxGoldChange, contentColor = LuxBlack),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("+ Liability", fontSize = 11.sp)
            }
        }
        Spacer(modifier = Modifier.height(12.dp))

        if (debts.isEmpty()) {
            Text(text = "No outstandings found. Complete setup to evaluate amortization speed improvements.", color = LuxMuted, style = Typography.bodyLarge, modifier = Modifier.padding(vertical = 8.dp))
        } else {
            debts.forEach { debt ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 5.dp),
                    colors = CardDefaults.cardColors(containerColor = LuxDarkGray),
                    border = BorderStroke(1.dp, LuxCardGray)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = debt.name.uppercase(), color = LuxIvory, fontWeight = FontWeight.Bold)
                            IconButton(onClick = { onDelete(debt) }, modifier = Modifier.size(24.dp)) {
                                Icon(imageVector = Icons.Default.Delete, contentDescription = null, tint = LuxError.copy(alpha = 0.5f))
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(text = "Outstanding Principal", color = LuxMuted, fontSize = 11.sp)
                                Text(text = "$currency${String.format("%,.2f", debt.outstandingAmount)}", color = LuxIvory, fontWeight = FontWeight.Bold)
                            }
                            Column {
                                Text(text = "Estimated EMI value", color = LuxMuted, fontSize = 11.sp)
                                Text(text = "$currency${String.format("%,.0f", debt.emiAmount)}/mo", color = LuxGoldChange, fontWeight = FontWeight.SemiBold)
                            }
                            Column {
                                Text(text = "Interest Rate", color = LuxMuted, fontSize = 11.sp)
                                Text(text = "${debt.interestRate}% APR", color = LuxIvory)
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Debt avalanche vs Snowball comparison simulator
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = LuxCardGray),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, LuxGoldChange.copy(alpha = 0.4f))
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(text = "REPAYMENT ENGINE SIMULATION", style = Typography.labelLarge, color = LuxGoldChange)
                Text(text = "Accelerated amortization estimates computed in real-time.", color = LuxIvory, fontSize = 12.sp, modifier = Modifier.padding(bottom = 12.dp))

                OutlinedTextField(
                    value = simulatorDebtAmount,
                    onValueChange = { simulatorDebtAmount = it },
                    label = { Text("Outstanding Debt Balance Simulator") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = LuxGoldChange,
                        unfocusedBorderColor = LuxDarkGray
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = extraPaymentInput,
                    onValueChange = { extraPaymentInput = it },
                    label = { Text("Extra monthly repayment buffer") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = LuxGoldChange,
                        unfocusedBorderColor = LuxDarkGray
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                val principalVal = simulatorDebtAmount.toDoubleOrNull() ?: 50000.0
                val extraPayment = extraPaymentInput.toDoubleOrNull() ?: 2000.0
                val normMonths = (principalVal / (principalVal * 0.05).coerceAtLeast(100.0)).toInt()
                val acceleratedMonths = (principalVal / ((principalVal * 0.05).coerceAtLeast(100.0) + extraPayment)).toInt()

                Text(
                    text = "Amortization schedule drops from $normMonths months to $acceleratedMonths months.",
                    color = LuxGreen,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Text(
                    text = "Saves approximately ${((principalVal * 0.01) * (normMonths - acceleratedMonths)).toInt().coerceAtLeast(0)} $currency in estimated interest charges.",
                    color = LuxIvory,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

@Composable
fun GoalsSubView(
    goals: List<FinancialGoal>,
    accounts: List<Account>,
    currency: String,
    onAddClick: () -> Unit,
    onAddFunds: (FinancialGoal, Double, Long) -> Unit,
    onDelete: (FinancialGoal) -> Unit
) {
    var showFundGoalDialog by remember { mutableStateOf<FinancialGoal?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "RESERVE WEALTH BUCKETS", color = LuxGoldChange, style = Typography.labelLarge)
            Button(
                onClick = onAddClick,
                colors = ButtonDefaults.buttonColors(containerColor = LuxGoldChange, contentColor = LuxBlack),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("+ Goal Bucket", fontSize = 11.sp)
            }
        }
        Spacer(modifier = Modifier.height(12.dp))

        if (goals.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = "No custom target buckets configured. Create savings goals or a mini-emergency fund.", color = LuxMuted, textAlign = TextAlign.Center)
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(goals) { g ->
                    val percentage = (g.currentAmount / g.targetAmount).toFloat().coerceIn(0.0f, 1.0f)
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 5.dp),
                        colors = CardDefaults.cardColors(containerColor = LuxDarkGray),
                        border = BorderStroke(1.dp, LuxCardGray)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(text = g.name.uppercase(), color = LuxIvory, fontWeight = FontWeight.Bold)
                                    Text(text = "Priority: ${g.priority} • Saved: $currency${String.format("%,.0f", g.currentAmount)} / $currency${String.format("%,.0f", g.targetAmount)}", color = LuxMuted, fontSize = 12.sp)
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    IconButton(onClick = { onDelete(g) }) {
                                        Icon(imageVector = Icons.Default.Delete, contentDescription = null, tint = LuxError.copy(alpha = 0.5f), modifier = Modifier.size(20.dp))
                                    }
                                    Button(
                                        onClick = { showFundGoalDialog = g },
                                        colors = ButtonDefaults.buttonColors(containerColor = LuxGoldChange, contentColor = LuxBlack),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text("+ Fund", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            LinearProgressIndicator(
                                progress = { percentage },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp)),
                                color = LuxGoldChange,
                                trackColor = LuxCardGray,
                            )
                        }
                    }
                }
            }
        }

        if (showFundGoalDialog != null) {
            FundGoalSourceSelectionDialog(
                goalName = showFundGoalDialog!!.name,
                accounts = accounts,
                currency = currency,
                onDismiss = { showFundGoalDialog = null },
                onFundSelect = { amt, accId ->
                    onAddFunds(showFundGoalDialog!!, amt, accId)
                    showFundGoalDialog = null
                }
            )
        }
    }
}

@Composable
fun FundGoalSourceSelectionDialog(
    goalName: String,
    accounts: List<Account>,
    currency: String,
    onDismiss: () -> Unit,
    onFundSelect: (Double, Long) -> Unit
) {
    var amountInput by remember { mutableStateOf("1000") }
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = LuxDarkGray),
            border = BorderStroke(1.dp, LuxGoldChange)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Text(text = "FUND GOAL SECUREMENT", style = Typography.labelLarge, color = LuxGoldChange)
                Text(text = "Fund: $goalName", style = Typography.titleLarge, color = LuxIvory)

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = amountInput,
                    onValueChange = { amountInput = it },
                    label = { Text("Allocation amount") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = LuxGoldChange,
                        unfocusedBorderColor = LuxDarkGray
                    ),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                )

                if (accounts.isEmpty()) {
                    Text(text = "Configure an active checking account under Home tab first.", color = LuxError, fontSize = 12.sp)
                } else {
                    accounts.filter { it.type != "Credit Card" }.forEach { acc ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 5.dp)
                                .clickable { onFundSelect(amountInput.toDoubleOrNull() ?: 0.0, acc.id) },
                            colors = CardDefaults.cardColors(containerColor = LuxCardGray)
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = acc.name, color = LuxIvory, fontWeight = FontWeight.Bold)
                                Text(text = "$currency${acc.balance}", color = LuxGoldLight)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                OutlinedButton(
                    onClick = onDismiss,
                    border = BorderStroke(1.dp, LuxMuted),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = LuxMuted),
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Cancel")
                }
            }
        }
    }
}

// ---------------- INSIGHTS TAB screen ----------------
@Composable
fun InsightsScreen(
    viewModel: FinanceViewModel,
    transactions: List<Transaction>,
    debts: List<Debt>,
    profile: UserProfile?,
    currency: String,
    totalUsableBalance: Double,
    totalDebt: Double
) {
    // Scenario simulator states
    var mockSalaryGrowth by remember { mutableStateOf("10") } // percent
    var mockRentGrowth by remember { mutableStateOf("5") } // percent

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(text = "CLARITY RADAR", style = Typography.labelLarge, color = LuxGoldChange, letterSpacing = 3.sp)
        Text(text = "Financial Health Audit", style = Typography.headlineMedium, color = LuxIvory)

        Spacer(modifier = Modifier.height(20.dp))

        // Key finance ratios
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = LuxDarkGray),
            border = BorderStroke(1.dp, LuxCardGray)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(text = "SYSTEM RESILIENCE PARAMETERS", style = Typography.labelLarge, color = LuxGoldChange)
                Spacer(modifier = Modifier.height(16.dp))

                // Ratio 1: Debt to income
                val netSalary = profile?.salaryAmount ?: 60000.0
                val totalMinPayments = debts.sumOf { it.minimumPayment }
                val debtRatio = if (netSalary > 0) (totalMinPayments / netSalary) * 100 else 0.0

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Debt Commitment Ratio", color = LuxIvory)
                    Text(text = "${String.format("%.1f", debtRatio)}%", color = if (debtRatio > 35) LuxError else LuxGreen, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = "Recommended upper safety boundary is 35%. Current: ${(if (debtRatio > 35) "Excessive burden. Limit new EMIs." else "Healthy ratio.")}", color = LuxMuted, fontSize = 12.sp)

                Spacer(modifier = Modifier.height(16.dp))
                Divider(color = LuxCardGray)
                Spacer(modifier = Modifier.height(16.dp))

                // Ratio 2: Leverage metric
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Leverage to Cash", color = LuxIvory)
                    Text(text = "${String.format("%.1f", if (totalUsableBalance > 0) (totalDebt / totalUsableBalance) * 100 else 0.0)}%", color = LuxGoldLight, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Spending Leak Detection
        Text(text = "LEAK ALERTS & VULNERABILITIES", style = Typography.labelLarge, color = LuxGoldChange, letterSpacing = 2.sp)
        Spacer(modifier = Modifier.height(10.dp))

        val flexTransactions = transactions.filter { it.type == "Expense" && (it.category == "Shopping" || it.category == "Dining" || it.category == "Travel") }
        if (flexTransactions.size >= 3) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = LuxDarkGray),
                border = BorderStroke(1.dp, LuxError.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Warning, contentDescription = null, tint = LuxError)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Impulse Spend Leaks Observed", color = LuxIvory, fontWeight = FontWeight.Bold)
                    }
                    Text(
                        text = "You recorded ${flexTransactions.size} shopping/dining entries this week. Reducing these inputs by 15% secures your safety buffer baseline values.",
                        color = LuxMuted,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(imageVector = Icons.Default.Verified, contentDescription = null, tint = LuxGreen)
                Spacer(modifier = Modifier.width(10.dp))
                Text(text = "Zero high frequency spend leaks detected. Financial boundaries secured.", color = LuxMuted, fontSize = 13.sp)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Scenario Simulator Section (4.1/32)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = LuxCardGray),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, LuxGoldChange.copy(alpha = 0.3f))
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(text = "DECISION SCENARIO SIMULATOR", style = Typography.labelLarge, color = LuxGoldChange)
                Text(text = "Test upcoming choices without risk to real cash metrics.", color = LuxIvory, fontSize = 12.sp)

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = mockSalaryGrowth,
                    onValueChange = { mockSalaryGrowth = it },
                    label = { Text("Anticipated Salary Increase %") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = LuxGoldChange),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = mockRentGrowth,
                    onValueChange = { mockRentGrowth = it },
                    label = { Text("Anticipated Rent Increase %") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = LuxGoldChange),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(20.dp))

                val salGrow = (mockSalaryGrowth.toDoubleOrNull() ?: 10.0) / 100.0
                val rentGrow = (mockRentGrowth.toDoubleOrNull() ?: 5.0) / 100.0

                val currentIncome = profile?.salaryAmount ?: 60000.0
                val estimatedIncomeSurplus = currentIncome * salGrow
                val rentExpenseEstimate = currentIncome * 0.30
                val estimatedRentRaise = rentExpenseEstimate * rentGrow

                val netChange = estimatedIncomeSurplus - estimatedRentRaise

                Text(
                    text = "Projected Monthly Impact: ${if (netChange >= 0) "+" else ""}$currency${String.format("%,.0f", netChange)} /month",
                    color = if (netChange >= 0) LuxGreen else LuxError,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Text(
                    text = "This adjustment would drift your Safe To Spend baseline limit by $currency${String.format("%,.0f", netChange / 30)} per day.",
                    color = LuxIvory,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

// ---------------- COACH TAB SCREEN ----------------
@Composable
fun CoachScreen(viewModel: FinanceViewModel, currency: String) {
    val chatMessages by viewModel.chatMessages.collectAsStateWithLifecycle()
    val isLoading by viewModel.isChatLoading.collectAsStateWithLifecycle()

    var textInput by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(text = "AUREN INTEGRITY OS", style = Typography.labelLarge, color = LuxGoldChange, letterSpacing = 3.sp)
        Text(text = "AI Money Coach Insights", style = Typography.headlineMedium, color = LuxIvory)

        Spacer(modifier = Modifier.height(16.dp))

        // Chat conversation ledger
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1.0f)
                .background(LuxDarkGray, RoundedCornerShape(12.dp))
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(chatMessages) { msg ->
                val bubbleBg = if (msg.isUser) LuxCardGray else LuxBlack
                val strokeColor = if (msg.isUser) LuxGoldChange.copy(alpha = 0.3f) else LuxCardGray
                val alignment = if (msg.isUser) Alignment.End else Alignment.Start

                Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = alignment) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = bubbleBg),
                        border = BorderStroke(1.dp, strokeColor),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = msg.text,
                            color = LuxIvory,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }

            if (isLoading) {
                item {
                    Text(text = "Calculating OS grounding parameters...", color = LuxGoldChange, fontSize = 11.sp, modifier = Modifier.padding(8.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Instant preset suggestions to maintain section 22 coaching
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val prompts = listOf(
                "Why is my safe-to-spend low?",
                "Can I spend ${currency}5,000 on shopping?",
                "Suggest optimization strategies",
                "How to accelerate my EMIs?"
            )
            prompts.forEach { pr ->
                Card(
                    modifier = Modifier
                        .clickable {
                            viewModel.askAiCoach(pr)
                        },
                    shape = RoundedCornerShape(6.dp),
                    colors = CardDefaults.cardColors(containerColor = LuxCardGray)
                ) {
                    Text(text = pr, color = LuxIvory, fontSize = 11.sp, modifier = Modifier.padding(8.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Chat Inputs
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = textInput,
                onValueChange = { textInput = it },
                placeholder = { Text("Consult private wealth engine...", color = LuxMuted) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = LuxGoldChange,
                    unfocusedBorderColor = LuxCardGray,
                    focusedTextColor = LuxIvory,
                    unfocusedTextColor = LuxIvory
                ),
                modifier = Modifier.weight(1.0f)
            )

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = {
                    if (textInput.isNotBlank()) {
                        viewModel.askAiCoach(textInput)
                        textInput = ""
                    }
                },
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(LuxGoldChange)
            ) {
                Icon(imageVector = Icons.Default.Send, contentDescription = "Query", tint = LuxBlack)
            }
        }
    }
}

// ---------------- DIALOG COMPONENTS ----------------

@Composable
fun AddTransactionDialog(
    accounts: List<Account>,
    currency: String,
    onDismiss: () -> Unit,
    onAdd: (Double, String, Long, String, String, String, Long?) -> Unit
) {
    var amount by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf("Expense") } // "Income", "Expense", "Savings", "Transfer", "Refund"
    var selectedAccountId by remember { mutableStateOf(accounts.firstOrNull()?.id ?: 0L) }
    var targetAccountId by remember { mutableStateOf<Long?>(null) }
    var category by remember { mutableStateOf("Shopping") }
    var merchant by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }

    val categories = listOf("Groceries", "Rent", "Utilities", "Shopping", "Dining", "Savings", "Investment", "EMI", "Other")

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = LuxDarkGray),
            border = BorderStroke(1.dp, LuxGoldChange)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(text = "VALUATIVE LEDGER INPUT", style = Typography.labelLarge, color = LuxGoldChange)
                Text(text = "Add Transaction Record", style = Typography.titleLarge, color = LuxIvory, modifier = Modifier.padding(bottom = 16.dp))

                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Transaction Value") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = LuxGoldChange),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(text = "TRANSACTION METHODOLOGY", color = LuxGoldChange, fontSize = 11.sp, modifier = Modifier.padding(vertical = 4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val types = listOf("Expense", "Income", "Savings", "Transfer")
                    types.forEach { tp ->
                        val isSelected = selectedType == tp
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSelected) LuxGoldChange else LuxCardGray)
                                .clickable { selectedType = tp }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(text = tp, color = if (isSelected) LuxBlack else LuxIvory, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(text = "SOURCE VAULT", color = LuxGoldChange, fontSize = 11.sp)
                accounts.forEach { acc ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedAccountId = acc.id }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (selectedAccountId == acc.id) Icons.Default.RadioButtonChecked else Icons.Default.RadioButtonUnchecked,
                            contentDescription = null,
                            tint = if (selectedAccountId == acc.id) LuxGoldChange else LuxMuted
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "${acc.name} ($currency${acc.balance})", color = LuxIvory, fontSize = 13.sp)
                    }
                }

                if (selectedType == "Transfer") {
                    Text(text = "DESTINATION VAULT", color = LuxGoldChange, fontSize = 11.sp, modifier = Modifier.padding(top = 8.dp))
                    accounts.filter { it.id != selectedAccountId }.forEach { acc ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { targetAccountId = acc.id }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (targetAccountId == acc.id) Icons.Default.RadioButtonChecked else Icons.Default.RadioButtonUnchecked,
                                contentDescription = null,
                                tint = if (targetAccountId == acc.id) LuxGoldChange else LuxMuted
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "${acc.name} ($currency${acc.balance})", color = LuxIvory, fontSize = 13.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = merchant,
                    onValueChange = { merchant = it },
                    label = { Text("Recipient / Payee Name") },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = LuxGoldChange),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(text = "BUDGET CATEGORY", color = LuxGoldChange, fontSize = 11.sp)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    categories.forEach { cat ->
                        val isSelected = category == cat
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSelected) LuxGoldChange else LuxCardGray)
                                .clickable { category = cat }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(text = cat, color = if (isSelected) LuxBlack else LuxIvory, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Factual observations (notes)") },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = LuxGoldChange),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    OutlinedButton(onClick = onDismiss, border = BorderStroke(1.dp, LuxMuted)) {
                        Text("Dimiss", color = LuxIvory)
                    }

                    Button(
                        onClick = {
                            val amtVal = amount.toDoubleOrNull() ?: 0.0
                            if (amtVal > 0.0 && selectedAccountId != 0L) {
                                onAdd(amtVal, selectedType, selectedAccountId, category, merchant.ifBlank { "Direct Purchase" }, note, targetAccountId)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = LuxGoldChange, contentColor = LuxBlack)
                    ) {
                        Text("Add")
                    }
                }
            }
        }
    }
}

@Composable
fun AddAccountDialog(
    currency: String,
    onDismiss: () -> Unit,
    onAdd: (String, String, Double, Double, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("Savings") }
    var balance by remember { mutableStateOf("") }
    var limit by remember { mutableStateOf("") }
    var institution by remember { mutableStateOf("") }

    val types = listOf("Savings", "Salary", "Credit Card", "Cash", "Investment")

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = LuxDarkGray),
            border = BorderStroke(1.dp, LuxGoldChange)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(text = "VAULT ACCOUNT SETUP", style = Typography.labelLarge, color = LuxGoldChange)
                Text(text = "Add Asset Profile", style = Typography.titleLarge, color = LuxIvory, modifier = Modifier.padding(bottom = 16.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Signature Identification name") },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = LuxGoldChange),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(text = "VAULT SPECIFICATION", color = LuxGoldChange, fontSize = 11.sp, modifier = Modifier.padding(vertical = 4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    types.forEach { tp ->
                        val isSelected = type == tp
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSelected) LuxGoldChange else LuxCardGray)
                                .clickable { type = tp }
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Text(text = tp, color = if (isSelected) LuxBlack else LuxIvory, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = balance,
                    onValueChange = { balance = it },
                    label = { Text("Initial Ledger Balance Value") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = LuxGoldChange),
                    modifier = Modifier.fillMaxWidth()
                )

                if (type == "Credit Card") {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = limit,
                        onValueChange = { limit = it },
                        label = { Text("Approved Credit Limit Value") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = LuxGoldChange),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = institution,
                    onValueChange = { institution = it },
                    label = { Text("Institution Authority label") },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = LuxGoldChange),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    OutlinedButton(onClick = onDismiss, border = BorderStroke(1.dp, LuxMuted)) {
                        Text("Cancel", color = LuxIvory)
                    }

                    Button(
                        onClick = {
                            val bVal = balance.toDoubleOrNull() ?: 0.0
                            val limitVal = limit.toDoubleOrNull() ?: 0.0
                            if (name.isNotBlank()) {
                                onAdd(name, type, bVal, limitVal, institution)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = LuxGoldChange, contentColor = LuxBlack)
                    ) {
                        Text("Authorize")
                    }
                }
            }
        }
    }
}

@Composable
fun AddBillDialog(
    onDismiss: () -> Unit,
    onAdd: (String, Double, Int, String, Boolean) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var dueDate by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Utilities") }
    var isSubscription by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = LuxDarkGray),
            border = BorderStroke(1.dp, LuxGoldChange)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(text = "RECURRING SYSTEM LIABILITY", style = Typography.labelLarge, color = LuxGoldChange)
                Text(text = "Add Bill Schedule", style = Typography.titleLarge, color = LuxIvory, modifier = Modifier.padding(bottom = 16.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Service Profile / Provider") },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = LuxGoldChange),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Anticipated monthly cycle charge") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = LuxGoldChange),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = dueDate,
                    onValueChange = { dueDate = it },
                    label = { Text("Due Day of month (1-31)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = LuxGoldChange),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = isSubscription,
                        onCheckedChange = { isSubscription = it },
                        colors = CheckboxDefaults.colors(checkedColor = LuxGoldChange)
                    )
                    Text(text = "Is Subscription structure (e.g. Netflix, Spotify)", color = LuxIvory, fontSize = 13.sp)
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    OutlinedButton(onClick = onDismiss, border = BorderStroke(1.dp, LuxMuted)) {
                        Text("Cancel", color = LuxIvory)
                    }

                    Button(
                        onClick = {
                            val aVal = amount.toDoubleOrNull() ?: 0.0
                            val dVal = dueDate.toIntOrNull() ?: 1
                            if (name.isNotBlank()) {
                                onAdd(name, aVal, dVal, category, isSubscription)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = LuxGoldChange, contentColor = LuxBlack)
                    ) {
                        Text("Establish")
                    }
                }
            }
        }
    }
}

@Composable
fun AddGoalDialog(
    onDismiss: () -> Unit,
    onAdd: (String, Double, Double, Int, String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var target by remember { mutableStateOf("") }
    var current by remember { mutableStateOf("0") }
    var months by remember { mutableStateOf("12") }
    var priority by remember { mutableStateOf("Important") } // "Critical", "Important", "Lifestyle"

    val priorities = listOf("Critical", "Important", "Lifestyle", "Aspirational")

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = LuxDarkGray),
            border = BorderStroke(1.dp, LuxGoldChange)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(text = "FUTURE VENTURE SETUP", style = Typography.labelLarge, color = LuxGoldChange)
                Text(text = "Add Savings Bucket", style = Typography.titleLarge, color = LuxIvory, modifier = Modifier.padding(bottom = 16.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Target Achievement name") },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = LuxGoldChange),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = target,
                    onValueChange = { target = it },
                    label = { Text("Principal value required") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = LuxGoldChange),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = current,
                    onValueChange = { current = it },
                    label = { Text("Current capital held") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = LuxGoldChange),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = months,
                    onValueChange = { months = it },
                    label = { Text("Deadline tenure (Months)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = LuxGoldChange),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(text = "STRATEGIC PRIORITY", color = LuxGoldChange, fontSize = 11.sp)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    priorities.forEach { pr ->
                        val isSelected = priority == pr
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSelected) LuxGoldChange else LuxCardGray)
                                .clickable { priority = pr }
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Text(text = pr, color = if (isSelected) LuxBlack else LuxIvory, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    OutlinedButton(onClick = onDismiss, border = BorderStroke(1.dp, LuxMuted)) {
                        Text("Cancel", color = LuxIvory)
                    }

                    Button(
                        onClick = {
                            val tVal = target.toDoubleOrNull() ?: 10000.0
                            val cVal = current.toDoubleOrNull() ?: 0.0
                            val mVal = months.toIntOrNull() ?: 12
                            if (name.isNotBlank()) {
                                onAdd(name, tVal, cVal, mVal, priority, "Savings")
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = LuxGoldChange, contentColor = LuxBlack)
                    ) {
                        Text("Open Bucket")
                    }
                }
            }
        }
    }
}

@Composable
fun AddDebtDialog(
    onDismiss: () -> Unit,
    onAdd: (String, String, Double, Double, Double, Double, Int, Int) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("EMIs") }
    var outstanding by remember { mutableStateOf("") }
    var rate by remember { mutableStateOf("12") }
    var minPayment by remember { mutableStateOf("") }
    var emiAmount by remember { mutableStateOf("") }
    var dueDate by remember { mutableStateOf("5") }
    var tenure by remember { mutableStateOf("12") }

    val types = listOf("Personal Loan", "Credit Card", "EMIs", "Friends/Family")

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = LuxDarkGray),
            border = BorderStroke(1.dp, LuxGoldChange)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(text = "SECURED / UNSECURED LIABILITIES", style = Typography.labelLarge, color = LuxGoldChange)
                Text(text = "Add Liability Profile", style = Typography.titleLarge, color = LuxIvory, modifier = Modifier.padding(bottom = 16.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Liability / Loan identifier") },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = LuxGoldChange),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    types.forEach { tp ->
                        val isSelected = type == tp
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSelected) LuxGoldChange else LuxCardGray)
                                .clickable { type = tp }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(text = tp, color = if (isSelected) LuxBlack else LuxIvory, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = outstanding,
                    onValueChange = { outstanding = it },
                    label = { Text("Remaining Outstanding Balance") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = LuxGoldChange),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = rate,
                    onValueChange = { rate = it },
                    label = { Text("Contractual APR rate %") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = LuxGoldChange),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = emiAmount,
                    onValueChange = { emiAmount = it },
                    label = { Text("Monthly payment installment") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = LuxGoldChange),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = minPayment,
                    onValueChange = { minPayment = it },
                    label = { Text("Minimum required payment of cycle") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = LuxGoldChange),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    OutlinedButton(onClick = onDismiss, border = BorderStroke(1.dp, LuxMuted)) {
                        Text("Cancel", color = LuxIvory)
                    }

                    Button(
                        onClick = {
                            val outVal = outstanding.toDoubleOrNull() ?: 15000.0
                            val rateVal = rate.toDoubleOrNull() ?: 12.0
                            val emiVal = emiAmount.toDoubleOrNull() ?: 1200.0
                            val minVal = minPayment.ifBlank { emiAmount }.toDoubleOrNull() ?: emiVal
                            val dueVal = dueDate.toIntOrNull() ?: 5
                            val tVal = tenure.toIntOrNull() ?: 12

                            if (name.isNotBlank()) {
                                onAdd(name, type, outVal, rateVal, minVal, emiVal, dueVal, tVal)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = LuxGoldChange, contentColor = LuxBlack)
                    ) {
                        Text("Establish")
                    }
                }
            }
        }
    }
}

@Composable
fun WeeklyCheckInDialog(
    reviews: List<WeeklyReview>,
    onDismiss: () -> Unit,
    onSave: (String, String, String, Double, String, String) -> Unit
) {
    var step by remember { mutableStateOf(1) }

    var positiveAchievement by remember { mutableStateOf("Spent 15% less this week on non-essentials.") }
    var reasonForOverspend by remember { mutableStateOf("Dining out values higher than projected.") }
    var categoryExceeded by remember { mutableStateOf("Dining") }
    var auditAdjustmentInput by remember { mutableStateOf("1500") }
    var leakToMonitor by remember { mutableStateOf("Food Delivery Apps") }
    var futureAction by remember { mutableStateOf("Restore Safe To Spend limits by avoiding weekend delivery.") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = LuxDarkGray),
            border = BorderStroke(1.dp, LuxGoldChange),
            modifier = Modifier.padding(vertical = 12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(text = "INTEGRITY AUDIT PROCESS (Step $step of 3)", style = Typography.labelLarge, color = LuxGoldChange)
                Text(text = "Weekly Money Check-In", style = Typography.titleLarge, color = LuxIvory, modifier = Modifier.padding(bottom = 12.dp))

                when (step) {
                    1 -> {
                        OutlinedTextField(
                            value = positiveAchievement,
                            onValueChange = { positiveAchievement = it },
                            label = { Text("What milestone/achievement went well?") },
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = LuxGoldChange),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                        )
                    }
                    2 -> {
                        OutlinedTextField(
                            value = reasonForOverspend,
                            onValueChange = { reasonForOverspend = it },
                            label = { Text("Which priorities caused budget friction?") },
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = LuxGoldChange),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        OutlinedTextField(
                            value = categoryExceeded,
                            onValueChange = { categoryExceeded = it },
                            label = { Text("Enter Category affected") },
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = LuxGoldChange),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                        )
                    }
                    3 -> {
                        OutlinedTextField(
                            value = leakToMonitor,
                            onValueChange = { leakToMonitor = it },
                            label = { Text("Identify central spending leak") },
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = LuxGoldChange),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        OutlinedTextField(
                            value = futureAction,
                            onValueChange = { futureAction = it },
                            label = { Text("One primary action for next week cycle") },
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = LuxGoldChange),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    if (step > 1) {
                        OutlinedButton(onClick = { step-- }, border = BorderStroke(1.dp, LuxGoldChange)) {
                            Text("Back", color = LuxIvory)
                        }
                    } else {
                        OutlinedButton(onClick = onDismiss, border = BorderStroke(1.dp, LuxMuted)) {
                            Text("Dismiss", color = LuxIvory)
                        }
                    }

                    Button(
                        onClick = {
                            if (step < 3) {
                                step++
                            } else {
                                onSave(
                                    positiveAchievement,
                                    reasonForOverspend,
                                    categoryExceeded,
                                    auditAdjustmentInput.toDoubleOrNull() ?: 0.0,
                                    leakToMonitor,
                                    futureAction
                                )
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = LuxGoldChange, contentColor = LuxBlack)
                    ) {
                        Text(if (step == 3) "Conclude Audit" else "Continue")
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsDialog(
    profile: UserProfile?,
    onDismiss: () -> Unit,
    onSave: (String, String, String, Double, Double) -> Unit
) {
    var rawCurrency by remember { mutableStateOf(profile?.currency ?: "₹") }
    var rawObjective by remember { mutableStateOf(profile?.primaryObjective ?: "Control spending") }
    var rawMode by remember { mutableStateOf(profile?.appMode ?: "Strict Mode") }
    var rawSalary by remember { mutableStateOf(profile?.salaryAmount?.toString() ?: "60000") }
    var rawBuffer by remember { mutableStateOf(profile?.safetyBuffer?.toString() ?: "2000") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = LuxDarkGray),
            border = BorderStroke(1.dp, LuxGoldChange)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(text = "OS CONTROL SYSTEM", style = Typography.labelLarge, color = LuxGoldChange)
                Text(text = "App Mode & Controls Setup", style = Typography.titleLarge, color = LuxIvory, modifier = Modifier.padding(bottom = 16.dp))

                OutlinedTextField(
                    value = rawSalary,
                    onValueChange = { rawSalary = it },
                    label = { Text("Net net net Salary amount") },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = LuxGoldChange),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = rawBuffer,
                    onValueChange = { rawBuffer = it },
                    label = { Text("Safety Reserve baseline buffer") },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = LuxGoldChange),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(text = "CURRENCY SYMBOL", color = LuxGoldChange, fontSize = 11.sp)
                val currs = listOf("₹", "$", "€", "£")
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    currs.forEach { c ->
                        val isSelected = rawCurrency == c
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSelected) LuxGoldChange else LuxCardGray)
                                .clickable { rawCurrency = c }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(text = c, color = if (isSelected) LuxBlack else LuxIvory, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(text = "SYSTEM REGIONAL MODE POLICY", color = LuxGoldChange, fontSize = 11.sp)
                val modes = listOf("Strict Mode", "Balanced Mode", "Relaxed Mode")
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    modes.forEach { m ->
                        val isSelected = rawMode == m
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSelected) LuxGoldChange else LuxDarkGray)
                                .clickable { rawMode = m }
                                .padding(12.dp)
                        ) {
                            Text(text = m, color = if (isSelected) LuxBlack else LuxIvory, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    OutlinedButton(onClick = onDismiss, border = BorderStroke(1.dp, LuxMuted)) {
                        Text("Dimiss", color = LuxIvory)
                    }

                    Button(
                        onClick = {
                            val sVal = rawSalary.toDoubleOrNull() ?: 60000.0
                            val bVal = rawBuffer.toDoubleOrNull() ?: 2000.0
                            onSave(rawCurrency, rawObjective, rawMode, sVal, bVal)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = LuxGoldChange, contentColor = LuxBlack)
                    ) {
                        Text("Apply Rules")
                    }
                }
            }
        }
    }
}
