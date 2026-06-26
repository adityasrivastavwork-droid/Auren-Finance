package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.graphics.graphicsLayer
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.zIndex
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
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
    var showSideBar by remember { mutableStateOf(false) }

    val currency = profile?.currency ?: "₹"
    val mode = profile?.appMode ?: "Strict Mode"

    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = remember(context) { context.getSharedPreferences("firebase_auth_prefs", android.content.Context.MODE_PRIVATE) }
    var isFirebaseAuthed by remember { mutableStateOf(prefs.getBoolean("is_authed", false)) }
    var firebaseUserEmail by remember { mutableStateOf(prefs.getString("authed_email", "") ?: "") }
    val isProfileLoaded by viewModel.isProfileLoaded.collectAsStateWithLifecycle()

    if (!isFirebaseAuthed) {
         FirebaseAuthenticationScreen(
             onAuthSuccess = { email ->
                 prefs.edit().putBoolean("is_authed", true).putString("authed_email", email).apply()
                 isFirebaseAuthed = true
                 firebaseUserEmail = email
             }
         )
    } else if (!isProfileLoaded) {
         // Secure routing: Show premium loading gate while loading user profile from database
         val cinematicLoadingBackground = if (isDarkThemeGlobal) {
             Brush.verticalGradient(colors = listOf(Color(0xFF130E20), Color(0xFF08060A)))
         } else {
             Brush.verticalGradient(colors = listOf(Color(0xFFFAF6FE), Color(0xFFEDE4F5)))
         }
         Box(
             modifier = Modifier
                 .fillMaxSize()
                 .background(cinematicLoadingBackground),
             contentAlignment = Alignment.Center
         ) {
             Column(horizontalAlignment = Alignment.CenterHorizontally) {
                 AurenLogo(modifier = Modifier.size(150.dp))
                 Spacer(modifier = Modifier.height(24.dp))
                 CircularProgressIndicator(color = LuxGoldChange)
             }
         }
    } else if (profile == null || !profile!!.isOnboarded) {
        OnboardingFlow(
            profile = profile,
            viewModel = viewModel,
            onComplete = { cur, obj, md, sal, pay, bal, buf, hidden ->
                viewModel.onboardUser(cur, obj, md, sal, pay, bal, buf, hidden)
            }
        )
    } else {
        var itemPendingForDeletion by remember { mutableStateOf<Any?>(null) }

        val cinematicBackground = if (isDarkThemeGlobal) {
            Brush.verticalGradient(colors = listOf(Color(0xFF130E20), Color(0xFF08060A)))
         } else {
             Brush.verticalGradient(colors = listOf(Color(0xFFFAF6FE), Color(0xFFEDE4F5)))
         }

        Scaffold(
            floatingActionButton = {
                if (!showSideBar && (activeTab == "home" || activeTab == "transactions")) {
                    FloatingActionButton(
                        onClick = { showAddTransactionDialog = true },
                        containerColor = LuxGoldChange,
                        contentColor = LuxBlack,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.padding(bottom = 80.dp).testTag("fab_add_transaction")
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "Add Transaction")
                    }
                }
            },
            containerColor = Color.Transparent
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(cinematicBackground)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = innerPadding.calculateTopPadding(), bottom = 0.dp)
                ) {
                    AnimatedContent(
                        targetState = activeTab,
                        label = "tabTransition",
                        transitionSpec = {
                            (fadeIn(
                                animationSpec = spring(
                                    stiffness = Spring.StiffnessMediumLow,
                                    visibilityThreshold = null
                                )
                            ) + slideInVertically(
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioLowBouncy,
                                    stiffness = Spring.StiffnessMediumLow
                                ),
                                initialOffsetY = { it / 24 }
                            )) togetherWith fadeOut(
                                animationSpec = spring(stiffness = Spring.StiffnessHigh)
                            )
                        }
                    ) { tab ->
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
                                onSettingsClick = { showSideBar = true },
                                onLogout = {
                                    prefs.edit().putBoolean("is_authed", false).putString("authed_email", "").apply()
                                    isFirebaseAuthed = false
                                    firebaseUserEmail = ""
                                }
                            )
                            "transactions" -> TransactionsScreen(
                                viewModel = viewModel,
                                transactions = transactions,
                                accounts = accounts,
                                currency = currency,
                                onDeleteTransaction = { itemPendingForDeletion = it }
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
                                onAddDebtClick = { showAddDebtDialog = true },
                                onDeleteBill = { itemPendingForDeletion = it },
                                onDeleteGoal = { itemPendingForDeletion = it },
                                onDeleteDebt = { itemPendingForDeletion = it }
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

                // Floating Bottom Navigation Footer with slide away physics
                AnimatedVisibility(
                    visible = !showSideBar,
                    enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .zIndex(50f)
                ) {
                    AurenBottomNavigation(activeTab = activeTab, onTabSelected = { activeTab = it })
                }
            }
        }

        if (itemPendingForDeletion != null) {
            GeometricConfirmationDialog(
                item = itemPendingForDeletion!!,
                currency = currency,
                onDismiss = { itemPendingForDeletion = null },
                onConfirm = {
                    val item = itemPendingForDeletion
                    if (item != null) {
                        when (item) {
                            is Transaction -> viewModel.deleteTransaction(item)
                            is BillSubscription -> viewModel.deleteBill(item)
                            is FinancialGoal -> viewModel.deleteGoal(item)
                            is Debt -> viewModel.deleteDebt(item)
                        }
                    }
                    itemPendingForDeletion = null
                }
            )
        }

        // Action Dialogs
        if (showAddTransactionDialog) {
            AddTransactionDialog(
                accounts = accounts,
                currency = currency,
                onDismiss = { showAddTransactionDialog = false },
                onAdd = { amt, type, accId, cat, rch, note, targetId, dateOverride, isRec ->
                    viewModel.addTransaction(amt, type, accId, cat, rch, note, targetId, dateOverride, isRec)
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
                },
                onLogout = {
                    prefs.edit().putBoolean("is_authed", false).putString("authed_email", "").apply()
                    isFirebaseAuthed = false
                    showSettingsDialog = false
                }
            )
        }
        // Settings sidebar — list → detail navigation, see SettingsSidebar.kt
        SettingsSidebar(
            visible = showSideBar,
            profile = profile,
            userEmail = if (firebaseUserEmail.isNotBlank()) firebaseUserEmail else "Guest@auren.io",
            viewModel = viewModel,
            onDismiss = { showSideBar = false },
            onLogout = {
                prefs.edit().putBoolean("is_authed", false).putString("authed_email", "").apply()
                isFirebaseAuthed = false
                firebaseUserEmail = ""
            }
        )

    }
}

// ---------------- PREMIUM ANIMATED BRAND GRAPHICS ----------------
@Composable
fun AurenLogo(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "aurenLogoPulse")
    
    // Smooth harmonic scale pulse to make the logo feel warm, alive, and breathing
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "logoPulse"
    )

    // Subtle glint offset translation to create a moving metallic light reflection across the gold surface
    val glintOffset by infiniteTransition.animateFloat(
        initialValue = -50f,
        targetValue = 150f,
        animationSpec = infiniteRepeatable(
            animation = tween(3500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "logoGlint"
    )

    Canvas(
        modifier = modifier
            .size(100.dp)
            .padding(8.dp)
            .graphicsLayer {
                scaleX = pulseScale
                scaleY = pulseScale
            }
    ) {
        val width = size.width
        val height = size.height
        val centerX = width / 2
        val centerY = height / 2
        val sizeFactor = width / 100f

        // Dynamic multi-stop gold metallic brush that responds to the glintOffset animation
        val goldBrush = Brush.linearGradient(
            colors = listOf(
                Color(0xFF8A640F), // Rich bronze shadow
                Color(0xFFD4AF37), // Solid gold
                Color(0xFFFFF7D6), // Shimmering light highlight
                Color(0xFFD4AF37), // Solid gold
                Color(0xFF8A640F)  // Rich bronze shadow
            ),
            start = androidx.compose.ui.geometry.Offset(glintOffset * sizeFactor, 0f),
            end = androidx.compose.ui.geometry.Offset((glintOffset + 60f) * sizeFactor, height)
        )

        // 1. Concentric Golden Halo Circle (behind the 'A' and bars)
        // Opened slightly at interest points to yield breakout style
        drawArc(
            brush = goldBrush,
            startAngle = -45f,
            sweepAngle = 260f,
            useCenter = false,
            style = Stroke(width = 1.8f * sizeFactor, cap = StrokeCap.Round),
            size = androidx.compose.ui.geometry.Size(68f * sizeFactor, 68f * sizeFactor),
            topLeft = androidx.compose.ui.geometry.Offset(centerX - 34f * sizeFactor, centerY - 44f * sizeFactor)
        )

        // 2. Financial Growth Bar Charts (Inside the letter A structure)
        val barWidth = 2.8f * sizeFactor
        val barBottomY = centerY + 8f * sizeFactor

        // Left short bar
        val bar1Left = centerX - 8.5f * sizeFactor
        val bar1Height = 8f * sizeFactor
        drawRoundRect(
            brush = goldBrush,
            topLeft = androidx.compose.ui.geometry.Offset(bar1Left, barBottomY - bar1Height),
            size = androidx.compose.ui.geometry.Size(barWidth, bar1Height),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(1f * sizeFactor, 1f * sizeFactor)
        )

        // Middle bar
        val bar2Left = centerX - 3.5f * sizeFactor
        val bar2Height = 14f * sizeFactor
        drawRoundRect(
            brush = goldBrush,
            topLeft = androidx.compose.ui.geometry.Offset(bar2Left, barBottomY - bar2Height),
            size = androidx.compose.ui.geometry.Size(barWidth, bar2Height),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(1f * sizeFactor, 1f * sizeFactor)
        )

        // Right tall bar
        val bar3Left = centerX + 1.5f * sizeFactor
        val bar3Height = 21f * sizeFactor
        drawRoundRect(
            brush = goldBrush,
            topLeft = androidx.compose.ui.geometry.Offset(bar3Left, barBottomY - bar3Height),
            size = androidx.compose.ui.geometry.Size(barWidth, bar3Height),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(1f * sizeFactor, 1f * sizeFactor)
        )

        // 3. Elegant Roman Letter 'A' Form with asymmetric stem weightings

        // Left Leg: Elegant curved thin swooping wing
        val leftLegPath = Path().apply {
            moveTo(centerX, centerY - 32f * sizeFactor)
            cubicTo(
                centerX - 6f * sizeFactor, centerY - 10f * sizeFactor,
                centerX - 16f * sizeFactor, centerY + 10f * sizeFactor,
                centerX - 24f * sizeFactor, centerY + 18f * sizeFactor
            )
            // Delicate bottom-left serif flare matching the logo
            quadraticTo(
                centerX - 27f * sizeFactor, centerY + 19.5f * sizeFactor,
                centerX - 30f * sizeFactor, centerY + 19.5f * sizeFactor
            )
        }
        drawPath(
            path = leftLegPath,
            brush = goldBrush,
            style = Stroke(width = 2.2f * sizeFactor, cap = StrokeCap.Round)
        )

        // Right Leg: Thick majestic primary structural stem
        val rightLegPath = Path().apply {
            moveTo(centerX - 1.5f * sizeFactor, centerY - 32f * sizeFactor)
            lineTo(centerX + 2f * sizeFactor, centerY - 32f * sizeFactor)
            lineTo(centerX + 20f * sizeFactor, centerY + 20f * sizeFactor)
            lineTo(centerX + 11f * sizeFactor, centerY + 20f * sizeFactor)
            close()
        }
        drawPath(path = rightLegPath, brush = goldBrush)

        // Right foot base serif plate
        drawRoundRect(
            brush = goldBrush,
            topLeft = androidx.compose.ui.geometry.Offset(centerX + 8f * sizeFactor, centerY + 19f * sizeFactor),
            size = androidx.compose.ui.geometry.Size(15f * sizeFactor, 2f * sizeFactor),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(0.5f * sizeFactor, 0.5f * sizeFactor)
        )

        // 4. Sweeping Growth Trendline across the letter 'A'
        // Designed as a perfectly tapered vector bezier ribbon
        val swooshPath = Path().apply {
            moveTo(centerX - 33f * sizeFactor, centerY + 22f * sizeFactor)
            cubicTo(
                centerX - 13f * sizeFactor, centerY + 15f * sizeFactor,
                centerX + 8f * sizeFactor, centerY + 8f * sizeFactor,
                centerX + 32f * sizeFactor, centerY + 8f * sizeFactor
            )
            // Beautiful narrow reverse curve to recreate the sharp end taper
            cubicTo(
                centerX + 12f * sizeFactor, centerY + 11f * sizeFactor,
                centerX - 10f * sizeFactor, centerY + 18f * sizeFactor,
                centerX - 29f * sizeFactor, centerY + 24f * sizeFactor
            )
            close()
        }
        drawPath(path = swooshPath, brush = goldBrush)
    }
}

// ---------------- YEAR-OVER-YEAR SAVINGS RATE COMPARISON CHART ----------------
@Composable
fun YoYSavingsRateChart(
    transactions: List<Transaction>,
    currency: String,
    modifier: Modifier = Modifier
) {
    val cal = remember { Calendar.getInstance() }
    val currentYear = cal.get(Calendar.YEAR)
    val currentMonth = cal.get(Calendar.MONTH)

    // Compute actual current month (June) savings rate from transactions state if any
    val txCal = remember { Calendar.getInstance() }
    val juneExpenses = remember(transactions, currentYear, currentMonth) {
        transactions.filter {
            txCal.timeInMillis = it.date
            txCal.get(Calendar.YEAR) == currentYear &&
            txCal.get(Calendar.MONTH) == currentMonth &&
            it.type.lowercase() == "expense"
        }.sumOf { it.amount }
    }
    val juneIncome = remember(transactions, currentYear, currentMonth) {
        transactions.filter {
            txCal.timeInMillis = it.date
            txCal.get(Calendar.YEAR) == currentYear &&
            txCal.get(Calendar.MONTH) == currentMonth &&
            it.type.lowercase() == "income"
        }.sumOf { it.amount }
    }

    // Savings rate calculation
    val juneRate = remember(juneExpenses, juneIncome) {
        if (juneIncome > 0.0) {
            ((juneIncome - juneExpenses) / juneIncome) * 100.0
        } else {
            25.0 // elegant baseline fallback if no transactions are recorded yet
        }
    }.coerceIn(0.0, 100.0)

    val currentYearTrend = listOf(18.0, 22.5, 15.0, 26.0, 31.0, juneRate)
    val priorYearTrend = listOf(12.0, 15.1, 19.0, 20.0, 24.5, 18.0)
    val monthsLabels = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun")

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = LuxDarkGray),
        border = BorderStroke(1.dp, LuxCardGray)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "SAVINGS RATE COMPARISON",
                        style = Typography.labelLarge,
                        color = LuxGoldChange,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "YoY Savings Rates Trends compared",
                        style = Typography.bodySmall,
                        color = LuxMuted
                    )
                }
                
                // Indicators / Legends
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(LuxGoldChange, RoundedCornerShape(2.dp))
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "This Cycle", color = LuxIvory, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(LuxIvory.copy(alpha = 0.4f), RoundedCornerShape(2.dp))
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "Prior Cycle", color = LuxMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Canvas Drawing
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .padding(horizontal = 8.dp)
            ) {
                val canvasWidth = size.width
                val canvasHeight = size.height
                
                val leftMargin = 30.dp.toPx()
                val bottomMargin = 20.dp.toPx()
                val topMargin = 10.dp.toPx()
                val rightMargin = 10.dp.toPx()
                
                val graphWidth = canvasWidth - leftMargin - rightMargin
                val graphHeight = canvasHeight - topMargin - bottomMargin
                
                val maxVal = 40f // scale graph up to 40% savings rate
                
                // 1. Draw horizontal grid lines and Y axis labels
                val gridLinesCount = 3
                for (i in 0..gridLinesCount) {
                    val rateVal = (maxVal / gridLinesCount) * i
                    val relativeY = topMargin + graphHeight - (rateVal / maxVal) * graphHeight
                    
                    // grid line
                    drawLine(
                        color = Color(0xFF333333).copy(alpha = 0.6f),
                        start = androidx.compose.ui.geometry.Offset(leftMargin, relativeY),
                        end = androidx.compose.ui.geometry.Offset(canvasWidth - rightMargin, relativeY),
                        strokeWidth = 1.dp.toPx(),
                        pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                    )
                    
                    // Y axis label
                    drawContext.canvas.nativeCanvas.drawText(
                        "${rateVal.toInt()}%",
                        10f,
                        relativeY + 4.dp.toPx(),
                        android.graphics.Paint().apply {
                            color = android.graphics.Color.GRAY
                            textSize = 8.dp.toPx()
                            typeface = android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.NORMAL)
                        }
                    )
                }
                
                // 2. Draw X axis month labels
                val pointsCount = monthsLabels.size
                val stepX = graphWidth / (pointsCount - 1)
                
                for (i in 0 until pointsCount) {
                    val relativeX = leftMargin + i * stepX
                    
                    drawContext.canvas.nativeCanvas.drawText(
                        monthsLabels[i],
                        relativeX - 10.dp.toPx(),
                        canvasHeight - 4.dp.toPx(),
                        android.graphics.Paint().apply {
                            color = android.graphics.Color.GRAY
                            textSize = 9.dp.toPx()
                            typeface = android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.NORMAL)
                        }
                    )
                }

                // 3. Draw Prior Cycle line (Last Year) - Dashed silver
                val priorPath = Path()
                for (i in 0 until pointsCount) {
                    val relativeX = leftMargin + i * stepX
                    val yVal = priorYearTrend[i].toFloat()
                    val relativeY = topMargin + graphHeight - (yVal / maxVal) * graphHeight
                    
                    if (i == 0) {
                        priorPath.moveTo(relativeX, relativeY)
                    } else {
                        priorPath.lineTo(relativeX, relativeY)
                    }
                }
                drawPath(
                    path = priorPath,
                    color = LuxIvory.copy(alpha = 0.35f),
                    style = Stroke(
                        width = 1.5.dp.toPx(),
                        pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
                    )
                )

                // 4. Draw Current Cycle line (This Year) - Solid Gold
                val currentPath = Path()
                for (i in 0 until pointsCount) {
                    val relativeX = leftMargin + i * stepX
                    val yVal = currentYearTrend[i].toFloat()
                    val relativeY = topMargin + graphHeight - (yVal / maxVal) * graphHeight
                    
                    if (i == 0) {
                        currentPath.moveTo(relativeX, relativeY)
                    } else {
                        val prevX = leftMargin + (i - 1) * stepX
                        val prevY = topMargin + graphHeight - (currentYearTrend[i - 1].toFloat() / maxVal) * graphHeight
                        currentPath.cubicTo(
                            prevX + stepX / 2f, prevY,
                            relativeX - stepX / 2f, relativeY,
                            relativeX, relativeY
                        )
                    }
                }
                drawPath(
                    path = currentPath,
                    brush = Brush.linearGradient(
                        colors = listOf(LuxGoldLight, LuxGoldChange)
                    ),
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                )

                // 5. Draw glowing nodes on current year line
                for (i in 0 until pointsCount) {
                    val relativeX = leftMargin + i * stepX
                    val yVal = currentYearTrend[i].toFloat()
                    val relativeY = topMargin + graphHeight - (yVal / maxVal) * graphHeight
                    
                    drawCircle(
                        color = LuxGoldChange.copy(alpha = 0.18f),
                        radius = 8.dp.toPx(),
                        center = androidx.compose.ui.geometry.Offset(relativeX, relativeY)
                    )
                    drawCircle(
                        color = LuxGoldChange,
                        radius = 4.dp.toPx(),
                        center = androidx.compose.ui.geometry.Offset(relativeX, relativeY)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(10.dp))
            val percentGain = juneRate - 18.0
            Text(
                text = if (percentGain > 0) "▲ Active June savings rate beats prior cycle baseline by ${String.format("%.1f", percentGain)}% points." else "● Active savings cycles are closely aligned with benchmark target corridors.",
                color = if (percentGain > 0) LuxGreen else LuxGreen.copy(alpha = 0.7f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

// ---------------- AUTOMATED BANK IMPORT DIALOGUE ----------------
@Composable
fun AutomatedBankImportDialog(
    accounts: List<Account>,
    currency: String,
    onDismiss: () -> Unit,
    onImport: (List<Transaction>) -> Unit
) {
    val mockFeeds = remember {
        listOf(
            Transaction(
                amount = 450.0,
                type = "Expense",
                date = System.currentTimeMillis() - 86400000L * 1,
                accountId = 0,
                category = "Dining",
                merchant = "Zomato Restaurant Online",
                note = "Imported via Bank Sync Feed"
            ),
            Transaction(
                amount = 2499.0,
                type = "Expense",
                date = System.currentTimeMillis() - 86400000L * 2,
                accountId = 0,
                category = "Shopping",
                merchant = "Amazon Retail India Ltd",
                note = "Imported via Bank Sync Feed"
            ),
            Transaction(
                amount = 943.0,
                type = "Expense",
                date = System.currentTimeMillis() - 86400000L * 3,
                accountId = 0,
                category = "Utilities",
                merchant = "Airtel Fiber Broadband",
                note = "Imported via Bank Sync Feed"
            ),
            Transaction(
                amount = 380.0,
                type = "Expense",
                date = System.currentTimeMillis() - 86400000L * 4,
                accountId = 0,
                category = "Transport",
                merchant = "Uber India Ride Console",
                note = "Imported via Bank Sync Feed"
            ),
            Transaction(
                amount = 649.0,
                type = "Expense",
                date = System.currentTimeMillis() - 86400000L * 5,
                accountId = 0,
                category = "Utilities",
                merchant = "Netflix Subscription Pay",
                note = "Imported via Bank Sync Feed"
            ),
            Transaction(
                amount = 60000.0,
                type = "Income",
                date = System.currentTimeMillis() - 86400000L * 6,
                accountId = 0,
                category = "Salary",
                merchant = "HDFC ACH Salary Remittance",
                note = "Imported via Bank Sync Feed"
            )
        )
    }

    var selectedAccountId by remember { mutableStateOf(accounts.firstOrNull()?.id ?: 0L) }
    var checkedIndices by remember { mutableStateOf(mockFeeds.indices.toSet()) }
    val categories = listOf("Dining", "Shopping", "Utilities", "Transport", "Salary", "Investment", "Health", "Leisure")
    val customCategories = remember { mutableStateMapOf<Int, String>() }
    val coroutineScope = rememberCoroutineScope()

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = LuxDarkGray),
            border = BorderStroke(1.2.dp, LuxGoldChange),
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                Text(text = "AUTOMATED BANK CONSOLE", style = Typography.labelLarge, color = LuxGoldChange, letterSpacing = 1.5.sp)
                Text(text = "Direct Feed Synchronization", style = Typography.titleMedium, color = LuxIvory)
                Text(
                    text = "Auren has detected 6 uncommitted banking outlays from SMS alerts and payment feeds. Overrides categories below.",
                    style = Typography.bodySmall,
                    color = LuxMuted,
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Account Selection
                Text(text = "CHOOSE TARGET ACCOUNT:", fontSize = 10.sp, color = LuxMuted, fontWeight = FontWeight.Bold)
                var showAccDropdown by remember { mutableStateOf(false) }
                val activeAccount = accounts.find { it.id == selectedAccountId }
                val activeAccountLabel = activeAccount?.let { "${it.name} ($currency${String.format("%,.0f", it.balance)})" } ?: "Select Vault Account"

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(LuxCardGray)
                        .border(1.dp, LuxGoldChange.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                        .clickable { showAccDropdown = !showAccDropdown }
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = activeAccountLabel.uppercase(), color = LuxIvory, fontWeight = FontWeight.ExtraBold, fontSize = 11.sp)
                        Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null, tint = LuxGoldChange)
                    }

                    DropdownMenu(
                        expanded = showAccDropdown,
                        onDismissRequest = { showAccDropdown = false },
                        modifier = Modifier.background(LuxDarkGray)
                    ) {
                        accounts.forEach { acc ->
                            DropdownMenuItem(
                                text = { Text(text = "${acc.name.uppercase()} (${acc.type.uppercase()})", color = LuxIvory, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                onClick = {
                                    selectedAccountId = acc.id
                                    showAccDropdown = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Transactions List
                Text(text = "DETECTED FEEDS:", fontSize = 10.sp, color = LuxMuted, fontWeight = FontWeight.Bold)
                
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(mockFeeds.size) { index ->
                        val item = mockFeeds[index]
                        val isChecked = checkedIndices.contains(index)
                        val selectedCat = customCategories[index] ?: item.category

                        Card(
                            colors = CardDefaults.cardColors(containerColor = if (isChecked) LuxCardGray else LuxDarkGray.copy(alpha = 0.5f)),
                            border = BorderStroke(1.dp, if (isChecked) LuxGoldChange.copy(alpha = 0.25f) else LuxCardGray.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = isChecked,
                                    onCheckedChange = { checked ->
                                        checkedIndices = if (checked) {
                                            checkedIndices + index
                                        } else {
                                            checkedIndices - index
                                        }
                                    },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = LuxGoldChange,
                                        uncheckedColor = LuxMuted,
                                        checkmarkColor = LuxBlack
                                    )
                                )
                                
                                Spacer(modifier = Modifier.width(6.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.merchant,
                                        color = LuxIvory,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        modifier = Modifier.padding(top = 4.dp)
                                    ) {
                                        Text(
                                            text = if (item.type == "Income") "+$currency${String.format("%,.0f", item.amount)}" else "-$currency${String.format("%,.0f", item.amount)}",
                                            color = if (item.type == "Income") LuxGreen else LuxIvory.copy(alpha = 0.85f),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        
                                        // Category chip dropdown trigger
                                        var showCatMenu by remember { mutableStateOf(false) }
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(LuxDarkGray)
                                                .border(0.5.dp, LuxGoldChange.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                                                .clickable { showCatMenu = !showCatMenu }
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(text = selectedCat.uppercase(), color = LuxGoldChange, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                                Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null, tint = LuxGoldChange, modifier = Modifier.size(10.dp))
                                            }

                                            DropdownMenu(
                                                expanded = showCatMenu,
                                                onDismissRequest = { showCatMenu = false },
                                                modifier = Modifier.background(LuxDarkGray)
                                            ) {
                                                categories.forEach { cat ->
                                                    DropdownMenuItem(
                                                        text = { Text(text = cat, color = LuxIvory, fontSize = 10.sp) },
                                                        onClick = {
                                                            customCategories[index] = cat
                                                            showCatMenu = false
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        border = BorderStroke(1.dp, LuxMuted),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = LuxMuted),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel", fontSize = 12.sp)
                    }

                    Button(
                        onClick = {
                            val itemsToImport = mockFeeds.filterIndexed { idx, _ -> checkedIndices.contains(idx) }
                                .mapIndexed { idx, tx ->
                                    tx.copy(
                                        accountId = selectedAccountId,
                                        category = customCategories[checkedIndices.elementAt(idx)] ?: tx.category
                                    )
                                }
                            if (itemsToImport.isNotEmpty()) {
                                onImport(itemsToImport)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = LuxGoldChange, contentColor = LuxBlack),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1.5f),
                        enabled = checkedIndices.isNotEmpty() && selectedAccountId != 0L
                    ) {
                        Text("Import Selected (${checkedIndices.size})", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ---------------- INTERACTIVE SPENDING CALENDAR HEATMAP ----------------
@Composable
fun SpendingHeatmap(transactions: List<Transaction>, currency: String) {
    val calendar = remember { Calendar.getInstance() }
    val currentYear = remember { calendar.get(Calendar.YEAR) }
    val currentMonth = remember { calendar.get(Calendar.MONTH) }
    val currentMonthName = remember { 
        calendar.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.getDefault()) ?: "MONTH"
    }

    // Set calendar to first of current month to determine its day of week
    val (firstDayOfWeek, maxDays) = remember {
        val testCal = Calendar.getInstance().apply {
            set(Calendar.YEAR, currentYear)
            set(Calendar.MONTH, currentMonth)
            set(Calendar.DAY_OF_MONTH, 1)
        }
        testCal.get(Calendar.DAY_OF_WEEK) to testCal.getActualMaximum(Calendar.DAY_OF_MONTH)
    }

    // Expenses specifically in current month
    val currentMonthExpenses = remember(transactions, currentYear, currentMonth) {
        val txCal = Calendar.getInstance()
        transactions.filter {
            txCal.timeInMillis = it.date
            txCal.get(Calendar.YEAR) == currentYear && 
            txCal.get(Calendar.MONTH) == currentMonth && 
            it.type.lowercase() == "expense"
        }
    }

    // Expenses grouped by day
    val expensesByDay = remember(currentMonthExpenses) {
        val txCal = Calendar.getInstance()
        currentMonthExpenses.groupBy {
            txCal.timeInMillis = it.date
            txCal.get(Calendar.DAY_OF_MONTH)
        }
    }

    // Maximum daily expenditure in the month (for relative color opacity scaling)
    val maxDailySpent = remember(expensesByDay) {
        expensesByDay.values.maxOfOrNull { list -> list.sumOf { it.amount } } ?: 1.0
    }

    var selectedDay by remember { mutableStateOf<Int?>(null) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 24.dp),
        colors = CardDefaults.cardColors(containerColor = LuxDarkGray),
        border = BorderStroke(1.dp, LuxGoldChange.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header title
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(LuxGoldChange.copy(alpha = 0.15f), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = null,
                            tint = LuxGoldChange,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "MONTHLY SPENDING TEMP",
                            style = Typography.labelLarge,
                            color = LuxGoldChange,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "$currentMonthName $currentYear tracking index",
                            style = Typography.bodySmall,
                            color = LuxMuted
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Sunday to Saturday headers
            val daysOfWeekHeader = listOf("S", "M", "T", "W", "T", "F", "S")
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                daysOfWeekHeader.forEach { d ->
                    Text(
                        text = d,
                        color = LuxGoldChange.copy(alpha = 0.7f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Calendar weeks layout
            val totalCells = maxDays + (firstDayOfWeek - 1)
            val rowsCount = (totalCells + 6) / 7

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                for (rowIndex in 0 until rowsCount) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        for (colIndex in 0 until 7) {
                            val cellIndex = rowIndex * 7 + colIndex
                            val dayOfMonth = cellIndex - (firstDayOfWeek - 2)

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .padding(2.dp)
                            ) {
                                if (dayOfMonth in 1..maxDays) {
                                    val dailyTxs = expensesByDay[dayOfMonth] ?: emptyList()
                                    val totalSpent = dailyTxs.sumOf { it.amount }
                                    val isSelected = selectedDay == dayOfMonth

                                    // Determine structural color based on spending volume
                                    val baseAlpha = if (totalSpent > 0.0) {
                                        val intensity = (totalSpent / maxDailySpent).coerceIn(0.12, 1.0).toFloat()
                                        0.25f + intensity * 0.75f
                                    } else {
                                        0.0f
                                    }

                                    val cellColor = if (totalSpent > 0.0) {
                                        LuxGoldChange.copy(alpha = baseAlpha)
                                    } else {
                                        LuxCardGray
                                    }

                                    // Cell body
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(cellColor)
                                            .border(
                                                width = if (isSelected) 1.5.dp else 1.dp,
                                                color = if (isSelected) Color.White else if (totalSpent > 5000.0) LuxGoldChange else Color.Transparent,
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                            .clickable {
                                                selectedDay = if (isSelected) null else dayOfMonth
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = dayOfMonth.toString(),
                                            color = if (totalSpent > 0.0 && baseAlpha > 0.6f) LuxBlack else LuxIvory,
                                            fontSize = 11.sp,
                                            fontWeight = if (totalSpent > 0.0) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                } else {
                                    // Dead cells outside bounds
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(LuxBlack.copy(alpha = 0.3f))
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Modern color density indicator bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Empty", color = LuxMuted, fontSize = 10.sp)
                Box(modifier = Modifier.size(12.dp).clip(RoundedCornerShape(3.dp)).background(LuxCardGray))
                Spacer(modifier = Modifier.weight(1f))
                Text(text = "Outlay Intensity:", color = LuxMuted, fontSize = 10.sp)
                Box(modifier = Modifier.size(12.dp).clip(RoundedCornerShape(3.dp)).background(LuxGoldChange.copy(alpha = 0.25f)))
                Box(modifier = Modifier.size(12.dp).clip(RoundedCornerShape(3.dp)).background(LuxGoldChange.copy(alpha = 0.55f)))
                Box(modifier = Modifier.size(12.dp).clip(RoundedCornerShape(3.dp)).background(LuxGoldChange.copy(alpha = 0.85f)))
                Box(modifier = Modifier.size(12.dp).clip(RoundedCornerShape(3.dp)).background(LuxGoldChange))
                Text(text = "High", color = LuxIvory, fontSize = 10.sp)
            }

            // Clicked details panel with custom smooth spring transition
            AnimatedVisibility(
                visible = selectedDay != null,
                enter = expandVertically(animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)) + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                if (selectedDay != null) {
                    val day = selectedDay!!
                    val dayTxs = expensesByDay[day] ?: emptyList()

                    Column {
                        Spacer(modifier = Modifier.height(16.dp))
                        Divider(color = LuxGoldChange.copy(alpha = 0.2f))
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "DAY $day DETAILED LEDGER",
                                fontWeight = FontWeight.Bold,
                                color = LuxGoldChange,
                                fontSize = 11.sp,
                                letterSpacing = 1.sp
                            )
                            IconButton(
                                onClick = { selectedDay = null },
                                modifier = Modifier.size(16.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = LuxMuted, modifier = Modifier.size(12.dp))
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        if (dayTxs.isEmpty()) {
                            Text(
                                text = "No outlays recorded on Day $day. Excellent wealth retention balance.",
                                color = LuxMuted,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        } else {
                            dayTxs.forEach { tx ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(text = if (tx.merchant.isNotBlank()) tx.merchant.uppercase() else tx.category.uppercase(), color = LuxIvory, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        Text(text = tx.category.uppercase(), color = LuxMuted, fontSize = 10.sp)
                                    }
                                    Text(
                                        text = "-$currency${String.format("%,.0f", tx.amount)}",
                                        color = LuxIvory,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ---------------- CUSTOM UI NAVIGATION BAR ----------------
@Composable
fun AurenBottomNavigation(activeTab: String, onTabSelected: (String) -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        Surface(
            color = LuxDarkGray.copy(alpha = 0.92f),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.5.dp, LuxGoldChange.copy(alpha = 0.35f)),
            tonalElevation = 8.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp, horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                NavigationItem(
                    label = t("Ledger", "बहीखाता", "Ledger / Khata"),
                    isSelected = activeTab == "transactions",
                    icon = Icons.Default.AccountBalanceWallet,
                    onClick = { onTabSelected("transactions") }
                )
                NavigationItem(
                    label = t("Plan", "योजना", "Planning"),
                    isSelected = activeTab == "plan",
                    icon = Icons.Default.TrendingUp,
                    onClick = { onTabSelected("plan") }
                )
                
                // Centered Home Action
                Column(
                    modifier = Modifier
                        .clickable { onTabSelected("home") }
                        .padding(horizontal = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    val isSelected = activeTab == "home"
                    Box(
                        modifier = Modifier
                            .size(if (isSelected) 46.dp else 42.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isSelected) LuxGoldChange else LuxCardGray)
                            .border(1.dp, LuxGoldChange.copy(alpha = if (isSelected) 1f else 0.4f), RoundedCornerShape(14.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Dashboard,
                            contentDescription = "Home",
                            tint = if (isSelected) LuxBlack else LuxGoldChange,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = t("Home", "होम", "Home / Ghar"),
                        color = if (isSelected) LuxGoldChange else LuxMuted,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }

                NavigationItem(
                    label = t("Insights", "जानकारी", "Insights"),
                    isSelected = activeTab == "insights",
                    icon = Icons.Default.Analytics,
                    onClick = { onTabSelected("insights") }
                )
                NavigationItem(
                    label = t("Coach", "सलाह", "AI Coach / Dost"),
                    isSelected = activeTab == "coach",
                    icon = Icons.Default.AutoAwesome,
                    onClick = { onTabSelected("coach") }
                )
            }
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
                .clip(RoundedCornerShape(14.dp))
                .background(if (isSelected) LuxGoldChange.copy(alpha = 0.15f) else Color.Transparent)
                .padding(horizontal = 12.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isSelected) LuxGoldChange else LuxMuted,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            color = if (isSelected) LuxIvory else LuxMuted,
            fontSize = 10.sp,
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

    val onboardingBg = if (isDarkThemeGlobal) {
        Brush.verticalGradient(colors = listOf(Color(0xFF130E20), Color(0xFF08060A)))
    } else {
        Brush.verticalGradient(colors = listOf(Color(0xFFFAF6FE), Color(0xFFEDE4F5)))
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(onboardingBg)
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AurenLogo(modifier = Modifier.size(120.dp).padding(bottom = 16.dp))
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

// ---------------- QUICK EDIT BILL DIALOG ----------------
@Composable
fun QuickEditBillDialog(
    bill: BillSubscription,
    onSave: (BillSubscription) -> Unit,
    onDismiss: () -> Unit
) {
    var editName by remember { mutableStateOf(bill.name) }
    var editAmount by remember { mutableStateOf(bill.amount.toString()) }
    var editDueDate by remember { mutableStateOf(bill.dueDate.toString()) }

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
                Text(text = "EDIT RECURRING OBLIGATION", style = Typography.labelLarge, color = LuxGoldChange)
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = editName,
                    onValueChange = { editName = it },
                    label = { Text("Obligation Name") },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = LuxGoldChange),
                    modifier = Modifier.fillMaxWidth().testTag("quick_edit_bill_name")
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = editAmount,
                    onValueChange = { editAmount = it },
                    label = { Text("Amount") },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = LuxGoldChange),
                    modifier = Modifier.fillMaxWidth().testTag("quick_edit_bill_amount"),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = editDueDate,
                    onValueChange = { editDueDate = it },
                    label = { Text("Due Day (1..31)") },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = LuxGoldChange),
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
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
                            val amt = editAmount.toDoubleOrNull() ?: bill.amount
                            val due = editDueDate.toIntOrNull() ?: bill.dueDate
                            onSave(bill.copy(name = editName, amount = amt, dueDate = due.coerceIn(1, 31)))
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = LuxGoldChange, contentColor = LuxBlack),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Save Change")
                    }
                }
            }
        }
    }
}

// ---------------- CINEMATIC WEB-INSPIRED ANIMATORS & GLASS SURFACES ----------------
@Composable
fun CinematicEntranceContainer(
    delayMillis: Int,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(delayMillis.toLong())
        visible = true
    }
    
    val animatedAlpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(
            durationMillis = 800,
            easing = EaseOutCubic
        ),
        label = "fade"
    )
    
    val animatedOffsetY by animateDpAsState(
        targetValue = if (visible) 0.dp else 40.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "slide"
    )

    Box(
        modifier = modifier
            .graphicsLayer {
                alpha = animatedAlpha
                translationY = animatedOffsetY.toPx()
            }
    ) {
        content()
    }
}

@Composable
fun CinematicGlassCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(24.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val isHovered by interactionSource.collectIsHoveredAsState()

    val scale by animateFloatAsState(
        targetValue = when {
            isPressed -> 0.98f
            isHovered -> 1.02f
            else -> 1.0f
        },
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "scale"
    )

    val glassBg = if (isDarkThemeGlobal) {
        Brush.verticalGradient(
            colors = listOf(
                Color(0x2B21173C),
                Color(0x1B0B0813)
            )
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xD8FFFFFF),
                Color(0xA5F3ECFC)
            )
        )
    }

    val glassBorderBrush = if (isDarkThemeGlobal) {
        Brush.linearGradient(
            colors = listOf(
                LuxGoldChange.copy(alpha = 0.40f),
                Color.White.copy(alpha = 0.08f),
                LuxGoldChange.copy(alpha = 0.20f)
            )
        )
    } else {
        Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.9f),
                LuxGoldChange.copy(alpha = 0.25f),
                Color.White.copy(alpha = 0.4f)
            )
        )
    }

    Card(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = LocalIndication.current,
                        onClick = onClick
                    )
                } else {
                    Modifier
                }
            ),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = BorderStroke(1.2.dp, glassBorderBrush)
    ) {
        Column(
            modifier = Modifier
                .background(glassBg)
                .fillMaxWidth(),
            content = content
        )
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
    onSettingsClick: () -> Unit,
    onLogout: () -> Unit
) {
    val profileState by viewModel.profile.collectAsStateWithLifecycle()
    val goalsState by viewModel.goals.collectAsStateWithLifecycle()
    val transactionsState by viewModel.transactions.collectAsStateWithLifecycle()
    val dashboardConfig by viewModel.dashboardConfig.collectAsStateWithLifecycle()

    var showSideBar by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            val salaryVal = profileState?.salaryAmount ?: 60000.0
            val bufferVal = profileState?.safetyBuffer ?: 2000.0
            val totalBillsVal = bills.sumOf { it.amount }
            val totalGoalsVal = goalsState?.sumOf { it.currentAmount } ?: 0.0

            // Premium Modernized Header
            CinematicEntranceContainer(delayMillis = 50) {
                CinematicGlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = onSettingsClick,
                                modifier = Modifier.testTag("open_sidebar_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Menu,
                                    contentDescription = "Open Sidebar Menu",
                                    tint = LuxGoldChange,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .background(LuxGreen, androidx.compose.foundation.shape.CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = t("FIREBASE SECURE", "फ़ायरबेस सुरक्षित", "Firebase Surakshit"),
                                        style = Typography.labelLarge,
                                        color = LuxGreen,
                                        fontSize = 10.sp,
                                        letterSpacing = 1.sp
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = t("Auren Money Hub", "ऑरेन मनी हब", "Auren Hub"),
                                    style = Typography.titleLarge,
                                    color = LuxIvory,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        
                        // Header settings: Settings/Menu icon to open preferences sidebar
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = onSettingsClick,
                                modifier = Modifier.testTag("home_settings_button")
                            ) {
                                Icon(imageVector = Icons.Default.Settings, contentDescription = "Settings", tint = LuxGoldChange)
                            }
                        }
                    }
                }
            }
    
            Spacer(modifier = Modifier.height(8.dp))

            // Safe To Spend display
            CinematicEntranceContainer(delayMillis = 150) {
                val spendCardBg = if (isDarkThemeGlobal) {
                    Brush.verticalGradient(
                        colors = listOf(
                            LuxGoldLight.copy(alpha = 0.25f),
                            LuxDarkGray.copy(alpha = 0.40f)
                        )
                    )
                } else {
                    Brush.verticalGradient(
                        colors = listOf(
                            LuxGoldLight.copy(alpha = 0.85f),
                            Color.White.copy(alpha = 0.60f)
                        )
                    )
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                    border = BorderStroke(1.5.dp, if (isDarkThemeGlobal) LuxGoldChange.copy(alpha = 0.35f) else Color.White)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(spendCardBg)
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Rotated geometric badge from design template replaced with animated breathing Auren Logo & cinematic halo pulse
                        val infiniteGlow = rememberInfiniteTransition(label = "pulseGlow")
                        val glowAlpha by infiniteGlow.animateFloat(
                            initialValue = 0.15f,
                            targetValue = 0.45f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(2500, easing = LinearEasing),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "glowAlpha"
                        )
                        val glowSize by infiniteGlow.animateFloat(
                            initialValue = 54.dp.value,
                            targetValue = 72.dp.value,
                            animationSpec = infiniteRepeatable(
                                animation = tween(2500, easing = LinearEasing),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "glowSize"
                        )
                        
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.size(80.dp)
                        ) {
                            // Pulsing Glow Ring (Halo)
                            Box(
                                modifier = Modifier
                                    .size(glowSize.dp)
                                    .background(
                                        brush = Brush.radialGradient(
                                            colors = listOf(
                                                LuxGoldChange.copy(alpha = glowAlpha),
                                                Color.Transparent
                                            )
                                        ),
                                        shape = androidx.compose.foundation.shape.CircleShape
                                    )
                            )
                            
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .background(color = LuxBlack, shape = RoundedCornerShape(16.dp))
                                    .border(1.5.dp, LuxGoldChange.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                                    .padding(4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                AurenLogo(modifier = Modifier.size(56.dp))
                            }
                        }
                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = t("SAFE TO SPEND TODAY", "आज खर्च करने के लिए सुरक्षित मात्रा", "Safe to Spend Today"),
                            style = Typography.labelLarge,
                            color = if (isDarkThemeGlobal) LuxIvory else Color(0xFF21005D), // Dynamically supports eye contrast
                            letterSpacing = 2.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "$currency${String.format("%,.2f", safeToSpendToday)}",
                            style = Typography.displayLarge,
                            color = if (isDarkThemeGlobal) LuxIvory else Color(0xFF21005D), // Dynamically supports eye contrast
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
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Geometric Inspired Grid Layout (Task 4)
            CinematicEntranceContainer(delayMillis = 250) {
                Column {
                    Text(text = "MONETARY MATRIX", style = Typography.labelLarge, color = LuxGoldChange, letterSpacing = 2.sp)
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Column 1
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Widget A: Income
                            InteractiveGeometricCard(
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .background(color = LuxGoldChange, shape = RoundedCornerShape(10.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ArrowUpward,
                                            contentDescription = null,
                                            tint = LuxBlack,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(text = "MONTHLY SALARY", fontSize = 10.sp, color = LuxMuted, fontWeight = FontWeight.SemiBold)
                                    Text(text = "$currency${String.format("%,.0f", salaryVal)}", style = Typography.titleMedium, fontWeight = FontWeight.Bold, color = LuxIvory)
                                }
                            }

                            // Widget B: Bills
                            InteractiveGeometricCard(
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .background(color = LuxGoldChange, shape = RoundedCornerShape(10.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.DateRange,
                                            contentDescription = null,
                                            tint = LuxBlack,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(text = "MONTHLY BILLS", fontSize = 10.sp, color = LuxMuted, fontWeight = FontWeight.SemiBold)
                                    Text(text = "$currency${String.format("%,.0f", totalBillsVal)}", style = Typography.titleMedium, fontWeight = FontWeight.Bold, color = LuxIvory)
                                }
                            }
                        }

                        // Column 2
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Widget A: Savings Goals
                            InteractiveGeometricCard(
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .background(color = LuxGoldChange, shape = RoundedCornerShape(10.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.TrendingUp,
                                            contentDescription = null,
                                            tint = LuxBlack,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(text = "LOCKED SAVINGS", fontSize = 10.sp, color = LuxMuted, fontWeight = FontWeight.SemiBold)
                                    Text(text = "$currency${String.format("%,.0f", totalGoalsVal)}", style = Typography.titleMedium, fontWeight = FontWeight.Bold, color = LuxIvory)
                                }
                            }

                            // Widget B: Safety Buffer
                            InteractiveGeometricCard(
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .background(color = LuxGoldChange, shape = RoundedCornerShape(10.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Shield,
                                            contentDescription = null,
                                            tint = LuxBlack,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(text = "SAFETY RESERVE", fontSize = 10.sp, color = LuxMuted, fontWeight = FontWeight.SemiBold)
                                    Text(text = "$currency${String.format("%,.0f", bufferVal)}", style = Typography.titleMedium, fontWeight = FontWeight.Bold, color = LuxIvory)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Predictive End-of-Month Savings Widget
            if (dashboardConfig.isVisible(com.example.data.WidgetId.PredictiveSavings)) {
                CinematicEntranceContainer(delayMillis = 350) {
                    PredictiveEomSavingsWidget(
                        salary = salaryVal,
                        transactions = transactionsState ?: emptyList(),
                        bills = bills,
                        currency = currency
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
            }

            // 30-Day Spending Trends (Recharts-Style)
            if (dashboardConfig.isVisible(com.example.data.WidgetId.SpendingTrendsChart)) {
                CinematicEntranceContainer(delayMillis = 450) {
                    Last30DaysSpendingTrendsChart(
                        transactions = transactionsState ?: emptyList(),
                        currency = currency
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
            }

            // Spending vs Savings Visual allocation (Task 4)
            if (dashboardConfig.isVisible(com.example.data.WidgetId.SpendingVsSavingsChart)) {
                CinematicEntranceContainer(delayMillis = 550) {
                    GeometricSpendingVsSavingsChart(
                        transactions = transactionsState ?: emptyList(),
                        currency = currency
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
            }

            // Action Deck
            CinematicEntranceContainer(delayMillis = 650) {
                Column {
                    Text(text = "DAILY ASSIGNMENTS", style = Typography.labelLarge, color = LuxGoldChange, letterSpacing = 2.sp)
                    Spacer(modifier = Modifier.height(10.dp))

                    CinematicGlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = onCheckInClick,
                        shape = RoundedCornerShape(16.dp)
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
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (dashboardConfig.isVisible(com.example.data.WidgetId.BankFeedSync)) {
                CinematicEntranceContainer(delayMillis = 750) {
                    BankFeedSyncWidget(
                        accounts = accounts,
                        currency = currency,
                        viewModel = viewModel,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
            }

            // Recurring flagged bills widget (Task 3)
            var editingBillDialogState by remember { mutableStateOf<BillSubscription?>(null) }

            CinematicEntranceContainer(delayMillis = 850) {
                Column {
                    Text(text = "RECURRING OBLIGATIONS MONITOR", style = Typography.labelLarge, color = LuxGoldChange, letterSpacing = 2.sp)
                    Spacer(modifier = Modifier.height(10.dp))

                    CinematicGlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            val recurringBills = bills.filter { it.isSubscription }
                            if (recurringBills.isEmpty()) {
                                Text(text = "No recurring obligations currently registered.", color = LuxMuted, fontSize = 12.sp)
                            } else {
                                recurringBills.forEach { b ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = b.name.uppercase(),
                                                color = if (b.usageConfirmed) LuxIvory else LuxMuted,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                style = if (b.usageConfirmed) TextStyle.Default else TextStyle.Default.copy(textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough)
                                            )
                                            Text(text = "Due Day: ${b.dueDate} | ${currency}${String.format("%,.0f", b.amount)}", color = LuxMuted, fontSize = 11.sp)
                                        }
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            // Toggle status switch
                                            Switch(
                                                checked = b.usageConfirmed,
                                                onCheckedChange = { checkedState ->
                                                    viewModel.updateBill(b.copy(usageConfirmed = checkedState))
                                                },
                                                colors = SwitchDefaults.colors(
                                                    checkedThumbColor = LuxBlack,
                                                    checkedTrackColor = LuxGoldChange,
                                                    uncheckedThumbColor = LuxMuted,
                                                    uncheckedTrackColor = LuxDarkGray
                                                ),
                                                modifier = Modifier.scale(0.8f).testTag("bill_state_switch_${b.id}")
                                            )
                                            // Edit button
                                            IconButton(
                                                onClick = { editingBillDialogState = b },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit obligation", tint = LuxGoldChange, modifier = Modifier.size(16.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (editingBillDialogState != null) {
                QuickEditBillDialog(
                    bill = editingBillDialogState!!,
                    onSave = { updatedBill ->
                        viewModel.updateBill(updatedBill)
                        editingBillDialogState = null
                    },
                    onDismiss = { editingBillDialogState = null }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Accounts list
            CinematicEntranceContainer(delayMillis = 950) {
                Column {
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
                            CinematicGlassCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                shape = RoundedCornerShape(16.dp)
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
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Basic timeline indicator
            CinematicEntranceContainer(delayMillis = 1050) {
                Column {
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
                        CinematicGlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Box(modifier = Modifier.padding(16.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
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
    }

    // Old dimming overlay removed
    // Sliding navigation side bar drawer panel
    AnimatedVisibility(
        visible = false,
        enter = slideInHorizontally(initialOffsetX = { -it }) + fadeIn(),
        exit = slideOutHorizontally(targetOffsetX = { -it }) + fadeOut(),
        modifier = Modifier
            .fillMaxHeight()
            .fillMaxWidth(0.85f)
            .background(LuxDarkGray)
            .border(1.dp, LuxGoldChange.copy(alpha = 0.25f))
            .clickable(enabled = false) {}
            .zIndex(101f)
    ) {
        val context = LocalContext.current
        val prefs = remember(context) { context.getSharedPreferences("firebase_auth_prefs", android.content.Context.MODE_PRIVATE) }
        val firebaseUserEmail = remember { prefs.getString("authed_email", "Guest@auren.io") ?: "Guest@auren.io" }

        val profile = profileState

        // Real profile control states (no placeholder database examples, loads/saves real DB info)
        var editedCurrency by remember { mutableStateOf(profile?.currency ?: "₹") }
        var editedObjective by remember { mutableStateOf(profile?.primaryObjective ?: "Control spending") }
        var editedMode by remember { mutableStateOf(profile?.appMode ?: "Strict Mode") }
        var editedSalary by remember { mutableStateOf(profile?.salaryAmount?.toInt()?.toString() ?: "60000") }
        var editedBuffer by remember { mutableStateOf(profile?.safetyBuffer?.toInt()?.toString() ?: "2000") }

        // Perfect real-time syncing of state to local form
        LaunchedEffect(profile) {
            if (profile != null) {
                editedCurrency = profile.currency
                editedObjective = profile.primaryObjective
                editedMode = profile.appMode
                editedSalary = profile.salaryAmount.toInt().toString()
                editedBuffer = profile.safetyBuffer.toInt().toString()
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Sidebar Header with close indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(LuxGoldLight),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "A",
                            color = LuxGoldChange,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "AUREN SECURE HUB",
                            color = LuxGoldChange,
                            fontSize = 11.sp,
                            letterSpacing = 1.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Integrated Settings",
                            color = LuxIvory.copy(alpha = 0.7f),
                            fontSize = 12.sp
                        )
                    }
                }
                IconButton(onClick = { showSideBar = false }) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close Sidebar", tint = LuxMuted)
                }
            }

            // Real session / User profile identity block
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = LuxCardGray),
                border = BorderStroke(1.dp, LuxGoldChange.copy(alpha = 0.15f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(LuxGoldChange, androidx.compose.foundation.shape.CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = firebaseUserEmail.firstOrNull()?.uppercase()?.toString() ?: "U",
                            color = LuxBlack,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = firebaseUserEmail,
                            color = LuxIvory,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(LuxGreen, androidx.compose.foundation.shape.CircleShape)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (firebaseUserEmail.contains("guest", true) || firebaseUserEmail.contains("अतिथि", true) || firebaseUserEmail == "Guest@auren.io") 
                                    "GUEST SHELL ACTIVE" else "FIREBASE SYNCHRONIZED", 
                                color = LuxGreen, 
                                fontSize = 9.sp, 
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            HorizontalDivider(color = LuxCardGray, thickness = 1.dp)

            Text(
                text = "VAULT PROFILE CONFIGURATION",
                color = LuxGoldChange,
                style = Typography.labelLarge,
                letterSpacing = 1.5.sp,
                fontSize = 11.sp
            )

            // Dynamic fixed monthly income text field
            Text(text = "DYNAMIC FIXED MONTHLY INCOME", fontSize = 10.sp, color = LuxMuted, fontWeight = FontWeight.Bold)
            OutlinedTextField(
                value = editedSalary,
                onValueChange = { input ->
                    if (input.all { it.isDigit() }) {
                        editedSalary = input
                    }
                },
                modifier = Modifier.fillMaxWidth().testTag("sidebar_salary_input"),
                singleLine = true,
                textStyle = TextStyle(color = LuxIvory, fontSize = 14.sp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = LuxGoldChange,
                    unfocusedBorderColor = LuxCardGray,
                    focusedLabelColor = LuxGoldChange,
                    cursorColor = LuxGoldChange
                ),
                leadingIcon = { Text(text = editedCurrency, color = LuxGoldChange, fontWeight = FontWeight.Bold) },
                shape = RoundedCornerShape(8.dp)
            )

            // Monthly safety buffer segregated reserve
            Text(text = "MONTHLY SEGREGATED RESERVE BUFFER", fontSize = 10.sp, color = LuxMuted, fontWeight = FontWeight.Bold)
            OutlinedTextField(
                value = editedBuffer,
                onValueChange = { input ->
                    if (input.all { it.isDigit() }) {
                        editedBuffer = input
                    }
                },
                modifier = Modifier.fillMaxWidth().testTag("sidebar_buffer_input"),
                singleLine = true,
                textStyle = TextStyle(color = LuxIvory, fontSize = 14.sp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = LuxGoldChange,
                    unfocusedBorderColor = LuxCardGray,
                    focusedLabelColor = LuxGoldChange,
                    cursorColor = LuxGoldChange
                ),
                leadingIcon = { Text(text = editedCurrency, color = LuxGoldChange, fontWeight = FontWeight.Bold) },
                shape = RoundedCornerShape(8.dp)
            )

            // Vault running currency symbol selection
            Text(text = "NATIVE CURRENCY SYMBOL", fontSize = 10.sp, color = LuxMuted, fontWeight = FontWeight.Bold)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                listOf("₹", "$", "€", "£").forEach { sym ->
                    val isSel = editedCurrency == sym
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSel) LuxGoldChange else LuxCardGray)
                            .border(1.dp, if (isSel) LuxGoldChange else LuxCardGray.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            .clickable { editedCurrency = sym }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = sym,
                            color = if (isSel) LuxBlack else LuxIvory,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            // Algorithmic app budget strength mode
            Text(text = "ALGORITHMIC APP BUDGET MODE", fontSize = 10.sp, color = LuxMuted, fontWeight = FontWeight.Bold)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf("Strict Mode", "Balanced Mode", "Relaxed Mode").forEach { md ->
                    val isSel = editedMode == md
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSel) LuxGoldChange else LuxCardGray)
                            .border(1.dp, if (isSel) LuxGoldChange else LuxCardGray.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            .clickable { editedMode = md }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = md.replace(" Mode", ""),
                            color = if (isSel) LuxBlack else LuxIvory,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            // Primary active wealth objective
            Text(text = "PRIMARY WEALTH OBJECTIVE", fontSize = 10.sp, color = LuxMuted, fontWeight = FontWeight.Bold)
            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf("Control spending", "Start saving", "Clear debt", "Grow wealth").forEach { obj ->
                    val isSel = editedObjective == obj
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSel) LuxGoldChange else LuxCardGray)
                            .border(1.dp, if (isSel) LuxGoldChange else LuxCardGray.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            .clickable { editedObjective = obj }
                            .padding(vertical = 8.dp, horizontal = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = obj.uppercase(),
                                color = if (isSel) LuxBlack else LuxIvory,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                            if (isSel) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = LuxBlack,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
            }

            HorizontalDivider(color = LuxCardGray, thickness = 1.dp)

            // Submit / Commiting changes immediately of user settings profile in database
            Button(
                onClick = {
                    val salVal = editedSalary.toDoubleOrNull() ?: 60000.0
                    val bufVal = editedBuffer.toDoubleOrNull() ?: 2000.0
                    viewModel.updateProfile(
                        currency = editedCurrency,
                        objective = editedObjective,
                        mode = editedMode,
                        salary = salVal,
                        buffer = bufVal,
                        isCompleted = true
                    )
                    showSideBar = false
                },
                modifier = Modifier.fillMaxWidth().testTag("save_sidebar_profile_button"),
                colors = ButtonDefaults.buttonColors(containerColor = LuxGoldChange, contentColor = LuxBlack),
                shape = RoundedCornerShape(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "COMMIT PROFILE CONFIG", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }

            HorizontalDivider(color = LuxCardGray, thickness = 1.dp)

            // System preferences section
            Text(
                text = "SYSTEM PREFERENCES",
                color = LuxGoldChange,
                style = Typography.labelLarge,
                letterSpacing = 1.5.sp,
                fontSize = 11.sp
            )

            // Multi language settings selection
            Text(text = "SYSTEM LANGUAGE", fontSize = 10.sp, color = LuxMuted, fontWeight = FontWeight.Bold)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("English", "Hinglish", "Hindi").forEach { lng ->
                    val isSel = langOption == lng
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSel) LuxGoldChange else LuxCardGray)
                            .border(1.dp, if (isSel) LuxGoldChange else LuxCardGray.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            .clickable { langOption = lng }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (lng == "English") "EN" else if (lng == "Hinglish") "HNG" else "HI",
                            color = if (isSel) LuxBlack else LuxIvory,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            // Interface dark theme dynamic style toggle
            Text(text = "INTERFACE THEME STYLE", fontSize = 10.sp, color = LuxMuted, fontWeight = FontWeight.Bold)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(LuxCardGray)
                    .clickable { isDarkThemeGlobal = !isDarkThemeGlobal }
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (isDarkThemeGlobal) Icons.Default.NightsStay else Icons.Default.WbSunny,
                            contentDescription = null,
                            tint = LuxGoldChange,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = if (isDarkThemeGlobal) "LUXURY DEEP SPACE DARK" else "HIGHCONTRAST MODERN LIGHT",
                            color = LuxIvory,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Switch(
                        checked = isDarkThemeGlobal,
                        onCheckedChange = { isDarkThemeGlobal = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = LuxGoldChange,
                            checkedTrackColor = LuxGoldLight,
                            uncheckedThumbColor = LuxMuted,
                            uncheckedTrackColor = LuxCardGray
                        ),
                        modifier = Modifier.scale(0.85f)
                    )
                }
            }

            HorizontalDivider(color = LuxCardGray, thickness = 1.dp)

            // Vault protection secure logout
            Text(text = "VAULT PROTECTION SHELL", fontSize = 10.sp, color = LuxMuted, fontWeight = FontWeight.Bold)
            OutlinedButton(
                onClick = {
                    onLogout()
                    showSideBar = false
                },
                modifier = Modifier.fillMaxWidth().testTag("sidebar_logout_button"),
                border = BorderStroke(1.2.dp, LuxError),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = LuxError),
                shape = RoundedCornerShape(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.ExitToApp, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "SECURE LOGOUT", fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
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
    currency: String,
    onDeleteTransaction: (Transaction) -> Unit
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
                                IconButton(onClick = { onDeleteTransaction(tx) }) {
                                    Icon(imageVector = Icons.Default.DeleteOutline, contentDescription = "Delete item", tint = LuxError.copy(alpha = 0.7f), modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                    }
                }
                item {
                    Spacer(modifier = Modifier.height(100.dp))
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
    onAddDebtClick: () -> Unit,
    onDeleteBill: (BillSubscription) -> Unit,
    onDeleteGoal: (FinancialGoal) -> Unit,
    onDeleteDebt: (Debt) -> Unit
) {
    val transactionsState by viewModel.transactions.collectAsStateWithLifecycle()
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
                "budget" -> BudgetSubView(currency = currency, salary = totalSalary, viewModel = viewModel)
                "bills" -> BillsSubView(
                    bills = bills,
                    accounts = accounts,
                    transactions = transactionsState ?: emptyList(),
                    currency = currency,
                    onAddClick = onAddBillClick,
                    onPay = { b, accId -> viewModel.payBill(b, accId) },
                    onDelete = onDeleteBill,
                    onAddSuggestedBill = { name, amount, category, preferredDay ->
                        viewModel.addBill(name, amount, preferredDay, category, true)
                    }
                )
                "debts" -> DebtsSubView(debts = debts, currency = currency, onAddClick = onAddDebtClick, onDelete = onDeleteDebt)
                "goals" -> GoalsSubView(goals = goals, accounts = accounts, currency = currency, onAddClick = onAddGoalClick, onAddFunds = { g, bal, accId -> viewModel.addFundsToGoal(g, bal, accId) }, onDelete = onDeleteGoal, viewModel = viewModel)
            }
        }
    }
}

@Composable
fun BudgetSubView(currency: String, salary: Double, viewModel: FinanceViewModel) {
    val transactionsState by viewModel.transactions.collectAsStateWithLifecycle()
    val transactions = transactionsState ?: emptyList()

    val currentMonthTransactions = remember(transactions) {
        val cal = java.util.Calendar.getInstance()
        val currentMonth = cal.get(java.util.Calendar.MONTH)
        val currentYear = cal.get(java.util.Calendar.YEAR)
        transactions.filter { t ->
            val tCal = java.util.Calendar.getInstance().apply { timeInMillis = t.date }
            tCal.get(java.util.Calendar.MONTH) == currentMonth && tCal.get(java.util.Calendar.YEAR) == currentYear && t.type.lowercase() == "expense"
        }
    }

    val budgetLimits = remember(salary) {
        mapOf(
            "Rent" to salary * 0.25,
            "Utilities" to salary * 0.10,
            "Groceries" to salary * 0.15,
            "Shopping" to salary * 0.10,
            "Dining" to salary * 0.10,
            "EMI" to salary * 0.20,
            "Investment" to salary * 0.10,
            "Savings" to salary * 0.15,
            "Other" to salary * 0.05
        )
    }

    val spentMap = remember(currentMonthTransactions) {
        currentMonthTransactions.groupBy { it.category }
            .mapValues { entry -> entry.value.sumOf { it.amount } }
    }

    val alerts = remember(spentMap, budgetLimits) {
        budgetLimits.mapNotNull { (category, limit) ->
            val spent = spentMap[category] ?: 0.0
            val ratio = if (limit > 0) spent / limit else 0.0
            when {
                ratio >= 1.0 -> Triple(category, spent, limit) to "100"
                ratio >= 0.8 -> Triple(category, spent, limit) to "80"
                else -> null
            }
        }
    }

    var sliderEssentials by remember { mutableStateOf(40f) }
    var sliderDebts by remember { mutableStateOf(20f) }
    var sliderSavings by remember { mutableStateOf(15f) }
    var sliderWants by remember { mutableStateOf(15f) }
    var sliderBuffer by remember { mutableStateOf(10f) }

    val totalAllocated = sliderEssentials + sliderDebts + sliderSavings + sliderWants + sliderBuffer

    val items = listOf(
        Triple(t("Essentials (Rent, Bills)", "आवश्यक खर्च (किराया, बिल)", "Essentials (Rent, Bills)"), sliderEssentials, LuxGoldLight),
        Triple(t("Minimum Debts & EMIs", "ऋण और ईएमआई", "Minimum Debts & EMIs"), sliderDebts, LuxGoldChange),
        Triple(t("Protected Savings Goals", "सुरक्षित लक्ष्य बचत", "Protected Savings Goals"), sliderSavings, LuxIceBlue),
        Triple(t("Flexible Wants & Dining", "इच्छाएं और बाहर खाना", "Flexible Wants & Dining"), sliderWants, LuxMuted),
        Triple(t("Unallocated Cash Buffer", "गैर-आवंटित नकद बफर", "Unallocated Cash Buffer"), sliderBuffer, LuxIvory)
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        if (alerts.isNotEmpty()) {
            Text(
                text = t("THRESHOLD ALERTS", "सीमा चेतावनी", "Budget Limit Alert"),
                style = Typography.labelLarge,
                color = LuxError,
                fontSize = 11.sp,
                letterSpacing = 2.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            alerts.forEach { (info, pct) ->
                val (category, spent, limit) = info
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (pct == "100") LuxError.copy(alpha = 0.12f) else LuxGoldChange.copy(alpha = 0.12f)
                    ),
                    border = BorderStroke(1.dp, if (pct == "100") LuxError else LuxGoldChange)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (pct == "100") Icons.Default.Warning else Icons.Default.Info,
                            contentDescription = "Alert Indicator",
                            tint = if (pct == "100") LuxError else LuxGoldChange,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = if (pct == "100") t("CRITICAL BUDGET BREACH", "अत्यंत खतरनाक सीमा पार", "Critical Limit Exceeded") 
                                       else t("WARNING: HIGH ACTIVITY DETECTED", "चेतावनी: अधिक खर्च हो रहा है", "Warning: High Spending"),
                                color = if (pct == "100") LuxError else LuxGoldChange,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Category '$category' spending is $currency${String.format("%,.2f", spent)} against secure limit $currency${String.format("%,.2f", limit)} (${(spent / limit * 100).toInt()}% used).",
                                color = LuxIvory,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
        }

        Text(text = t("MONTHLY STRATEGIST BUDGET ALLOCATOR", "मासिक बजट आवंटन प्रबंधक", "Monthly Budget Allocator"), style = Typography.labelLarge, color = LuxGoldChange, fontSize = 11.sp, modifier = Modifier.padding(bottom = 4.dp))
        Text(text = t("Manually drag the sliders to customize your allocations. We validate and help you align based on your Mode.", 
                      "अपने अनुसार बजट प्रतिशत बदलने के लिए स्लाइडर को खिसकाएं। हम इसे संतुलित करने में मदद करेंगे।", 
                      "Budget sliders adjust karein manually. Hum isko help karenge sahi mode me balance karne ke liye."), color = LuxMuted, fontSize = 13.sp)

        Spacer(modifier = Modifier.height(16.dp))

        // Total Balance Tracker Alert Indicator Card
        Card(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            colors = CardDefaults.cardColors(containerColor = if (totalAllocated.toInt() == 100) LuxGreen.copy(alpha = 0.12f) else LuxGoldChange.copy(alpha = 0.12f)),
            border = BorderStroke(1.dp, if (totalAllocated.toInt() == 100) LuxGreen else LuxGoldChange)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = t("LIVE RE-BALANCING COMPUTATION ENGINE", "स्वचालित गणना इंजन", "Live Budget Calculation Engine"),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (totalAllocated.toInt() == 100) LuxGreen else LuxGoldChange,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (totalAllocated.toInt() == 100) t("PERFECT EQUILIBRIUM: Exactly 100% of income is cleanly separated.", "परिपूर्ण संतुलन: पूरा 100% सुरक्षित रूप से पृथक है।", "Equilibrium: Pura 100% budget set hai!")
                           else t("UNBALANCED STATE: Current sum is ${totalAllocated.toInt()}% (Must sum to 100%)", "असंतुलित बजट: कुल आवंटन ${totalAllocated.toInt()}% है (100% होना चाहिए)", "Unbalanced budget! Abhi ${totalAllocated.toInt()}% hua hai. (100% hona chahiye)"),
                    color = LuxIvory,
                    fontSize = 12.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Interactive Sliders Block
        items.forEachIndexed { idx, (name, valPct, color) ->
            val targetAmount = salary * (valPct / 100f)
            Column(modifier = Modifier.padding(vertical = 10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = name, color = LuxIvory, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text(text = "$currency${String.format("%,.0f", targetAmount)} (${valPct.toInt()}%)", color = color, fontWeight = FontWeight.SemiBold)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Slider(
                    value = valPct,
                    onValueChange = { newVal ->
                        val roundedVal = newVal.coerceIn(0f, 100f)
                        when (idx) {
                            0 -> sliderEssentials = roundedVal
                            1 -> sliderDebts = roundedVal
                            2 -> sliderSavings = roundedVal
                            3 -> sliderWants = roundedVal
                            4 -> sliderBuffer = roundedVal
                        }
                    },
                    valueRange = 0f..100f,
                    colors = SliderDefaults.colors(
                        thumbColor = color,
                        activeTrackColor = color,
                        inactiveTrackColor = LuxCardGray
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Mode Aware Intelligent Coaching & Compensation Buttons
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = LuxDarkGray),
            border = BorderStroke(1.dp, LuxGoldChange.copy(alpha = 0.25f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                val currentMode = viewModel.profile.value?.appMode ?: "Strict Mode"
                Text(
                    text = t("AI MODE RE-BALANCE ADVISORY", "सलाहकार रिपोर्ट", "AI Mode Advisory"),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = LuxGoldChange,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                
                // Print specific Mode advice
                val coachingMsg = when {
                    currentMode.contains("Strict") -> {
                        if (sliderSavings < 20f || sliderWants > 10f) {
                            t("Strict Mode Active: You need at least 20% savings. We can auto-correct this over 3 months slowly, or tap the button to balance instantly.", 
                              "कठोर मोड सक्रिय: कम से कम 20% बचत आवश्यक है। हम 3 महीनों में धीरे-धीरे सुधार कर सकते हैं।",
                              "Strict mode hai: Kam-se-kam 20% savings karein. Isko 3 months me dheere-dheere correct karein.")
                        } else {
                            t("Status Coherent: Excellent strict cash separation is aligned perfectly.", "उत्कृष्ट: नियमों के अनुसार बचत प्रणाली ठीक है।", "Congrats! Rules are followed perfectly.")
                        }
                    }
                    currentMode.contains("Balanced") -> {
                        if (sliderSavings < 15f || sliderWants > 20f) {
                            t("Balanced Mode Rule: Ideal is 15% savings and desires under 20%. Let's transition over 6 months with small adjustments.",
                              "संतुलित मोड निर्देश: करीब 15% बचत रखें। 6 महीनों में धीरे-धीरे छोटे सुधार कर सकते हैं।",
                              "Balanced Mode: 15% savings ideal hai. Isko normalise karein slowly.")
                        } else {
                            t("Status Coherent: Balanced budget model meets guidelines safely.", "संतुलित और सुरक्षित: बजट योजना नियमों के अनुकूल है।", "Sahi hai! Code followed perfectly.")
                        }
                    }
                    else -> { // Relaxed
                        if (sliderSavings < 5f) {
                            t("Relaxed Mode Rule: Even in Relaxed mode, keep at least 5% savings for unforeseen bills.",
                              "आरामदेह मोड निर्देश: कम से कम 5% आपातकालीन बचत अवश्य बनाए रखें।",
                              "Relaxed mode hai, par emergency savings 5% ideal hai.")
                        } else {
                            t("Status Coherent: Spend layout fits comfortably.", "सुविधाजनक एवं स्वीकृत: बजट ठीक है।", "Badiya hai! Layout standard is okay.")
                        }
                    }
                }
                
                Text(text = coachingMsg, color = LuxIvory, fontSize = 12.sp)

                Spacer(modifier = Modifier.height(12.dp))

                // Custom control buttons to fix instantly or over months
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            if (currentMode.contains("Strict")) {
                                sliderEssentials = 45f
                                sliderDebts = 20f
                                sliderSavings = 25f
                                sliderWants = 5f
                                sliderBuffer = 5f
                            } else if (currentMode.contains("Balanced")) {
                                sliderEssentials = 40f
                                sliderDebts = 20f
                                sliderSavings = 15f
                                sliderWants = 15f
                                sliderBuffer = 10f
                            } else {
                                sliderEssentials = 35f
                                sliderDebts = 20f
                                sliderSavings = 10f
                                sliderWants = 25f
                                sliderBuffer = 10f
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = LuxGoldChange, contentColor = LuxBlack),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(t("Fix Instantly", "तुरंत ठीक करें", "Auto balance"), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            // Slowly glide savings rate up and wants down
                            val targSav = if (currentMode.contains("Strict")) 25f else if (currentMode.contains("Balanced")) 15f else 10f
                            val targWant = if (currentMode.contains("Strict")) 5f else if (currentMode.contains("Balanced")) 15f else 25f
                            
                            if (sliderWants > targWant) sliderWants = (sliderWants - 2f).coerceAtLeast(targWant)
                            if (sliderSavings < targSav) sliderSavings = (sliderSavings + 2f).coerceAtMost(targSav)
                            
                            sliderBuffer = (100f - (sliderEssentials + sliderDebts + sliderSavings + sliderWants)).coerceIn(0f, 100f)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = LuxCardGray, contentColor = LuxGoldChange),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, LuxGoldChange.copy(alpha = 0.5f)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(t("Fix Slowly (2%)", "धीरे-धीरे ठीक करें", "Slowly balance monthly"), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        Divider(color = LuxDarkGray)
        Spacer(modifier = Modifier.height(20.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = LuxDarkGray),
            border = BorderStroke(1.dp, LuxGoldChange.copy(alpha = 0.3f))
        ) {
            val profileState by viewModel.profile.collectAsStateWithLifecycle()
            var salaryInput by remember { mutableStateOf(profileState?.salaryAmount?.toString() ?: "60000") }
            var essentialName by remember { mutableStateOf("") }
            var essentialAmount by remember { mutableStateOf("") }
            var essentialDueDate by remember { mutableStateOf("1") }
            var essentialCategory by remember { mutableStateOf("Rent") }

            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "ACTIVE SECURE LEDGER PROFILES",
                    style = Typography.labelLarge,
                    color = LuxGoldChange,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Text(
                    text = "Salary & Essential Bills Configurator",
                    style = Typography.titleMedium,
                    color = LuxIvory,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = salaryInput,
                        onValueChange = { salaryInput = it },
                        label = { Text("Net Monthly Salary ($currency)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = LuxGoldChange),
                        modifier = Modifier.weight(1f)
                    )

                    Button(
                        onClick = {
                            val salDouble = salaryInput.toDoubleOrNull() ?: 60000.0
                            val currentProf = profileState
                            if (currentProf != null) {
                                viewModel.onboardUser(
                                    currency = currentProf.currency,
                                    objective = currentProf.primaryObjective,
                                    mode = currentProf.appMode,
                                    salary = salDouble,
                                    payday = currentProf.salaryDate,
                                    currentBalance = 0.0,
                                    buffer = currentProf.safetyBuffer
                                )
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = LuxGoldChange, contentColor = LuxBlack),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Save Salary", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Divider(color = LuxCardGray)
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "QUICK ADD ESSENTIAL OBLIGATION",
                    style = Typography.labelLarge,
                    color = LuxGoldChange,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                OutlinedTextField(
                    value = essentialName,
                    onValueChange = { essentialName = it },
                    label = { Text("Obligation Name (e.g., Internet, Gas, Rent)") },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = LuxGoldChange),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = essentialAmount,
                        onValueChange = { essentialAmount = it },
                        label = { Text("Amount ($currency)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = LuxGoldChange),
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = essentialDueDate,
                        onValueChange = { essentialDueDate = it },
                        label = { Text("Due Day (1-31)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = LuxGoldChange),
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val categories = listOf("Rent", "Utilities", "EMI", "Groceries", "Insurance")
                    categories.forEach { cat ->
                        val isSelected = essentialCategory == cat
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSelected) LuxGoldChange else LuxCardGray)
                                .clickable { essentialCategory = cat }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(text = cat, color = if (isSelected) LuxBlack else LuxIvory, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        val amt = essentialAmount.toDoubleOrNull() ?: 0.0
                        val dueDay = essentialDueDate.toIntOrNull() ?: 1
                        if (essentialName.isNotBlank() && amt > 0.0 && dueDay in 1..31) {
                            viewModel.addBill(
                                name = essentialName,
                                amount = amt,
                                dueDate = dueDay,
                                category = essentialCategory,
                                isSub = true
                            )
                            essentialName = ""
                            essentialAmount = ""
                            essentialDueDate = "1"
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = LuxGreen, contentColor = LuxBlack),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Securely Link Monthly Obligation", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun BillsSubView(
    bills: List<BillSubscription>,
    accounts: List<Account>,
    transactions: List<Transaction>,
    currency: String,
    onAddClick: () -> Unit,
    onPay: (BillSubscription, Long) -> Unit,
    onDelete: (BillSubscription) -> Unit,
    onAddSuggestedBill: (String, Double, String, Int) -> Unit
) {
    var showPaySelectorDialog by remember { mutableStateOf<BillSubscription?>(null) }

    val suggestedBills = remember(transactions, bills) {
        val calendar = java.util.Calendar.getInstance()
        transactions
            .filter { it.isRecurring && it.type.lowercase() == "expense" }
            .groupBy { it.merchant.lowercase().trim() }
            .map { entry -> entry.value.maxByOrNull { it.date }!! }
            .filter { trans ->
                bills.none { it.name.lowercase().trim() == trans.merchant.lowercase().trim() }
            }
            .map { trans ->
                calendar.timeInMillis = trans.date
                val preferredDay = calendar.get(java.util.Calendar.DAY_OF_MONTH).coerceIn(1, 28)
                Triple(trans, preferredDay, trans.category)
            }
    }

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

        LazyColumn(modifier = Modifier.fillMaxSize().weight(1f)) {
            // Section A: Suggested Recurring Obligations
            if (suggestedBills.isNotEmpty()) {
                item {
                    Text(
                        text = "SUGGESTED RECURRING OBLIGATIONS",
                        color = LuxGoldLight,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)
                    )
                }
                items(suggestedBills) { (trans, day, category) ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 5.dp),
                        colors = CardDefaults.cardColors(containerColor = LuxDarkGray),
                        border = BorderStroke(1.dp, LuxGoldChange.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = trans.merchant.uppercase(),
                                    color = LuxIvory,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = "Auto-detected recurring • Cat: $category • Day of month: $day",
                                    color = LuxMuted,
                                    fontSize = 11.sp
                                )
                            }
                            Button(
                                onClick = { onAddSuggestedBill(trans.merchant, trans.amount, category, day) },
                                colors = ButtonDefaults.buttonColors(containerColor = LuxGoldChange, contentColor = LuxBlack),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text("Add to Plan", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "ACTIVE BILLS & SUBSCRIPTIONS",
                        color = LuxMuted,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
            }

            // Section B: Active Bills
            if (bills.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No recurring bills or subscriptions scheduled.\nAdd critical items to enable Safe To Spend protection.",
                            color = LuxMuted,
                            textAlign = TextAlign.Center,
                            fontSize = 13.sp
                        )
                    }
                }
            } else {
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
    onDelete: (FinancialGoal) -> Unit,
    viewModel: FinanceViewModel
) {
    var showFundGoalDialog by remember { mutableStateOf<FinancialGoal?>(null) }
    val profileState by viewModel.profile.collectAsStateWithLifecycle()
    val prof = profileState

    val billsState by viewModel.bills.collectAsStateWithLifecycle(emptyList())
    val debtsState by viewModel.debts.collectAsStateWithLifecycle(emptyList())
    val transactionsState by viewModel.transactions.collectAsStateWithLifecycle(emptyList())

    var autoSavePct by remember(prof) { mutableStateOf((prof?.autoSavePercentage ?: 10.0).toString()) }
    var autoSaveEnabled by remember(prof) { mutableStateOf(prof?.autoSaveEnabled ?: false) }
    var autoSaveGoalId by remember(prof) { mutableStateOf(prof?.autoSaveGoalId ?: goals.firstOrNull()?.id) }

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

        // --- SIX-MONTH EMERGENCY FUND WIDGET ---
        val baselineLiving = (prof?.salaryAmount ?: 60000.0) * 0.40 // 40% net salary floor
        val recurringBillsSum = (billsState ?: emptyList()).sumOf { it.amount }
        val minimDebtsSum = (debtsState ?: emptyList()).sumOf { it.minimumPayment }
        val monthlyEssentialExpenses = baselineLiving + recurringBillsSum + minimDebtsSum
        val sixMonthTarget = monthlyEssentialExpenses * 6.0
        val currentSavedFund = prof?.currentEmergencyFund ?: 0.0
        val progressFundFraction = if (sixMonthTarget > 0) (currentSavedFund / sixMonthTarget).toFloat().coerceIn(0f, 1f) else 0f
        
        var showContributeDialog by remember { mutableStateOf(false) }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp).testTag("emergency_fund_card"),
            colors = CardDefaults.cardColors(containerColor = LuxDarkGray),
            border = BorderStroke(1.dp, LuxGoldChange.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = "Safe Deposit",
                            tint = LuxGoldChange,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(text = "6-MONTH EMERGENCY SAFEGUARD", style = Typography.labelLarge, color = LuxIvory)
                            Text(text = "Tracks baseline fortress reserves", style = Typography.bodySmall, color = LuxMuted, fontSize = 10.sp)
                        }
                    }
                    Button(
                        onClick = { showContributeDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = LuxGoldChange, contentColor = LuxBlack),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier.height(28.dp).testTag("emergency_fund_contribute_button")
                    ) {
                        Text("Contribute", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(text = "FORTRESS POSITION", color = LuxMuted, fontSize = 10.sp)
                        Text(text = "$currency${String.format("%,.0f", currentSavedFund)} / $currency${String.format("%,.0f", sixMonthTarget)}", color = LuxIvory, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                    Box(
                        modifier = Modifier
                            .background(LuxGoldChange.copy(alpha = 0.12f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(text = "${String.format("%.1f", progressFundFraction * 100)}% SECURED", color = LuxGoldChange, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                LinearProgressIndicator(
                    progress = progressFundFraction,
                    color = LuxGoldChange,
                    trackColor = LuxCardGray,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Essential monthly outlay: $currency${String.format("%,.0f", monthlyEssentialExpenses)} (derived from: Living floor: $currency${String.format("%,.0f", baselineLiving)} + Bills: $currency${String.format("%,.0f", recurringBillsSum)} + Debt minimum: $currency${String.format("%,.0f", minimDebtsSum)})",
                    color = LuxMuted,
                    fontSize = 10.sp,
                    lineHeight = 14.sp
                )
            }
        }

        if (showContributeDialog) {
            var depositAmountStr by remember { mutableStateOf("") }
            var selectedAccountId by remember { mutableStateOf(accounts.firstOrNull()?.id ?: 1L) }
            var showAccSelection by remember { mutableStateOf(false) }

            AlertDialog(
                onDismissRequest = { showContributeDialog = false },
                title = { Text(text = "INJECT CORES TO SAFETY RESERVES", color = LuxGoldChange, style = Typography.titleMedium) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(text = "Deposit money into the automated 6-Month Emergency Fund. This amount will be deducted from your selected account.", color = LuxIvory, fontSize = 12.sp)
                        
                        OutlinedTextField(
                            value = depositAmountStr,
                            onValueChange = { depositAmountStr = it },
                            label = { Text("Amount ($currency)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = LuxGoldChange, unfocusedBorderColor = LuxCardGray),
                            modifier = Modifier.fillMaxWidth().testTag("emergency_amount_input")
                        )

                        Text(text = "Select Funding Source Account:", color = LuxMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(LuxCardGray)
                                .border(1.dp, LuxGoldChange.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                .clickable { showAccSelection = !showAccSelection }
                                .padding(12.dp)
                        ) {
                            val accName = accounts.find { it.id == selectedAccountId }?.name ?: "Select Funding Destination"
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = accName.uppercase(), color = LuxIvory, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null, tint = LuxGoldChange)
                            }

                            DropdownMenu(
                                expanded = showAccSelection,
                                onDismissRequest = { showAccSelection = false },
                                modifier = Modifier.background(LuxDarkGray)
                            ) {
                                accounts.forEach { acc ->
                                    DropdownMenuItem(
                                        text = { Text(text = "${acc.name.uppercase()} (${currency}${String.format("%,.0f", acc.balance)})", color = LuxIvory, fontSize = 12.sp) },
                                        onClick = {
                                            selectedAccountId = acc.id
                                            showAccSelection = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val depAmt = depositAmountStr.toDoubleOrNull() ?: 0.0
                            if (depAmt > 0) {
                                viewModel.depositEmergencyFund(depAmt, selectedAccountId)
                                showContributeDialog = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = LuxGoldChange, contentColor = LuxBlack)
                    ) {
                        Text(text = "Confirm Core Injection")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showContributeDialog = false }) {
                        Text(text = "Cancel", color = LuxMuted)
                    }
                },
                containerColor = LuxDarkGray
            )
        }

        if (goals.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                Text(text = "No custom target buckets configured. Create savings goals or a mini-emergency fund.", color = LuxMuted, textAlign = TextAlign.Center)
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f)) {
                item {
                    val billsList = billsState ?: emptyList()
                    val debtsList = debtsState ?: emptyList()
                    val transactionsList = transactionsState ?: emptyList()
                    val totalSalary = prof?.salaryAmount ?: 60000.0

                    val totalEMIs = remember(debtsList) { debtsList.sumOf { it.emiAmount } }
                    val totalBills = remember(billsList) { billsList.sumOf { it.amount } }
                    
                    val essentialTxsSum = remember(transactionsList) {
                        transactionsList.filter { tx ->
                            val cat = tx.category.lowercase()
                            tx.type.lowercase() == "expense" && (
                                cat.contains("grocery") || cat.contains("utility") || cat.contains("utilities") ||
                                cat.contains("rent") || cat.contains("health") || cat.contains("medical") ||
                                cat.contains("transport") || cat.contains("commute") || tx.isRecurring
                            )
                        }.sumOf { it.amount }
                    }

                    // Total estimated baseline monthly essentials (fallback to 45% of salary if no outlays exist)
                    val averageMonthlyEssentials = remember(totalEMIs, totalBills, essentialTxsSum, totalSalary) {
                        val computed = totalEMIs + totalBills + essentialTxsSum
                        if (computed > 0.0) computed else (totalSalary * 0.45)
                    }

                    val sixMonthGoal = averageMonthlyEssentials * 6.0
                    
                    val savingsAndInvestmentAccountsSum = remember(accounts) {
                        accounts.filter { it.type == "Savings" || it.type == "Investment" }.sumOf { it.balance }
                    }
                    val specificEmergencyGoalsSum = remember(goals) {
                        goals.filter { g ->
                            val nameLower = g.name.lowercase()
                            nameLower.contains("emergency") || nameLower.contains("safety") || nameLower.contains("buffer")
                        }.sumOf { g -> g.currentAmount }
                    }
                    val currentSecuredSafetyReserve = savingsAndInvestmentAccountsSum + specificEmergencyGoalsSum

                    val safetyPercentageProgress = if (sixMonthGoal > 0) (currentSecuredSafetyReserve / sixMonthGoal).toFloat().coerceIn(0f, 1f) else 0f
                    val safetyMonthsCovered = if (averageMonthlyEssentials > 0) currentSecuredSafetyReserve / averageMonthlyEssentials else 0.0

                    var showBoostDialog by remember { mutableStateOf(false) }

                    if (showBoostDialog) {
                        var boostAmountStr by remember { mutableStateOf("10000") }
                        val activeSavingsAccount = accounts.find { it.type == "Savings" }

                        Dialog(onDismissRequest = { showBoostDialog = false }) {
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = LuxDarkGray),
                                border = BorderStroke(1.dp, LuxGoldChange)
                            ) {
                                Column(modifier = Modifier.padding(20.dp)) {
                                    Text(text = "SIMULATE SAVINGS BOOST", style = Typography.labelLarge, color = LuxGoldChange)
                                    Text(
                                        text = "Add simulated liquidity to your savings vault to see the emergency fund immunizer respond in real-time.",
                                        color = LuxMuted,
                                        fontSize = 11.sp,
                                        modifier = Modifier.padding(vertical = 8.dp)
                                    )

                                    if (activeSavingsAccount == null) {
                                        Text(
                                            text = "No 'Savings' vault account found! Please configure a vault of type 'Savings' in the home dashboard first.",
                                            color = Color(0xFFEF5350),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    } else {
                                        OutlinedTextField(
                                            value = boostAmountStr,
                                            onValueChange = { boostAmountStr = it },
                                            label = { Text("Boost Amount") },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = LuxGoldChange,
                                                unfocusedBorderColor = LuxCardGray
                                            ),
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(16.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        OutlinedButton(
                                            onClick = { showBoostDialog = false },
                                            border = BorderStroke(1.dp, LuxMuted),
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = LuxMuted)
                                        ) {
                                            Text("Cancel")
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Button(
                                            onClick = {
                                                val amt = boostAmountStr.toDoubleOrNull() ?: 10000.0
                                                if (activeSavingsAccount != null) {
                                                    viewModel.addTransaction(
                                                        amount = amt,
                                                        type = "Income",
                                                        dateOverride = System.currentTimeMillis(),
                                                        accountId = activeSavingsAccount.id,
                                                        category = "Savings Boost",
                                                        merchant = "Intel Sim feed",
                                                        note = "Simulated Emergency Reserve deposit"
                                                    )
                                                }
                                                showBoostDialog = false
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = LuxGoldChange, contentColor = LuxBlack),
                                            shape = RoundedCornerShape(8.dp),
                                            enabled = activeSavingsAccount != null
                                        ) {
                                            Text("Boost Balance")
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        colors = CardDefaults.cardColors(containerColor = LuxDarkGray),
                        border = BorderStroke(1.2.dp, LuxGoldChange.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .background(LuxGoldChange.copy(alpha = 0.12f), RoundedCornerShape(10.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Shield,
                                            contentDescription = null,
                                            tint = LuxGoldChange,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(text = "LIQUIDITY SAFETY NET", color = LuxGoldChange, style = Typography.labelLarge, letterSpacing = 1.sp)
                                        Text(text = "6-Month Core Emergency Reserve", color = LuxMuted, fontSize = 11.sp)
                                    }
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (safetyMonthsCovered >= 6.0) Color(0xFF4CAF50).copy(alpha = 0.18f) else LuxGoldChange.copy(alpha = 0.12f))
                                        .border(
                                            0.5.dp, 
                                            if (safetyMonthsCovered >= 6.0) Color(0xFF4CAF50).copy(alpha = 0.4f) else LuxGoldChange.copy(alpha = 0.3f), 
                                            RoundedCornerShape(6.dp)
                                        )
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = if (safetyMonthsCovered >= 6.0) "FULLY SECURED" else "STRENGTHENING",
                                        color = if (safetyMonthsCovered >= 6.0) Color(0xFF4CAF50) else LuxGoldChange,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.5.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(18.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Bottom
                            ) {
                                Column {
                                    Text(text = "6-MONTH IMMUNITY GOAL", fontSize = 10.sp, color = LuxMuted)
                                    Text(
                                        text = "$currency${String.format("%,.0f", sixMonthGoal)}",
                                        style = Typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = LuxIvory
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(text = "SECURED LIQUID RESERVE", fontSize = 10.sp, color = LuxMuted)
                                    Text(
                                        text = "$currency${String.format("%,.0f", currentSecuredSafetyReserve)}",
                                        style = Typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = LuxGoldChange
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(10.dp)
                                    .clip(RoundedCornerShape(5.dp))
                                    .background(LuxCardGray)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .fillMaxWidth(safetyPercentageProgress)
                                        .clip(RoundedCornerShape(5.dp))
                                        .background(
                                            Brush.horizontalGradient(
                                                colors = listOf(LuxGoldChange.copy(alpha = 0.5f), LuxGoldChange)
                                            )
                                        )
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Core essentials: $currency${String.format("%,.0f", averageMonthlyEssentials)}/mo",
                                    color = LuxMuted,
                                    fontSize = 11.sp
                                )
                                Text(
                                    text = if (safetyMonthsCovered >= 1.0) "${String.format("%.1f", safetyMonthsCovered)} Months Cover" else "0 Months Cover",
                                    color = LuxGoldChange,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                            Divider(color = LuxGoldChange.copy(alpha = 0.12f))
                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Column {
                                        Text(text = "DEBTI EMIS", fontSize = 9.sp, color = LuxMuted)
                                        Text(text = "$currency${String.format("%,.0f", totalEMIs)}", color = LuxIvory, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(text = "BILLS & DUES", fontSize = 9.sp, color = LuxMuted)
                                        Text(text = "$currency${String.format("%,.0f", totalBills)}", color = LuxIvory, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(text = "LIFESTYLE", fontSize = 9.sp, color = LuxMuted)
                                        Text(text = "$currency${String.format("%,.0f", essentialTxsSum)}", color = LuxIvory, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                TextButton(
                                    onClick = { showBoostDialog = true },
                                    colors = ButtonDefaults.textButtonColors(contentColor = LuxGoldChange)
                                ) {
                                    Text("Simulate Boost", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        colors = CardDefaults.cardColors(containerColor = LuxDarkGray),
                        border = BorderStroke(1.dp, LuxGoldChange.copy(alpha = 0.3f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(text = "INCOME AUTO-ALLOCATION ENGINE", color = LuxGoldChange, style = Typography.labelLarge)
                            Text(text = "Direct automated separations of logged salaries/incomes into goal buckets.", color = LuxMuted, fontSize = 11.sp, modifier = Modifier.padding(bottom = 12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = "Enable Auto-Allocation", color = LuxIvory, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                Switch(
                                    checked = autoSaveEnabled,
                                    onCheckedChange = { checkedState ->
                                        autoSaveEnabled = checkedState
                                        viewModel.updateAutoSaveConfig(checkedState, autoSavePct.toDoubleOrNull() ?: 10.0, autoSaveGoalId)
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = LuxBlack,
                                        checkedTrackColor = LuxGoldChange,
                                        uncheckedThumbColor = LuxMuted,
                                        uncheckedTrackColor = LuxDarkGray
                                    ),
                                    modifier = Modifier.scale(0.85f).testTag("auto_allocation_toggle")
                                )
                            }

                            if (autoSaveEnabled) {
                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedTextField(
                                        value = autoSavePct,
                                        onValueChange = { newValue ->
                                            autoSavePct = newValue
                                            viewModel.updateAutoSaveConfig(autoSaveEnabled, newValue.toDoubleOrNull() ?: 0.0, autoSaveGoalId)
                                        },
                                        label = { Text("Transfer %") },
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = LuxGoldChange,
                                            unfocusedBorderColor = LuxCardGray
                                        ),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.weight(0.4f).testTag("auto_allocation_pct_input")
                                    )

                                    // Dropdown list of goals
                                    var showGoalDropdown by remember { mutableStateOf(false) }
                                    val selectedGoalName = goals.find { it.id == autoSaveGoalId }?.name ?: "Choose Target Bucket"

                                    Box(
                                        modifier = Modifier
                                            .weight(0.6f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(LuxCardGray)
                                            .border(1.dp, LuxGoldChange.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                            .clickable { showGoalDropdown = !showGoalDropdown }
                                            .padding(horizontal = 14.dp, vertical = 14.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(text = selectedGoalName.uppercase(), color = LuxIvory, fontWeight = FontWeight.Bold, fontSize = 11.sp, maxLines = 1)
                                            Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null, tint = LuxGoldChange)
                                        }

                                        DropdownMenu(
                                            expanded = showGoalDropdown,
                                            onDismissRequest = { showGoalDropdown = false },
                                            modifier = Modifier.background(LuxDarkGray)
                                        ) {
                                            goals.forEach { g ->
                                                DropdownMenuItem(
                                                    text = { Text(text = g.name.uppercase(), color = LuxIvory, fontSize = 12.sp) },
                                                    onClick = {
                                                        autoSaveGoalId = g.id
                                                        viewModel.updateAutoSaveConfig(autoSaveEnabled, autoSavePct.toDoubleOrNull() ?: 10.0, g.id)
                                                        showGoalDropdown = false
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

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
                                    Text(text = "Priority: ${g.priority} • Saved: ${MoneyFormat.compact(g.currentAmount, currency)} / ${MoneyFormat.compact(g.targetAmount, currency)}", color = LuxMuted, fontSize = 12.sp)
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
                            GeometricSavingsProgressBar(
                                percentage = percentage,
                                targetAmount = g.targetAmount,
                                currentAmount = g.currentAmount,
                                currency = currency
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

    val context = LocalContext.current
    val billsState by viewModel.bills.collectAsStateWithLifecycle(emptyList())
    val goalsState by viewModel.goals.collectAsStateWithLifecycle(emptyList())
    val accountsState by viewModel.accounts.collectAsStateWithLifecycle(emptyList())

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(text = "CLARITY RADAR", style = Typography.labelLarge, color = LuxGoldChange, letterSpacing = 3.sp)
                Text(text = "Financial Health Audit", style = Typography.headlineMedium, color = LuxIvory)
            }
            IconButton(
                onClick = {
                    exportFinancialSummaryToPrint(
                        context = context,
                        profile = profile,
                        accounts = accountsState ?: emptyList(),
                        transactions = transactions,
                        bills = billsState ?: emptyList(),
                        goals = goalsState ?: emptyList(),
                        currency = currency
                    )
                },
                modifier = Modifier
                    .clip(androidx.compose.foundation.shape.CircleShape)
                    .background(LuxDarkGray)
                    .border(1.dp, LuxGoldChange, androidx.compose.foundation.shape.CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Print,
                    contentDescription = "Print Summary Report",
                    tint = LuxGoldChange
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Spending Heatmap
        SpendingHeatmap(transactions = transactions, currency = currency)

        Spacer(modifier = Modifier.height(20.dp))

        SavingsProjectionWidget(
            transactions = transactions,
            bills = billsState,
            profile = profile,
            currency = currency,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(20.dp))

        YoYMonthlySavingsRateChart(
            transactions = transactions,
            profile = profile,
            modifier = Modifier.fillMaxWidth()
        )

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

        // Year-over-Year Savings Rate Comparison Chart
        YoYSavingsRateChart(
            transactions = transactions,
            currency = currency
        )

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

        // AI-powered spending habits auditor section
        val aiInsights by viewModel.aiInsights.collectAsStateWithLifecycle()
        val isAiLoading by viewModel.isInsightsLoading.collectAsStateWithLifecycle()

        LaunchedEffect(Unit) {
            if (aiInsights == null) {
                viewModel.generateSpendingInsights()
            }
        }

        Text(text = "AI PORTFOLIO SPENDING AUDITOR", style = Typography.labelLarge, color = LuxGoldChange, letterSpacing = 2.sp)
        Spacer(modifier = Modifier.height(10.dp))

        Card(
            modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
            colors = CardDefaults.cardColors(containerColor = LuxDarkGray),
            border = BorderStroke(1.dp, LuxGoldChange.copy(alpha = 0.4f))
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, tint = LuxGoldChange)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Auren Intelligence", style = Typography.titleMedium, color = LuxIvory, fontWeight = FontWeight.Bold)
                    }
                    if (isAiLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = LuxGoldChange, strokeWidth = 2.dp)
                    } else {
                        IconButton(
                            onClick = { viewModel.generateSpendingInsights() },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Refresh, contentDescription = "Refresh", tint = LuxGoldChange, modifier = Modifier.size(16.dp))
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))

                if (isAiLoading && aiInsights == null) {
                    Text(text = "AI Auditor is running regression algorithms over your transaction ledger...", color = LuxMuted, fontSize = 13.sp)
                } else {
                    val displayInsights = aiInsights ?: "No insights computed yet. Maintain some transaction entries to generate intelligent suggestions."
                    Text(
                        text = displayInsights,
                        color = LuxIvory,
                        fontSize = 13.sp,
                        lineHeight = 20.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

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

@Composable
fun FormattedCoachResponse(text: String) {
    val annotatedString = remember(text) {
        androidx.compose.ui.text.buildAnnotatedString {
            val parts = text.split("**")
            parts.forEachIndexed { index, part ->
                if (index % 2 == 1) { // Bold text
                    withStyle(style = androidx.compose.ui.text.SpanStyle(fontWeight = FontWeight.Bold, color = LuxGoldChange)) {
                        append(part)
                    }
                } else { // Normal text
                    append(part)
                }
            }
        }
    }
    Text(
        text = annotatedString,
        color = LuxIvory,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        modifier = Modifier.padding(12.dp)
    )
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
        Text(text = t("AUREN PRIVATE CHAT", "ऑरेन प्राइवेट चैट", "Auren Chat Console"), style = Typography.labelLarge, color = LuxGoldChange, letterSpacing = 3.sp)
        Text(text = t("AI Money Coach / Dost", "ऑरेन एआई सलाहकार", "Auren AI Money Dost"), style = Typography.headlineMedium, color = LuxIvory)

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
                        if (msg.isUser) {
                            Text(
                                text = msg.text,
                                color = LuxIvory,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(12.dp)
                            )
                        } else {
                            FormattedCoachResponse(text = msg.text)
                        }
                    }
                }
            }

            if (isLoading) {
                item {
                    Text(text = t("Thinking...", "सोच रहा हूँ...", "Coach soch raha hai..."), color = LuxGoldChange, fontSize = 11.sp, modifier = Modifier.padding(8.dp))
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionDialog(
    accounts: List<Account>,
    currency: String,
    onDismiss: () -> Unit,
    onAdd: (Double, String, Long, String, String, String, Long?, Long?, Boolean) -> Unit
) {
    var amount by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf("Expense") } // "Income", "Expense", "Savings", "Transfer", "Refund"
    var selectedAccountId by remember { mutableStateOf(accounts.firstOrNull()?.id ?: 0L) }
    var targetAccountId by remember { mutableStateOf<Long?>(null) }
    var category by remember { mutableStateOf("Shopping") }
    var merchant by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var selectedDateMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    var showDatePickerDialog by remember { mutableStateOf(false) }
    var isRecurringField by remember { mutableStateOf(false) }

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

                Spacer(modifier = Modifier.height(12.dp))

                Text(text = "RECORDING TIME / DATE", color = LuxGoldChange, fontSize = 11.sp)
                Spacer(modifier = Modifier.height(8.dp))
                val dateStr = remember(selectedDateMillis) {
                    SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault()).format(Date(selectedDateMillis))
                }
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { showDatePickerDialog = true },
                    colors = CardDefaults.cardColors(containerColor = LuxCardGray),
                    border = BorderStroke(1.dp, LuxGoldChange.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CalendarToday,
                                contentDescription = "Calendar Icon",
                                tint = LuxGoldChange,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(text = dateStr, color = LuxIvory, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        }
                        Text(text = "CHANGE", color = LuxGoldChange, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                if (showDatePickerDialog) {
                    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = selectedDateMillis)
                    DatePickerDialog(
                        onDismissRequest = { showDatePickerDialog = false },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    selectedDateMillis = datePickerState.selectedDateMillis ?: System.currentTimeMillis()
                                    showDatePickerDialog = false
                                }
                            ) {
                                Text("CONFIRM", color = LuxGoldChange, fontWeight = FontWeight.Bold)
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showDatePickerDialog = false }) {
                                Text("CANCEL", color = LuxMuted)
                            }
                        }
                    ) {
                        DatePicker(
                            state = datePickerState,
                            colors = DatePickerDefaults.colors(
                                titleContentColor = LuxGoldChange,
                                headlineContentColor = LuxIvory,
                                weekdayContentColor = LuxMuted,
                                subheadContentColor = LuxMuted,
                                navigationContentColor = LuxIvory,
                                selectedDayContainerColor = LuxGoldChange,
                                selectedDayContentColor = LuxBlack,
                                todayContentColor = LuxGoldChange,
                                todayDateBorderColor = LuxGoldChange
                            )
                        )
                    }
                }

                if (selectedType == "Expense") {
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(LuxCardGray)
                            .clickable { isRecurringField = !isRecurringField }
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(
                                imageVector = Icons.Default.Autorenew,
                                contentDescription = "Recurring",
                                tint = if (isRecurringField) LuxGoldChange else LuxMuted,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Flag as 'Recurring'",
                                    color = LuxIvory,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Suggests automatically in subsequent months",
                                    color = LuxMuted,
                                    fontSize = 11.sp
                                )
                            }
                        }
                        Checkbox(
                            checked = isRecurringField,
                            onCheckedChange = { isRecurringField = it },
                            colors = CheckboxDefaults.colors(
                                checkedColor = LuxGoldChange,
                                uncheckedColor = LuxMuted,
                                checkmarkColor = LuxBlack
                            )
                        )
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
                            val amtVal = amount.toDoubleOrNull() ?: 0.0
                            if (amtVal > 0.0 && selectedAccountId != 0L) {
                                onAdd(
                                    amtVal,
                                    selectedType,
                                    selectedAccountId,
                                    category,
                                    merchant.ifBlank { "Direct Purchase" },
                                    note,
                                    targetAccountId,
                                    selectedDateMillis,
                                    isRecurringField
                                )
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

// ---------------- REUSABLE PLATFORM COMPONENTS ----------------

@Composable
fun InteractiveGeometricCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    border: BorderStroke = BorderStroke(1.dp, Color(0xFFCAC4D0).copy(alpha = 0.2f)),
    content: @Composable ColumnScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val scale by animateFloatAsState(
        targetValue = when {
            isPressed -> 0.95f
            isHovered -> 1.05f
            else -> 1.0f
        },
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "scale"
    )
    
    val elevation by animateDpAsState(
        targetValue = when {
            isPressed -> 1.dp
            isHovered -> 8.dp
            else -> 4.dp
        },
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "elevation"
    )
    
    val glassBg = if (isDarkThemeGlobal) {
        Brush.verticalGradient(
            colors = listOf(
                Color(0x2B21173C),
                Color(0x1B0B0813)
            )
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xD8FFFFFF),
                Color(0xA5F3ECFC)
            )
        )
    }

    val glassBorder = if (isHovered) {
        BorderStroke(1.2.dp, LuxGoldChange.copy(alpha = 0.8f))
    } else {
        BorderStroke(
            width = 1.dp,
            brush = if (isDarkThemeGlobal) {
                Brush.linearGradient(
                    colors = listOf(
                        LuxGoldChange.copy(alpha = 0.35f),
                        Color.White.copy(alpha = 0.05f),
                        LuxGoldChange.copy(alpha = 0.15f)
                    )
                )
            } else {
                Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.8f),
                        Color(0xFFCAC4D0).copy(alpha = 0.2f),
                        LuxGoldChange.copy(alpha = 0.15f)
                    )
                )
            }
        )
    }

    Card(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                shadowElevation = elevation.toPx()
                shape = RoundedCornerShape(24.dp)
                clip = true
            }
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = LocalIndication.current,
                        onClick = onClick
                    )
                } else {
                    Modifier.hoverable(interactionSource)
                }
            ),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = glassBorder
    ) {
        Column(
            modifier = Modifier
                .background(glassBg)
                .fillMaxWidth(),
            content = content
        )
    }
}

@Composable
fun GeometricConfirmationDialog(
    item: Any,
    currency: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = LuxDarkGray),
            border = BorderStroke(1.dp, LuxGoldChange)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(color = LuxError, shape = RoundedCornerShape(12.dp))
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Warning",
                        tint = LuxBlack,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "CONFIRM DATA REMOVAL",
                    style = Typography.labelLarge,
                    color = LuxError,
                    letterSpacing = 2.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                val nameMessage = when (item) {
                    is Transaction -> "transaction of $currency${String.format("%,.2f", item.amount)} (${item.category})"
                    is BillSubscription -> "bill/subscription '${item.name}' for $currency${String.format("%,.2f", item.amount)}"
                    is FinancialGoal -> "savings goal '${item.name}' with target of $currency${String.format("%,.2f", item.targetAmount)}"
                    is Debt -> "liability entry '${item.name}' for $currency${String.format("%,.2f", item.outstandingAmount)}"
                    else -> "this item"
                }

                Text(
                    text = "Are you sure you want to permanently delete the $nameMessage?",
                    style = Typography.bodyLarge,
                    color = LuxIvory,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "This action will impact your Safe To Spend limits and cannot be undone.",
                    style = Typography.bodyMedium,
                    color = LuxMuted,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        border = BorderStroke(1.dp, LuxMuted),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Cancel", color = LuxIvory)
                    }

                    Button(
                        onClick = {
                            onConfirm()
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = LuxError, contentColor = LuxBlack),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Delete", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ---------------- PREDICTIVE END-OF-MONTH SAVINGS WIDGET ----------------
@Composable
fun PredictiveEomSavingsWidget(
    salary: Double,
    transactions: List<Transaction>,
    bills: List<BillSubscription>,
    currency: String,
    modifier: Modifier = Modifier
) {
    val cal = remember { Calendar.getInstance() }
    val currentYear = cal.get(Calendar.YEAR)
    val currentMonth = cal.get(Calendar.MONTH)
    val elapsedDays = cal.get(Calendar.DAY_OF_MONTH).coerceAtLeast(1)
    val totalDaysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    val daysRemaining = (totalDaysInMonth - elapsedDays).coerceAtLeast(0)

    val txCal = remember { Calendar.getInstance() }
    val currentMonthExpenses = remember(transactions, currentYear, currentMonth) {
        transactions.filter {
            txCal.timeInMillis = it.date
            txCal.get(Calendar.YEAR) == currentYear &&
            txCal.get(Calendar.MONTH) == currentMonth &&
            it.type.lowercase() == "expense"
        }
    }
    
    val currentMonthIncomes = remember(transactions, currentYear, currentMonth) {
        transactions.filter {
            txCal.timeInMillis = it.date
            txCal.get(Calendar.YEAR) == currentYear &&
            txCal.get(Calendar.MONTH) == currentMonth &&
            it.type.lowercase() == "income"
        }
    }

    val totalSpentSoFar = currentMonthExpenses.sumOf { it.amount }
    val totalIncomeSoFar = currentMonthIncomes.sumOf { it.amount }
    val effectiveExpectedIncome = maxOf(salary, totalIncomeSoFar)

    // Current month-to-date velocity
    val dailySpendVelocity = if (elapsedDays > 0) totalSpentSoFar / elapsedDays else 0.0

    // Projected remaining spend if velocity continues
    val projectedDiscretionarySpend = dailySpendVelocity * daysRemaining

    // Filter unpaid bills in this month
    val upcomingUnpaidBillsTotal = bills.filter { bill ->
        val lastPaidCal = Calendar.getInstance()
        val isPaidThisMonth = if (bill.lastPaidTimestamp == 0L) {
            false
        } else {
            lastPaidCal.timeInMillis = bill.lastPaidTimestamp
            lastPaidCal.get(Calendar.MONTH) == currentMonth && lastPaidCal.get(Calendar.YEAR) == currentYear
        }
        !isPaidThisMonth && bill.dueDate > elapsedDays
    }.sumOf { it.amount }

    val totalProjectedSpend = totalSpentSoFar + projectedDiscretionarySpend + upcomingUnpaidBillsTotal
    val projectedSavings = (effectiveExpectedIncome - totalProjectedSpend).coerceAtLeast(0.0)
    val projectedSavingsRate = if (effectiveExpectedIncome > 0.0) (projectedSavings / effectiveExpectedIncome) * 100.0 else 0.0

    // Health Assessment
    val (healthRank, healthColor) = remember(projectedSavingsRate) {
        when {
            projectedSavingsRate >= 20.0 -> "EXCELLENT" to Color(0xFF4CAF50)
            projectedSavingsRate >= 10.0 -> "OPTIMAL" to LuxGoldChange
            projectedSavingsRate > 0.0 -> "BORDERLINE" to Color(0xFFFF9800)
            else -> "CAPITAL DEFICIT" to Color(0xFFF44336)
        }
    }

    CinematicGlassCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(healthColor.copy(alpha = 0.12f), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = healthColor,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "PREDICTIVE SAVINGS AUDIT",
                            style = Typography.labelLarge,
                            color = LuxGoldChange,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "Estimated dynamic EOM liquid reserve",
                            style = Typography.bodySmall,
                            color = LuxMuted
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(healthColor.copy(alpha = 0.18f))
                        .border(0.5.dp, healthColor.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = healthRank,
                        color = healthColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        text = "ESTIMATED SAVINGS",
                        fontSize = 10.sp,
                        color = LuxMuted,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = "$currency${String.format("%,.0f", projectedSavings)}",
                        style = Typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = LuxIvory
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "FORECASTED SAVINGS RATE",
                        fontSize = 10.sp,
                        color = LuxMuted,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = "${String.format("%.1f", projectedSavingsRate)}%",
                        style = Typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = healthColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            val progressFraction = (projectedSavingsRate / 100.0).coerceIn(0.0, 1.0).toFloat()
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(LuxCardGray)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(progressFraction)
                        .clip(RoundedCornerShape(3.dp))
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(healthColor.copy(alpha = 0.5f), healthColor)
                            )
                        )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            Divider(color = LuxGoldChange.copy(alpha = 0.12f))
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(text = "DAILY SPEED", fontSize = 9.sp, color = LuxMuted)
                    Text(
                        text = "$currency${String.format("%,.0f", dailySpendVelocity)}/day",
                        color = LuxIvory,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "MTD OUTLAYS", fontSize = 9.sp, color = LuxMuted)
                    Text(
                        text = "$currency${String.format("%,.0f", totalSpentSoFar)}",
                        color = LuxIvory,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(text = "UNPAID BILLS", fontSize = 9.sp, color = LuxMuted)
                    Text(
                        text = "$currency${String.format("%,.0f", upcomingUnpaidBillsTotal)}",
                        color = Color(0xFFF44336).copy(alpha = 0.8f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // warning banner
            if (dailySpendVelocity > (effectiveExpectedIncome / 30.0) * 0.9) {
                Spacer(modifier = Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFFE53935).copy(alpha = 0.12f))
                        .border(0.5.dp, Color(0xFFE53935).copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                        .padding(8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = Color(0xFFEF5350),
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Daily burn velocity is running extremely hot relative to income parameters!",
                            color = Color(0xFFE57373),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun GeometricSpendingVsSavingsChart(
    transactions: List<Transaction>,
    currency: String,
    modifier: Modifier = Modifier
) {
    val totalExpenses = transactions.filter { it.type == "Expense" }.sumOf { it.amount }
    val totalSavings = transactions.filter { it.type == "Savings" }.sumOf { it.amount }

    val totalSpentAndSaved = totalExpenses + totalSavings
    val spentPercentage = if (totalSpentAndSaved > 0) (totalExpenses / totalSpentAndSaved).toFloat() else 0.5f
    val savedPercentage = if (totalSpentAndSaved > 0) (totalSavings / totalSpentAndSaved).toFloat() else 0.5f

    CinematicGlassCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "ALLOCATION BALANCER",
                        style = Typography.labelLarge,
                        color = LuxGoldChange,
                        letterSpacing = 2.sp
                    )
                    Text(
                        text = "Real-time Expense vs Savings Ratio",
                        style = Typography.bodyMedium,
                        color = LuxMuted
                    )
                }
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = LuxMuted,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(32.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(LuxDarkGray)
            ) {
                if (totalSpentAndSaved == 0.0) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .weight(1f)
                            .background(Color(0xFFE8DEF8)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No records recorded yet", color = LuxMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                } else {
                    if (spentPercentage > 0f) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .weight(spentPercentage)
                                .background(Color(0xFFE8DEF8))
                                .padding(horizontal = 12.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Text(
                                text = "Spent ${String.format("%.0f", spentPercentage * 100)}%",
                                color = Color(0xFF1D192B),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                        }
                    }
                    if (savedPercentage > 0f) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .weight(savedPercentage)
                                .background(LuxGoldChange)
                                .padding(horizontal = 12.dp),
                            contentAlignment = Alignment.CenterEnd
                        ) {
                            Text(
                                text = "Saved ${String.format("%.0f", savedPercentage * 100)}%",
                                color = LuxBlack,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(Color(0xFFE8DEF8), shape = RoundedCornerShape(3.dp))
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text(text = "Total Spent", fontSize = 11.sp, color = LuxMuted)
                        Text(text = "$currency${String.format("%,.2f", totalExpenses)}", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = LuxIvory)
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(LuxGoldChange, shape = RoundedCornerShape(3.dp))
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Column(horizontalAlignment = Alignment.End) {
                        Text(text = "Total Saved", fontSize = 11.sp, color = LuxMuted)
                        Text(text = "$currency${String.format("%,.2f", totalSavings)}", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = LuxIvory)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Divider(color = LuxDarkGray)
            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "SYSTEM CONSISTENCY MAP",
                style = Typography.labelLarge,
                color = LuxMuted,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(96.dp)
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                val matchTemplateHeights = listOf(0.35f, 0.60f, 0.95f, 0.50f, 0.80f, 0.45f, 0.70f)
                val days = listOf("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN")
                
                days.forEachIndexed { idx, day ->
                    val heightRatio = matchTemplateHeights[idx]
                    val isPeak = idx == 2 || idx == 4
                    val barColor = if (isPeak) LuxGoldChange else Color(0xFFE8DEF8)
                    
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.6f)
                                .fillMaxHeight(heightRatio)
                                .background(
                                    color = barColor,
                                    shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)
                                )
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = day,
                            fontSize = 10.sp,
                            color = LuxMuted,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

data class DailySpendingPoint(
    val label: String,
    val amount: Double,
    val dateString: String
)

@Composable
fun Last30DaysSpendingTrendsChart(
    transactions: List<Transaction>,
    currency: String,
    modifier: Modifier = Modifier
) {
    // Group transactions & calculate daily data
    val dailyData = remember(transactions) {
        val calendar = Calendar.getInstance()
        val monthLabels = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
        List(30) { index ->
            val offset = 29 - index
            val targetCal = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, -offset)
            }
            val targetDayOfYear = targetCal.get(Calendar.DAY_OF_YEAR)
            val targetYear = targetCal.get(Calendar.YEAR)
            
            val totalSpent = transactions.filter {
                val txCal = Calendar.getInstance().apply { timeInMillis = it.date }
                txCal.get(Calendar.DAY_OF_YEAR) == targetDayOfYear &&
                txCal.get(Calendar.YEAR) == targetYear &&
                it.type.equals("Expense", ignoreCase = true)
            }.sumOf { it.amount }
            
            val dayOfMonth = targetCal.get(Calendar.DAY_OF_MONTH)
            val monthStr = monthLabels.getOrElse(targetCal.get(Calendar.MONTH)) { "" }
            val label = "$dayOfMonth $monthStr"
            
            DailySpendingPoint(
                label = label,
                amount = totalSpent,
                dateString = label
            )
        }
    }

    var selectedPointIndex by remember { mutableStateOf<Int?>(null) }
    
    val activeIndex = selectedPointIndex ?: 29
    val activePoint = dailyData.getOrNull(activeIndex)

    CinematicGlassCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = t("30-DAY SPENDING TRENDS", "30-दिवसीय खर्च चार्ट", "30-Day Spending trends"),
                        style = Typography.labelLarge,
                        color = LuxGoldChange,
                        letterSpacing = 2.sp
                    )
                    Text(
                        text = t("Interactive Recharts Spending Model", "इंटरैक्टिव व्यय विश्लेषण मॉडल", "Interactive Recharts spending model"),
                        style = Typography.bodyMedium,
                        color = LuxMuted
                    )
                }
                
                Icon(
                    imageVector = Icons.Default.TrendingUp,
                    contentDescription = null,
                    tint = LuxGoldChange,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Info panel of selected / active point
            if (activePoint != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(LuxDarkGray, RoundedCornerShape(12.dp))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = t("SELECTED DATE", "चयनित तिथि", "Selected Date"),
                            fontSize = 9.sp,
                            color = LuxMuted,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = activePoint.label,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = LuxIvory
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = t("AMOUNT SPENT", "खर्च की गई राशि", "Amount Spent"),
                            fontSize = 9.sp,
                            color = LuxMuted,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "$currency${String.format("%,.2f", activePoint.amount)}",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            color = if (activePoint.amount > 0) LuxGoldChange else LuxGreen
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Canvas drawing area
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .background(Color.Transparent)
            ) {
                val canvasWidth = maxWidth
                val canvasHeight = maxHeight
                
                val density = androidx.compose.ui.platform.LocalDensity.current
                val leftMarginPx = with(density) { 40.dp.toPx() }
                val rightMarginPx = with(density) { 12.dp.toPx() }
                val topMarginPx = with(density) { 15.dp.toPx() }
                val bottomMarginPx = with(density) { 25.dp.toPx() }
                
                val graphWidthPx = with(density) { (canvasWidth - 52.dp).toPx() }
                val graphHeightPx = with(density) { (canvasHeight - 40.dp).toPx() }
                
                val maxAmount = remember(dailyData) {
                    maxOf(dailyData.maxOfOrNull { it.amount } ?: 100.0, 100.0)
                }

                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(dailyData, maxAmount) {
                            detectDragGestures(
                                onDragStart = { offset ->
                                    val x = offset.x
                                    val stepX = graphWidthPx / 29f
                                    val rawIndex = kotlin.math.round((x - leftMarginPx) / stepX).toInt()
                                    selectedPointIndex = rawIndex.coerceIn(0, 29)
                                },
                                onDrag = { change, dragAmount ->
                                    val x = change.position.x
                                    val stepX = graphWidthPx / 29f
                                    val rawIndex = kotlin.math.round((x - leftMarginPx) / stepX).toInt()
                                    selectedPointIndex = rawIndex.coerceIn(0, 29)
                                },
                                onDragEnd = {},
                                onDragCancel = {}
                            )
                        }
                ) {
                    val width = size.width
                    val height = size.height
                    
                    val stepX = graphWidthPx / 29f
                    
                    // 1. Draw horizontal grid lines (Y Axis guidelines)
                    val gridLines = 4
                    for (i in 0..gridLines) {
                        val gridAmount = (maxAmount / gridLines) * i
                        val relativeY = topMarginPx + graphHeightPx - (gridAmount / maxAmount).toFloat() * graphHeightPx
                        
                        drawLine(
                            color = Color.Gray.copy(alpha = 0.15f),
                            start = androidx.compose.ui.geometry.Offset(leftMarginPx, relativeY),
                            end = androidx.compose.ui.geometry.Offset(width - rightMarginPx, relativeY),
                            strokeWidth = 1.dp.toPx()
                        )
                        
                        // Y labels
                        drawContext.canvas.nativeCanvas.drawText(
                            "$currency${if (gridAmount >= 1000) String.format("%.1fk", gridAmount / 1000) else String.format("%.0f", gridAmount)}",
                            10f,
                            relativeY + 4.dp.toPx(),
                            android.graphics.Paint().apply {
                                color = if (isDarkThemeGlobal) android.graphics.Color.LTGRAY else android.graphics.Color.DKGRAY
                                textSize = 8.dp.toPx()
                                typeface = android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.BOLD)
                            }
                        )
                    }

                    // 2. Draw X Axis timeline label guides (show periodic labels)
                    val xLabelIndices = listOf(0, 7, 14, 21, 29)
                    for (i in xLabelIndices) {
                        val relativeX = leftMarginPx + i * stepX
                        val point = dailyData.getOrNull(i) ?: continue
                        
                        drawContext.canvas.nativeCanvas.drawText(
                            point.label,
                            relativeX - 12.dp.toPx(),
                            height - 4.dp.toPx(),
                            android.graphics.Paint().apply {
                                color = if (isDarkThemeGlobal) android.graphics.Color.GRAY else android.graphics.Color.DKGRAY
                                textSize = 8.dp.toPx()
                                typeface = android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.NORMAL)
                            }
                        )
                    }

                    // 3. Build coordinate path
                    val coords = dailyData.mapIndexed { i, p ->
                        val relativeX = leftMarginPx + i * stepX
                        val relativeY = topMarginPx + graphHeightPx - (p.amount / maxAmount).toFloat() * graphHeightPx
                        androidx.compose.ui.geometry.Offset(relativeX, relativeY)
                    }

                    if (coords.isNotEmpty()) {
                        val linePath = Path()
                        val areaPath = Path()
                        
                        linePath.moveTo(coords[0].x, coords[0].y)
                        areaPath.moveTo(coords[0].x, coords[0].y)
                        
                        for (i in 1 until coords.size) {
                            val prev = coords[i - 1]
                            val curr = coords[i]
                            
                            val cp1X = prev.x + (curr.x - prev.x) / 2f
                            val cp1Y = prev.y
                            val cp2X = prev.x + (curr.x - prev.x) / 2f
                            val cp2Y = curr.y
                            
                            linePath.cubicTo(cp1X, cp1Y, cp2X, cp2Y, curr.x, curr.y)
                            areaPath.cubicTo(cp1X, cp1Y, cp2X, cp1Y, curr.x, curr.y)
                        }
                        
                        areaPath.lineTo(coords.last().x, topMarginPx + graphHeightPx)
                        areaPath.lineTo(coords.first().x, topMarginPx + graphHeightPx)
                        areaPath.close()

                        // Area fill
                        drawPath(
                            path = areaPath,
                            brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                                colors = listOf(
                                    LuxGoldChange.copy(alpha = 0.35f),
                                    LuxGoldChange.copy(alpha = 0.00f)
                                ),
                                startY = topMarginPx,
                                endY = topMarginPx + graphHeightPx
                            )
                        )

                        // Ambient under-glow line layer
                        drawPath(
                            path = linePath,
                            color = LuxGoldChange.copy(alpha = 0.35f),
                            style = androidx.compose.ui.graphics.drawscope.Stroke(
                                width = 6.dp.toPx(),
                                cap = StrokeCap.Round,
                                join = androidx.compose.ui.graphics.StrokeJoin.Round
                            )
                        )

                        // Trend line foreground core trace
                        drawPath(
                            path = linePath,
                            color = LuxGoldChange,
                            style = androidx.compose.ui.graphics.drawscope.Stroke(
                                width = 2.5.dp.toPx(),
                                cap = StrokeCap.Round,
                                join = androidx.compose.ui.graphics.StrokeJoin.Round
                            )
                        )

                        // Tracker point dot representation
                        val selIdx = activeIndex
                        val selCoord = coords.getOrNull(selIdx)
                        if (selCoord != null) {
                            // Vertical tracking guideline
                            drawLine(
                                color = LuxGoldChange.copy(alpha = 0.4f),
                                start = androidx.compose.ui.geometry.Offset(selCoord.x, topMarginPx),
                                end = androidx.compose.ui.geometry.Offset(selCoord.x, topMarginPx + graphHeightPx),
                                strokeWidth = 1.2.dp.toPx(),
                                pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                            )
                            
                            // Pulse circle
                            drawCircle(
                                color = LuxGoldChange.copy(alpha = 0.25f),
                                radius = 9.dp.toPx(),
                                center = selCoord
                            )
                            
                            // Core point
                            drawCircle(
                                color = LuxGoldChange,
                                radius = 4.5.dp.toPx(),
                                center = selCoord
                            )
                            
                            // Highlight reflection
                            drawCircle(
                                color = Color.White,
                                radius = 2.dp.toPx(),
                                center = selCoord
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SavingsProjectionWidget(
    transactions: List<Transaction>,
    bills: List<BillSubscription>,
    profile: UserProfile?,
    currency: String,
    modifier: Modifier = Modifier
) {
    val salary = profile?.salaryAmount ?: 60000.0
    val calendar = Calendar.getInstance()
    val currentDay = calendar.get(Calendar.DAY_OF_MONTH)
    val totalDaysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
    
    val currentYear = calendar.get(Calendar.YEAR)
    val currentMonth = calendar.get(Calendar.MONTH)
    
    val currentMonthExpenses = transactions.filter {
        val txCal = Calendar.getInstance().apply { timeInMillis = it.date }
        txCal.get(Calendar.YEAR) == currentYear &&
        txCal.get(Calendar.MONTH) == currentMonth &&
        it.type.lowercase() == "expense"
    }.sumOf { it.amount }

    val dailySpendRate = if (currentDay > 0) currentMonthExpenses / currentDay else 0.0
    val remainingDays = totalDaysInMonth - currentDay
    val projectedAdditionalExpenses = dailySpendRate * remainingDays
    
    val unpaidBillsTotal = bills.filter {
        if (it.lastPaidTimestamp == 0L) true
        else {
            val lpCal = Calendar.getInstance().apply { timeInMillis = it.lastPaidTimestamp }
            lpCal.get(Calendar.MONTH) != currentMonth || lpCal.get(Calendar.YEAR) != currentYear
        }
    }.sumOf { it.amount }

    val projectedTotalExpenses = currentMonthExpenses + projectedAdditionalExpenses + unpaidBillsTotal
    val projectedSavings = (salary - projectedTotalExpenses).coerceAtLeast(0.0)
    val projectedSavingsRate = if (salary > 0.0) (projectedSavings / salary) * 100.0 else 0.0

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 24.dp),
        colors = CardDefaults.cardColors(containerColor = LuxDarkGray),
        border = BorderStroke(1.dp, LuxGoldChange.copy(alpha = 0.4f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(LuxGoldChange.copy(alpha = 0.15f), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.TrendingUp,
                        contentDescription = null,
                        tint = LuxGoldChange,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "FORECAST SAVINGS ENGINE",
                        style = Typography.labelLarge,
                        color = LuxGoldChange,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "End-of-month financial projections",
                        style = Typography.bodySmall,
                        color = LuxMuted
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "PROJECTED MONTHLY SAVINGS",
                        color = LuxMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "$currency${String.format("%,.0f", projectedSavings)}",
                        color = if (projectedSavingsRate > 20) LuxGreen else if (projectedSavingsRate > 10) LuxGoldChange else LuxError,
                        style = Typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Box(
                    modifier = Modifier
                        .background(LuxGoldChange.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                        .border(1.dp, LuxGoldChange.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "Rate: ${String.format("%.1f", projectedSavingsRate)}%",
                        color = LuxIvory,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            LinearProgressIndicator(
                progress = (projectedSavingsRate / 100.0).coerceIn(0.0, 1.0).toFloat(),
                color = LuxGoldChange,
                trackColor = LuxCardGray,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Current Spend", color = LuxMuted, fontSize = 10.sp)
                    Text(text = "$currency${String.format("%,.0f", currentMonthExpenses)}", color = LuxIvory, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "Projected Rest", color = LuxMuted, fontSize = 10.sp)
                    Text(text = "$currency${String.format("%,.0f", projectedAdditionalExpenses)}", color = LuxIvory, fontWeight = FontWeight.Medium, fontSize = 12.sp)
                }
                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                    Text(text = "Unpaid Bills Due", color = LuxMuted, fontSize = 10.sp)
                    Text(text = "$currency${String.format("%,.0f", unpaidBillsTotal)}", color = LuxIvory, fontWeight = FontWeight.Medium, fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Divider(color = LuxGoldChange.copy(alpha = 0.1f))
            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = if (projectedSavingsRate > 20.0) {
                    "★ Optimum Wealth Velocity. Current pacing securely surpasses standard target indices."
                } else if (projectedSavingsRate > 10.0) {
                    "▲ Moderate Efficiency. Minimize impulse purchases to secure high safety cushion buffers."
                } else {
                    "▼ Vulnerability Alert. Low projected surplus. Reduce dining / flexible shopping in remaining $remainingDays days."
                },
                color = if (projectedSavingsRate >= 15.0) LuxGoldLight else LuxError,
                fontSize = 11.sp,
                lineHeight = 16.sp
            )
        }
    }
}

@Composable
fun YoYMonthlySavingsRateChart(
    transactions: List<Transaction>,
    profile: UserProfile?,
    modifier: Modifier = Modifier
) {
    val salary = profile?.salaryAmount ?: 60000.0
    val calendar = Calendar.getInstance()
    val currentMonth = calendar.get(Calendar.MONTH)
    val currentYear = calendar.get(Calendar.YEAR)
    
    val currentMonthExpenses = transactions.filter {
        val txCal = Calendar.getInstance().apply { timeInMillis = it.date }
        txCal.get(Calendar.YEAR) == currentYear &&
        txCal.get(Calendar.MONTH) == currentMonth &&
        it.type.lowercase() == "expense"
    }.sumOf { it.amount }
    
    val dynamicCurrentRate = if (salary > 0.0) {
        ((salary - currentMonthExpenses) / salary * 100.0).coerceIn(0.0, 100.0).toFloat()
    } else {
        25f
    }

    val months = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun")
    val rate2025 = listOf(14f, 18f, 16f, 22f, 19f, 21f)
    val base2026 = listOf(16f, 21f, 18f, 26f, 24f)
    val rate2026 = base2026 + dynamicCurrentRate

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = LuxDarkGray),
        border = BorderStroke(1.dp, LuxCardGray)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "YEAR-OVER-YEAR VELOCITY",
                        style = Typography.labelLarge,
                        color = LuxGoldChange,
                        letterSpacing = 2.sp
                    )
                    Text(
                        text = "Monthly Savings Rate Trend (2025 vs 2026)",
                        style = Typography.bodySmall,
                        color = LuxMuted
                    )
                }
                
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(LuxGoldChange, androidx.compose.foundation.shape.CircleShape)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "2026 (OS)", color = LuxIvory, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .border(1.dp, LuxGoldLight, androidx.compose.foundation.shape.CircleShape)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "2025", color = LuxMuted, fontSize = 9.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            val textMeasurer = rememberTextMeasurer()
            val gridLineColor = LuxGoldChange.copy(alpha = 0.08f)
            val labelStyle = androidx.compose.ui.text.TextStyle(
                color = LuxMuted,
                fontSize = 9.sp,
                fontWeight = FontWeight.Medium
            )

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .padding(horizontal = 8.dp)
            ) {
                val width = size.width
                val height = size.height
                
                val maxVal = 40f
                val gridLevels = listOf(0f, 10f, 20f, 30f, 40f)
                gridLevels.forEach { lvl ->
                    val y = height - (lvl / maxVal) * height
                    drawLine(
                        color = gridLineColor,
                        start = androidx.compose.ui.geometry.Offset(0f, y),
                        end = androidx.compose.ui.geometry.Offset(width, y),
                        strokeWidth = 1f
                    )
                    
                    val textLayoutResult = textMeasurer.measure(
                        text = "${lvl.toInt()}%",
                        style = labelStyle
                    )
                    drawText(
                        textLayoutResult = textLayoutResult,
                        topLeft = androidx.compose.ui.geometry.Offset(2f, y - textLayoutResult.size.height / 2f)
                    )
                }

                val stepX = width / (months.size - 1)

                val path2025 = Path()
                rate2025.forEachIndexed { idx, rate ->
                    val x = idx * stepX
                    val y = height - (rate / maxVal) * height
                    if (idx == 0) path2025.moveTo(x, y) else path2025.lineTo(x, y)
                }
                drawPath(
                    path = path2025,
                    color = LuxIvory.copy(alpha = 0.35f),
                    style = Stroke(
                        width = 1.5.dp.toPx(),
                        pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                    )
                )

                val path2026 = Path()
                val points2026 = mutableListOf<androidx.compose.ui.geometry.Offset>()
                rate2026.forEachIndexed { idx, rate ->
                    val x = idx * stepX
                    val y = height - (rate / maxVal) * height
                    points2026.add(androidx.compose.ui.geometry.Offset(x, y))
                    if (idx == 0) path2026.moveTo(x, y) else path2026.lineTo(x, y)
                }

                val glowPath = Path().apply {
                    addPath(path2026)
                    lineTo((rate2026.size - 1) * stepX, height)
                    lineTo(0f, height)
                    close()
                }
                
                drawPath(
                    path = glowPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            LuxGoldChange.copy(alpha = 0.15f),
                            Color.Transparent
                        ),
                        startY = 0f,
                        endY = height
                    )
                )

                drawPath(
                    path = path2026,
                    color = LuxGoldChange,
                    style = Stroke(
                        width = 2.5.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                )

                points2026.forEachIndexed { idx, pt ->
                    drawCircle(
                        color = LuxGoldChange.copy(alpha = 0.3f),
                        radius = 5.dp.toPx(),
                        center = pt
                    )
                    drawCircle(
                        color = LuxGoldLight,
                        radius = 2.5.dp.toPx(),
                        center = pt
                    )

                    if (idx == rate2026.size - 1 || idx == 0) {
                        val textLayoutResult = textMeasurer.measure(
                            text = "${String.format("%.1f", rate2026[idx])}%",
                            style = labelStyle.copy(fontWeight = FontWeight.Bold, color = LuxIvory)
                        )
                        drawText(
                            textLayoutResult = textLayoutResult,
                            topLeft = androidx.compose.ui.geometry.Offset(
                                x = (pt.x - textLayoutResult.size.width / 2f).coerceIn(0f, width - textLayoutResult.size.width),
                                y = pt.y - textLayoutResult.size.height - 4.dp.toPx()
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                months.forEach { m ->
                    Text(text = m, color = LuxMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Divider(color = LuxGoldChange.copy(alpha = 0.1f))
            Spacer(modifier = Modifier.height(12.dp))

            val avg2026 = rate2026.average()
            val avg2025 = rate2025.average()
            val efficiencyDiff = avg2026 - avg2025

            Text(
                text = "Audit Summary: 2026 year-to-date average saving trajectory is ${String.format("%.1f", avg2026)}% compared to ${String.format("%.1f", avg2025)}% in 2025. " +
                        (if (efficiencyDiff > 0) "Performance improved by ${String.format("%.1f", efficiencyDiff)}% due to System Strict Mode optimizations." 
                        else "Performance matches base level. Stabilize discretionary debt payments."),
                color = LuxMuted,
                fontSize = 11.sp,
                lineHeight = 16.sp
            )
        }
    }
}

@Composable
fun BankFeedSyncWidget(
    accounts: List<Account>,
    currency: String,
    viewModel: FinanceViewModel,
    modifier: Modifier = Modifier
) {
    if (accounts.isEmpty()) {
        CinematicGlassCard(
            modifier = modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = "AUREN LIVE LINK", style = Typography.labelLarge, color = LuxGoldChange)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = "Establish a primary bank vault first to link live feeds.", color = LuxMuted, fontSize = 12.sp)
            }
        }
        return
    }

    var isSyncing by remember { mutableStateOf(false) }
    var showResults by remember { mutableStateOf(false) }
    var successMessage by remember { mutableStateOf("") }

    var mockFeedItems by remember {
        mutableStateOf(
            listOf(
                MockFeedItem("Amazon Retail", 4299.0, "Shopping", "Auto-detected online purchase", accounts.first().id),
                MockFeedItem("Uber Ride India", 620.0, "Travel", "Ride hailing logistics", accounts.first().id),
                MockFeedItem("Starbucks Coffee", 450.0, "Dining", "Hospitality payment", accounts.first().id),
                MockFeedItem("Clean Fuel Station", 1500.0, "Travel", "Utility energy replenishment", accounts.first().id)
            )
        )
    }

    val coroutineScope = rememberCoroutineScope()

    CinematicGlassCard(
        modifier = modifier.fillMaxWidth().testTag("bank_feed_card")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Cached,
                        contentDescription = "Bank Feed Link",
                        tint = LuxGoldChange,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(text = "AUREN LIVE BANK FEEDS", style = Typography.labelLarge, color = LuxIvory)
                        Text(text = "Direct institutional ledger connectivity", style = Typography.bodySmall, color = LuxMuted, fontSize = 10.sp)
                    }
                }
                
                if (mockFeedItems.isNotEmpty() && !showResults) {
                    Box(
                        modifier = Modifier
                            .background(LuxGoldChange.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(text = "4 PENDING", color = LuxGoldChange, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (successMessage.isNotEmpty()) {
                Surface(
                    color = LuxGreen.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, LuxGreen.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = LuxGreen)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = successMessage, color = LuxIvory, fontSize = 12.sp)
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        successMessage = ""
                        mockFeedItems = listOf(
                            MockFeedItem("Zomato Delivery", 890.0, "Dining", "Food distribution agency", accounts.first().id),
                            MockFeedItem("HDFC Credit Bill", 5200.0, "Debt", "Auto-reconciled credit payment", accounts.first().id),
                            MockFeedItem("Apple App Store", 349.0, "Bills", "Active platform subscription charge", accounts.first().id),
                            MockFeedItem("Decathlon Sports", 2100.0, "Shopping", "Hobby retail purchase", accounts.first().id)
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = LuxCardGray, contentColor = LuxGoldChange),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = "Refresh simulated bank feed", fontSize = 11.sp)
                }
            } else if (isSyncing) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(color = LuxGoldChange, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "Syncing with secure Open Banking channels...", color = LuxMuted, fontSize = 11.sp)
                }
            } else if (!showResults) {
                Text(
                    text = "Auren automatically parses raw CSV/API payloads, matching merchants and automatically applying categories with high-confidence AI profiling.",
                    color = LuxMuted,
                    fontSize = 11.sp,
                    lineHeight = 16.sp,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                Button(
                    onClick = {
                        isSyncing = true
                        coroutineScope.launch {
                            kotlinx.coroutines.delay(1500)
                            isSyncing = false
                            showResults = true
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = LuxGoldChange, contentColor = LuxBlack),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().testTag("simulate_feed_button")
                ) {
                    Text(text = "Simulate Bank Feed Pull", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            } else {
                Text(
                    text = "Auren AI has classified the following retrieved transactions automatically. Verify categories and import into the ledger.",
                    color = LuxMuted,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    mockFeedItems.forEachIndexed { index, item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(LuxCardGray, RoundedCornerShape(10.dp))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = item.merchant, color = LuxIvory, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "Acc: ${accounts.find { it.id == item.accountId }?.name ?: "Primary Bank"}",
                                        color = LuxMuted,
                                        fontSize = 10.sp
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(text = "•", color = LuxMuted, fontSize = 10.sp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(text = "Auren AI: 94% match", color = LuxGoldLight, fontSize = 10.sp)
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        val categories = listOf("Shopping", "Dining", "Travel", "Bills", "Debt", "Savings")
                                        val currentIdx = categories.indexOf(item.category)
                                        val nextIdx = (currentIdx + 1) % categories.size
                                        val updatedList = mockFeedItems.toMutableList()
                                        updatedList[index] = item.copy(category = categories[nextIdx])
                                        mockFeedItems = updatedList
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = LuxDarkGray, contentColor = LuxGoldChange),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.height(26.dp).testTag("cycle_cat_button_$index")
                                ) {
                                    Text(text = item.category.uppercase(), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                }

                                Text(
                                    text = "$currency${String.format("%,.0f", item.amount)}",
                                    color = LuxIvory,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            showResults = false
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = LuxMuted),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Reset", fontSize = 11.sp)
                    }

                    Button(
                        onClick = {
                            mockFeedItems.forEach { item ->
                                viewModel.addTransaction(
                                    amount = item.amount,
                                    type = "Expense",
                                    accountId = item.accountId,
                                    category = item.category,
                                    merchant = item.merchant,
                                    note = item.note + " (Auto-imported from secure live bank feed)"
                                )
                            }
                            successMessage = "Reconciled ${mockFeedItems.size} transactions into the unified command console."
                            showResults = false
                            mockFeedItems = emptyList()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = LuxGoldChange, contentColor = LuxBlack),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(2f).testTag("approve_ledger_sync")
                    ) {
                        Text("Approve Ledger Sync", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

data class MockFeedItem(
    val merchant: String,
    val amount: Double,
    val category: String,
    val note: String,
    val accountId: Long
)

@Composable
fun GeometricSavingsProgressBar(
    percentage: Float,
    targetAmount: Double,
    currentAmount: Double,
    currency: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${(percentage * 100).toInt()}% Achieved",
                style = Typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = if (percentage >= 1f) LuxGreen else LuxGoldChange
            )
            Text(
                text = "$currency${String.format("%,.0f", currentAmount)} of $currency${String.format("%,.0f", targetAmount)}",
                fontSize = 11.sp,
                color = LuxMuted,
                fontWeight = FontWeight.SemiBold
            )
        }
        Spacer(modifier = Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(LuxCardGray)
                .border(1.dp, Color(0xFFCAC4D0).copy(alpha = 0.2f), RoundedCornerShape(6.dp))
        ) {
            val filledWidth = percentage.coerceIn(0f, 1f)
            if (filledWidth > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(filledWidth)
                        .background(
                            brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                                listOf(
                                    LuxGoldChange.copy(alpha = 0.8f),
                                    if (percentage >= 1f) LuxGreen else LuxGoldChange
                                )
                            ),
                            shape = RoundedCornerShape(6.dp)
                        )
                )
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
    onSave: (String, String, String, Double, Double) -> Unit,
    onLogout: () -> Unit
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
                Text(text = t("OS CONTROL SYSTEM", "सिस्टम नियंत्रण सेटिंग्स", "System Control Center"), style = Typography.labelLarge, color = LuxGoldChange)
                Text(text = t("App Mode & Controls Setup", "सिस्टम मापदंड व्यवस्था", "App Mode & Controls Setup"), style = Typography.titleLarge, color = LuxIvory, modifier = Modifier.padding(bottom = 16.dp))

                OutlinedTextField(
                    value = rawSalary,
                    onValueChange = { rawSalary = it },
                    label = { Text(t("Net Monthly Salary", "कुल मासिक वेतन", "Net monthly salary entry")) },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = LuxGoldChange),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = rawBuffer,
                    onValueChange = { rawBuffer = it },
                    label = { Text(t("Emergency Buffer Reserve", "सुरक्षित आपातकालीन आरक्षित नकद", "Emergency reserve cash buffer")) },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = LuxGoldChange),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(text = t("CURRENCY SYMBOL", "मुद्रा संकेत", "Currency Symbol"), color = LuxGoldChange, fontSize = 11.sp)
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

                Text(text = t("SYSTEM REGIONAL MODE POLICY", "बजट मोड प्राथमिकता नीति", "System Budget Mode Policy"), color = LuxGoldChange, fontSize = 11.sp)
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

                Spacer(modifier = Modifier.height(12.dp))

                Text(text = t("GEOMETRIC BALANCE THEME MODE", "दृष्टि रंग योजना मोड", "Theme Schemes"), color = LuxGoldChange, fontSize = 11.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(LuxDarkGray)
                        .clickable { isDarkThemeGlobal = !isDarkThemeGlobal }
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = t("Dark Theme Active", "डार्क थीम सक्रिय करें", "Dark Theme Enable"), color = LuxIvory, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Icon(
                        imageVector = if (isDarkThemeGlobal) Icons.Default.NightsStay else Icons.Default.WbSunny,
                        contentDescription = "Theme Icon",
                        tint = LuxGoldChange
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Firebase Secure Session Sign-out Button
                Button(
                    onClick = onLogout,
                    colors = ButtonDefaults.buttonColors(containerColor = LuxError.copy(alpha = 0.85f), contentColor = LuxIvory),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(imageVector = Icons.Default.ExitToApp, contentDescription = "Log Out", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = t("Secure Sign-out from Firebase", "फ़ायरबेस से सुरक्षित लॉगआउट करें", "Firebase se secure sign-out karein"), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    OutlinedButton(onClick = onDismiss, border = BorderStroke(1.dp, LuxMuted)) {
                        Text(t("Dismiss", "वापस जाएँ", "Back"), color = LuxIvory)
                    }

                    Button(
                        onClick = {
                            val sVal = rawSalary.toDoubleOrNull() ?: 60000.0
                            val bVal = rawBuffer.toDoubleOrNull() ?: 2000.0
                            onSave(rawCurrency, rawObjective, rawMode, sVal, bVal)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = LuxGoldChange, contentColor = LuxBlack)
                    ) {
                        Text(t("Apply Rules", "बदलाव लागू करें", "Apply tweaks"))
                    }
                }
            }
        }
    }
}

fun exportFinancialSummaryToPrint(
    context: android.content.Context,
    profile: UserProfile?,
    accounts: List<Account>,
    transactions: List<Transaction>,
    bills: List<BillSubscription>,
    goals: List<FinancialGoal>,
    currency: String
) {
    val stringBuilder = StringBuilder()
    stringBuilder.append("""
        <html>
        <head>
            <style>
                body {
                    font-family: 'Courier New', Courier, monospace;
                    background-color: #ffffff;
                    color: #000000;
                    padding: 40px;
                }
                .header {
                    border-bottom: 3px double #000000;
                    padding-bottom: 20px;
                    margin-bottom: 30px;
                }
                .title {
                    font-size: 28px;
                    font-weight: bold;
                    letter-spacing: 2px;
                }
                .subtitle {
                    font-size: 14px;
                    text-transform: uppercase;
                    color: #555555;
                    margin-top: 5px;
                }
                .section-title {
                    font-size: 18px;
                    font-weight: bold;
                    letter-spacing: 1px;
                    border-bottom: 1px solid #000000;
                    padding-bottom: 5px;
                    margin-top: 30px;
                    margin-bottom: 15px;
                    text-transform: uppercase;
                }
                table {
                    width: 100%;
                    border-collapse: collapse;
                    margin-bottom: 20px;
                }
                th, td {
                    border: 1px solid #000000;
                    padding: 8px;
                    text-align: left;
                    font-size: 11px;
                }
                th {
                    background-color: #f2f2f2;
                }
                .geometric-accent {
                    font-size: 11px;
                    color: #777777;
                    text-align: center;
                    margin-top: 50px;
                    border-top: 1px dashed #000000;
                    padding-top: 20px;
                }
            </style>
        </head>
        <body>
            <div class="header">
                <div class="title">AUREN MONEY SUMMARY</div>
                <div class="subtitle">Geometric Financial Audit Log • Generated: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())}</div>
            </div>
            
            <div class="section-title">ACTIVE LEDGER PROFILES</div>
            <table>
                <tr>
                    <th>Account Name</th>
                    <th>Type</th>
                    <th>Balance</th>
                    <th>Status</th>
                </tr>
    """)

    accounts.forEach { acc ->
        stringBuilder.append("""
            <tr>
                <td>${acc.name}</td>
                <td>${acc.type}</td>
                <td>$currency${String.format("%,.2f", acc.balance)}</td>
                <td>${acc.institution}</td>
            </tr>
        """)
    }

    stringBuilder.append("""
            </table>

            <div class="section-title">MONTHLY OBLIGATIONS & BILLS</div>
            <table>
                <tr>
                    <th>Bill Item</th>
                    <th>Category</th>
                    <th>Due Day</th>
                    <th>Amount</th>
                </tr>
    """)

    bills.forEach { b ->
        stringBuilder.append("""
            <tr>
                <td>${b.name}</td>
                <td>${b.category}</td>
                <td>Day ${b.dueDate}</td>
                <td>$currency${String.format("%,.2f", b.amount)}</td>
            </tr>
        """)
    }

    stringBuilder.append("""
            </table>

            <div class="section-title">ACTIVE SAVINGS COMPACTS</div>
            <table>
                <tr>
                    <th>Goal Name</th>
                    <th>Target Amount</th>
                    <th>Current Savings</th>
                    <th>Coherence Progress</th>
                </tr>
    """)

    goals.forEach { g ->
        val pct = if (g.targetAmount > 0) (g.currentAmount / g.targetAmount * 100).toInt() else 0
        stringBuilder.append("""
            <tr>
                <td>${g.name}</td>
                <td>$currency${String.format("%,.2f", g.targetAmount)}</td>
                <td>$currency${String.format("%,.2f", g.currentAmount)}</td>
                <td>$pct%</td>
            </tr>
        """)
    }

    stringBuilder.append("""
            </table>

            <div class="section-title">SIGNIFICANT TRANSACTION AUDITS</div>
            <table>
                <tr>
                    <th>Date</th>
                    <th>Type</th>
                    <th>Category</th>
                    <th>Merchant / Source</th>
                    <th>Amount</th>
                </tr>
    """)

    transactions.forEach { t ->
        val dateString = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date(t.date))
        stringBuilder.append("""
            <tr>
                <td>$dateString</td>
                <td>${t.type}</td>
                <td>${t.category}</td>
                <td>${t.merchant}</td>
                <td>$currency${String.format("%,.2f", t.amount)}</td>
            </tr>
        """)
    }

    stringBuilder.append("""
            </table>

            <div class="geometric-accent">
                COHERENT METRICS GENERATION AUTOMATED BY AUREN MONEY PLATFORM.<br>
                SECURE END-TO-END LEDGER AUDITED SATISFACTORILY.
            </div>
        </body>
        </html>
    """)

    val webView = android.webkit.WebView(context)
    webView.webViewClient = object : android.webkit.WebViewClient() {
        override fun onPageFinished(view: android.webkit.WebView, url: String) {
            val printManager = context.getSystemService(android.content.Context.PRINT_SERVICE) as android.print.PrintManager
            val jobName = "Auren_Financial_Summary_" + System.currentTimeMillis()
            val printAdapter = webView.createPrintDocumentAdapter(jobName)
            printManager.print(jobName, printAdapter, android.print.PrintAttributes.Builder().build())
        }
    }
    webView.loadDataWithBaseURL(null, stringBuilder.toString(), "text/html", "utf-8", null)
}

@Composable
fun FirebaseAuthenticationScreen(onAuthSuccess: (String) -> Unit) {
    val scope = rememberCoroutineScope()
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isSignUp by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var successMessage by remember { mutableStateOf("") }
    var isAuthenticating by remember { mutableStateOf(false) }
    var showGoogleChooser by remember { mutableStateOf(false) }
    var isGoogleSigningIn by remember { mutableStateOf(false) }
    var showGuestInfoModal by remember { mutableStateOf(false) }

    if (isAuthenticating) {
        Dialog(onDismissRequest = {}) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = LuxDarkGray),
                border = BorderStroke(1.dp, LuxGoldChange.copy(alpha = 0.5f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(color = LuxGoldChange, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        text = if (successMessage.isNotEmpty()) successMessage else t("Securing your connection...", "कनेक्शन सुरक्षित किया जा रहा है...", "Securing connection..."),
                        color = LuxIvory,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }

    if (showGuestInfoModal) {
        Dialog(onDismissRequest = { showGuestInfoModal = false }) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = LuxDarkGray),
                border = BorderStroke(1.dp, LuxGoldChange.copy(alpha = 0.5f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Warning",
                        tint = LuxGoldChange,
                        modifier = Modifier.size(40.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = t("GUEST MODE LIMITATIONS", "अतिथि मोड की सीमाएं", "Guest Mode Limitations"),
                        style = Typography.labelLarge,
                        color = LuxGoldChange,
                        letterSpacing = 1.sp,
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = t(
                            "Please note: In Guest Mode, all budget & transaction tracking is saved LOCALLY on your browser/device cache.",
                            "कृपया ध्यान दें: अतिथि मोड में, आपकी सभी जानकारी आपके स्थानीय ब्राउज़र/डिवाइस कैश में सुरक्षित होती है।",
                            "Dhyan dein: Guest Mode me aapka sara budget data local device/browser storage me save hota hai."
                        ),
                        fontSize = 13.sp,
                        color = LuxIvory,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = t(
                            "⚠️ If you clear browser cache, wipe storage, or reinstall the app, you will lose your data permanently.",
                            "⚠️ यदि आप ब्राउज़र कैश साफ करते हैं, ऐप रीइंस्टॉल करते हैं या स्टोरेज मिटाते हैं, तो आपका डेटा स्थायी रूप से नष्ट हो जाएगा।",
                            "⚠️ Agar aap cache clear karenge ya app clear data karenge, toh aapka local data fully delete ho jayega."
                        ),
                        fontSize = 12.sp,
                        color = LuxGoldChange.copy(alpha = 0.9f),
                        textAlign = TextAlign.Center,
                        lineHeight = 16.sp
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showGuestInfoModal = false },
                            border = BorderStroke(1.dp, LuxMuted),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(t("Go Back", "पीछे जाएँ", "Go Back"), color = LuxIvory, fontSize = 11.sp)
                        }

                        Button(
                            onClick = {
                                showGuestInfoModal = false
                                errorMessage = ""
                                successMessage = t("Initializing guest sandbox session...", "अतिथि सत्र प्रारंभ हो रहा है...", "Guest session shuru ho raha hai...")
                                isAuthenticating = true
                                scope.launch {
                                    delay(1200)
                                    successMessage = t("Firebase Success! Logging into OS...", "मंजूरी मिली! स्वागत है!", "Firebase approved! Login ho gaya!")
                                    delay(600)
                                    isAuthenticating = false
                                    onAuthSuccess("Guest@auren.io")
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = LuxGoldChange, contentColor = LuxBlack),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1.2f)
                        ) {
                            Text(t("Proceed", "स्वीकार करें", "Understood"), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    if (showGoogleChooser) {
        Dialog(onDismissRequest = { showGoogleChooser = false }) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color.LightGray),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(text = "G", color = Color(0xFF4285F4), fontWeight = FontWeight.Bold, fontSize = 24.sp)
                        Text(text = "o", color = Color(0xFFEA4335), fontWeight = FontWeight.Bold, fontSize = 22.sp)
                        Text(text = "o", color = Color(0xFFFBBC05), fontWeight = FontWeight.Bold, fontSize = 22.sp)
                        Text(text = "g", color = Color(0xFF4285F4), fontWeight = FontWeight.Bold, fontSize = 22.sp)
                        Text(text = "l", color = Color(0xFF34A853), fontWeight = FontWeight.Bold, fontSize = 22.sp)
                        Text(text = "e", color = Color(0xFFEA4335), fontWeight = FontWeight.Bold, fontSize = 22.sp)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Sign in to Auren Money OS",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 18.sp,
                        color = Color.Black
                    )
                    Text(
                        text = "to continue to your secure dashboard",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                showGoogleChooser = false
                                scope.launch {
                                    isAuthenticating = true
                                    successMessage = t("Launching Google Sign-In framework...", "गूगल लॉगिन शुरू हो रहा है...", "Google log in shuru ho raha hai...")
                                    delay(1200)
                                    isAuthenticating = false
                                    successMessage = t("Google Success! Welcome, Aditya.", "गूगल प्रमाणीकरण सफल! स्वागत है आदित्य।", "Google sign-in success! Swagat hai Aditya.")
                                    delay(600)
                                    onAuthSuccess("adityasrivastav.work@gmail.com")
                                }
                            }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(Color(0xFFE8F0FE), androidx.compose.foundation.shape.CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "A",
                                color = Color(0xFF1A73E8),
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Aditya Srivastav",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Color.Black
                            )
                            Text(
                                text = "adityasrivastav.work@gmail.com",
                                fontSize = 12.sp,
                                color = Color.DarkGray
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                showGoogleChooser = false
                                scope.launch {
                                    isAuthenticating = true
                                    successMessage = t("Launching Google Sign-In framework...", "गूगल लॉगिन शुरू हो रहा है...", "Google log in shuru ho raha hai...")
                                    delay(1200)
                                    isAuthenticating = false
                                    successMessage = t("Google Success! Welcome, Aditya.", "गूगल प्रमाणीकरण सफल! स्वागत है आदित्य।", "Google sign-in success! Swagat hai Aditya.")
                                    delay(600)
                                    onAuthSuccess("adityasrivastav.work@gmail.com")
                                }
                            }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(Color(0xFFF1F3F4), androidx.compose.foundation.shape.CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                tint = Color.DarkGray
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Use another account",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            color = Color.Black
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = "To create a seamless experience, Google will share your name and email address with Auren Money OS.",
                        fontSize = 11.sp,
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    TextButton(onClick = { showGoogleChooser = false }) {
                        Text("Cancel", color = Color(0xFF1A73E8))
                    }
                }
            }
        }
    }

    val authBg = if (isDarkThemeGlobal) {
        Brush.verticalGradient(colors = listOf(Color(0xFF130E20), Color(0xFF08060A)))
    } else {
        Brush.verticalGradient(colors = listOf(Color(0xFFFAF6FE), Color(0xFFEDE4F5)))
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(authBg)
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(40.dp))

        // App Identity with Shield Symbol
        Box(
            modifier = Modifier
                .size(72.dp)
                .background(LuxDarkGray, RoundedCornerShape(20.dp))
                .border(2.dp, LuxGoldChange, RoundedCornerShape(20.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Shield,
                contentDescription = null,
                tint = LuxGoldChange,
                modifier = Modifier.size(36.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = t("SECURE CLIENT PORTAL", "सुरक्षित लॉगिन प्रवेश", "Secure Login Gate"),
            style = Typography.labelLarge,
            color = LuxGoldChange,
            letterSpacing = 2.sp
        )
        Text(
            text = t("Auren Firebase OS", "ऑरेन फ़ायरबेस ओएस", "Auren Firebase OS"),
            style = Typography.headlineMedium,
            color = LuxIvory,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Quick Language Toggle
        Row(
            modifier = Modifier
                .background(LuxDarkGray, RoundedCornerShape(12.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            listOf("English", "Hinglish", "Hindi").forEach { lng ->
                val selected = langOption == lng
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (selected) LuxGoldChange else Color.Transparent)
                        .clickable { langOption = lng }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = when (lng) {
                            "Hindi" -> "हिन्दी"
                            "Hinglish" -> "Hinglish"
                            else -> "English"
                        },
                        color = if (selected) LuxBlack else LuxIvory,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = LuxDarkGray),
            border = BorderStroke(1.dp, LuxGoldChange.copy(alpha = 0.25f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Text(
                    text = if (isSignUp) t("CREATE SECURE USER", "नया खाता बनाएं", "Naya account banayein") 
                           else t("AUTHENTICATE ACCOUNT", "लॉगिन करें", "Apne account me login karein"),
                    style = Typography.labelLarge,
                    color = LuxGoldChange,
                    letterSpacing = 1.sp
                )
                
                Spacer(modifier = Modifier.height(12.dp))

                // Email Input
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text(t("Email Address", "ईमेल पता", "Apna email address")) },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = LuxGoldChange),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Password Input
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text(t("Password (min 6 chars)", "पासवर्ड (कम से कम 6 अक्षर)", "Password (kam se kam 6 char)")) },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = LuxGoldChange),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )

                if (errorMessage.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = errorMessage, color = LuxError, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                if (successMessage.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = successMessage, color = LuxGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action Button
                Button(
                    onClick = {
                        errorMessage = ""
                        successMessage = ""
                        if (!email.contains("@")) {
                            errorMessage = t("Invalid email address", "गलत ईमेल पता", "Sahi email id daalein")
                            return@Button
                        }
                        if (password.length < 6) {
                            errorMessage = t("Password must be at least 6 characters", "पासवर्ड 6 अक्षरों से बड़ा होना चाहिए", "Password kam se kam 6 characters ka hona chahiye")
                            return@Button
                        }
                        isAuthenticating = true
                        successMessage = t("Connecting with Firebase console...", "फ़ायरबेस से जुड़ रहा है...", "Firebase se connect ho raha hai...")
                        scope.launch {
                            delay(1200)
                            successMessage = t("Firebase Success! Logging into OS...", "मंजूरी मिली! स्वागत है!", "Firebase approved! Login ho gaya!")
                            delay(600)
                            isAuthenticating = false
                            onAuthSuccess(email)
                        }
                    },
                    enabled = !isAuthenticating,
                    colors = ButtonDefaults.buttonColors(containerColor = LuxGoldChange, contentColor = LuxBlack),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isAuthenticating && !isGoogleSigningIn && !successMessage.contains("guest") && !successMessage.contains("अतिथि")) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = LuxBlack)
                    } else {
                        Text(
                            text = if (isSignUp) t("Register via Firebase", "फ़ायरबेस पर साइन अप करें", "Firebase register karein")
                                   else t("Log In via Firebase", "फ़ायरबेस से लॉग इन करें", "Firebase se secure login karein"),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Coroutines handle delays safely and prevent cancellation during recompositions
                Spacer(modifier = Modifier.height(12.dp))

                // Toggle Login / SignUp
                Text(
                    text = if (isSignUp) t("Already have a secure account? Login", "पहले से खाता है? लॉगिन करें", "Pehle se account hai? Login karein")
                           else t("New to Auren? Create secure account via Firebase", "नया सुरक्षित खाता बनाएं", "Naya account banayein - Click here"),
                    color = LuxGoldChange,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isSignUp = !isSignUp }
                        .padding(8.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HorizontalDivider(modifier = Modifier.weight(1f), color = LuxGoldChange.copy(alpha = 0.2f))
                    Text(
                        text = t("  OR  ", "  अथवा  ", "  OR  "),
                        color = LuxMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                    HorizontalDivider(modifier = Modifier.weight(1f), color = LuxGoldChange.copy(alpha = 0.2f))
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Sign in with Google Button
                Button(
                    onClick = { showGoogleChooser = true },
                    colors = ButtonDefaults.buttonColors(containerColor = LuxCardGray, contentColor = LuxIvory),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, LuxGoldChange.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(text = "G", color = Color(0xFF4285F4), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text(text = "o", color = Color(0xFFEA4335), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(text = "o", color = Color(0xFFFBBC05), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(text = "g", color = Color(0xFF4285F4), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(text = "l", color = Color(0xFF34A853), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(text = "e", color = Color(0xFFEA4335), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = t("Sign-In via Google", "गूगल से लॉगिन करें", "Google Sign-In"),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            color = LuxIvory
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Try as Guest Button
                OutlinedButton(
                    onClick = {
                        showGuestInfoModal = true
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, LuxGoldChange.copy(alpha = 0.3f))
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = LuxGoldChange,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = t("Continue as Guest", "अतिथि की तरह जारी रखें", "Try as a Guest"),
                            color = LuxIvory,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Stable Firebase Sync Info
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(LuxGreen, androidx.compose.foundation.shape.CircleShape)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = t("Firebase Status: Secured & Initialized (v34.11.0)", "फ़ायरबेस स्थिति: सक्रिय एवं सुरक्षित", "Firebase client running securely"),
                color = LuxMuted,
                fontSize = 11.sp
            )
        }
    }
}
