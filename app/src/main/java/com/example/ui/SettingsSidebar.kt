package com.example.ui

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.data.UserProfile
import com.example.ui.theme.LuxBlack
import com.example.ui.theme.LuxCardGray
import com.example.ui.theme.LuxDarkGray
import com.example.ui.theme.LuxError
import com.example.ui.theme.LuxGoldChange
import com.example.ui.theme.LuxGoldLight
import com.example.ui.theme.LuxGreen
import com.example.ui.theme.LuxIvory
import com.example.ui.theme.LuxMuted
import com.example.ui.theme.Typography
import com.example.ui.theme.isDarkThemeGlobal
import com.example.ui.theme.langOption

/**
 * Settings sidebar — a clean list → detail navigation pattern.
 *
 * Replaces the old 500-line tabbed inline panel that stacked every form on one
 * screen. Now the drawer opens with a quiet vertical list (icon · label · value ·
 * chevron) and tapping a row swaps the panel to its dedicated editor.
 *
 * Inspired by iOS Settings / Material 3 Navigation Drawer. Per AGENT.md §5
 * (readability) — one decision per screen, generous spacing, no competing anchors.
 */

private enum class SettingsRoute { LIST, IDENTITY, INCOME, BUFFER, CURRENCY, MODE, OBJECTIVE, LANGUAGE, THEME, WIDGETS, SHAKE, RESET, LOGOUT }

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun SettingsSidebar(
    visible: Boolean,
    profile: UserProfile?,
    userEmail: String,
    viewModel: FinanceViewModel,
    onDismiss: () -> Unit,
    onLogout: () -> Unit
) {
    // Dimming scrim — tappable to dismiss
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(spring(stiffness = Spring.StiffnessMedium)),
        exit = fadeOut(spring(stiffness = Spring.StiffnessHigh)),
        modifier = Modifier.zIndex(100f)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.55f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { onDismiss() }
        )
    }

    AnimatedVisibility(
        visible = visible,
        enter = slideInHorizontally(
            initialOffsetX = { -it },
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = Spring.StiffnessMediumLow
            )
        ) + fadeIn(spring(stiffness = Spring.StiffnessMedium)),
        exit = slideOutHorizontally(
            targetOffsetX = { -it },
            animationSpec = spring(stiffness = Spring.StiffnessMedium)
        ) + fadeOut(spring(stiffness = Spring.StiffnessHigh)),
        modifier = Modifier
            .fillMaxHeight()
            .fillMaxWidth(0.86f)
            .zIndex(101f)
    ) {
        var route by remember { mutableStateOf(SettingsRoute.LIST) }

        // Local edit state — mirrors profile, committed via viewModel.updateProfile.
        var editedCurrency by remember { mutableStateOf(profile?.currency ?: "₹") }
        var editedObjective by remember { mutableStateOf(profile?.primaryObjective ?: "Control spending") }
        var editedMode by remember { mutableStateOf(profile?.appMode ?: "Strict Mode") }
        var editedSalary by remember { mutableStateOf(profile?.salaryAmount?.toInt()?.toString() ?: "60000") }
        var editedBuffer by remember { mutableStateOf(profile?.safetyBuffer?.toInt()?.toString() ?: "2000") }
        LaunchedEffect(profile) {
            if (profile != null) {
                editedCurrency = profile.currency
                editedObjective = profile.primaryObjective
                editedMode = profile.appMode
                editedSalary = profile.salaryAmount.toInt().toString()
                editedBuffer = profile.safetyBuffer.toInt().toString()
            }
        }
        // Reset route when the drawer is closed so it always reopens on the list.
        LaunchedEffect(visible) { if (!visible) route = SettingsRoute.LIST }

        // Commit helper — used by every detail screen that mutates profile fields.
        fun commitProfile() {
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
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(LuxDarkGray)
                .border(1.dp, LuxGoldChange.copy(alpha = 0.18f))
                .statusBarsPadding()
                .navigationBarsPadding()
                .clickable(enabled = false) {}  // swallow taps so scrim doesn't fire
        ) {
            // Header — title plus close/back. Switches affordance based on route.
            SidebarHeader(
                title = titleFor(route),
                showBack = route != SettingsRoute.LIST,
                onBack = { route = SettingsRoute.LIST },
                onClose = onDismiss
            )

            HorizontalDivider(color = LuxCardGray, thickness = 1.dp)

            // Body — animated between list and detail.
            AnimatedContent(
                targetState = route,
                label = "settingsRouteAnim",
                transitionSpec = {
                    val goingDeeper = targetState.ordinal > initialState.ordinal
                    if (goingDeeper) {
                        slideInHorizontally(spring(stiffness = Spring.StiffnessMediumLow)) { it / 6 } +
                            fadeIn(spring()) togetherWith
                            slideOutHorizontally(spring(stiffness = Spring.StiffnessMediumLow)) { -it / 6 } +
                            fadeOut(spring(stiffness = Spring.StiffnessHigh))
                    } else {
                        slideInHorizontally(spring(stiffness = Spring.StiffnessMediumLow)) { -it / 6 } +
                            fadeIn(spring()) togetherWith
                            slideOutHorizontally(spring(stiffness = Spring.StiffnessMediumLow)) { it / 6 } +
                            fadeOut(spring(stiffness = Spring.StiffnessHigh))
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) { r ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    when (r) {
                        SettingsRoute.LIST -> SettingsListScreen(
                            userEmail = userEmail,
                            currency = editedCurrency,
                            salary = editedSalary,
                            buffer = editedBuffer,
                            mode = editedMode,
                            objective = editedObjective,
                            onRoute = { route = it }
                        )
                        SettingsRoute.IDENTITY -> IdentityCard(userEmail = userEmail)
                        SettingsRoute.INCOME -> SingleNumberEditor(
                            label = "Monthly income",
                            helper = "The fixed amount you receive every payday. Used to compute Safe-to-Spend and the Predictive End-of-Month savings.",
                            value = editedSalary,
                            currency = editedCurrency,
                            onChange = { editedSalary = it },
                            onSave = { commitProfile(); route = SettingsRoute.LIST },
                            testTag = "sidebar_salary_input"
                        )
                        SettingsRoute.BUFFER -> SingleNumberEditor(
                            label = "Safety buffer",
                            helper = "Cash reserved on every cycle and excluded from Safe-to-Spend. Acts as a final cushion against overshooting.",
                            value = editedBuffer,
                            currency = editedCurrency,
                            onChange = { editedBuffer = it },
                            onSave = { commitProfile(); route = SettingsRoute.LIST },
                            testTag = "sidebar_buffer_input"
                        )
                        SettingsRoute.CURRENCY -> ChoiceList(
                            options = listOf("₹", "$", "€", "£", "¥"),
                            selected = editedCurrency,
                            display = { it },
                            onSelect = { editedCurrency = it; commitProfile(); route = SettingsRoute.LIST }
                        )
                        SettingsRoute.MODE -> ChoiceList(
                            options = listOf("Strict Mode", "Balanced Mode", "Relaxed Mode"),
                            selected = editedMode,
                            display = { it },
                            description = { when (it) {
                                "Strict Mode" -> "Hardest. Buffer + bills are locked first; Safe-to-Spend stays low to enforce discipline."
                                "Balanced Mode" -> "Default. Lets you spend down to a calculated daily figure while protecting essentials."
                                else -> "Most flexible. Useful when income or expenses vary widely month to month."
                            } },
                            onSelect = { editedMode = it; commitProfile(); route = SettingsRoute.LIST }
                        )
                        SettingsRoute.OBJECTIVE -> ChoiceList(
                            options = listOf("Control spending", "Start saving", "Clear debt", "Grow wealth"),
                            selected = editedObjective,
                            display = { it },
                            onSelect = { editedObjective = it; commitProfile(); route = SettingsRoute.LIST }
                        )
                        SettingsRoute.LANGUAGE -> ChoiceList(
                            options = listOf("English", "Hinglish", "Hindi"),
                            selected = langOption,
                            display = { it },
                            onSelect = { langOption = it; route = SettingsRoute.LIST }
                        )
                        SettingsRoute.THEME -> ThemeEditor()
                        SettingsRoute.WIDGETS -> WidgetsEditor(viewModel = viewModel)
                        SettingsRoute.SHAKE -> ShakeToAddEditor(viewModel = viewModel, profile = profile)
                        SettingsRoute.RESET -> ResetSetupConfirm(
                            onCancel = { route = SettingsRoute.LIST },
                            onConfirm = {
                                viewModel.resetOnboarding()
                                onDismiss()
                            }
                        )
                        SettingsRoute.LOGOUT -> LogoutConfirm(
                            onCancel = { route = SettingsRoute.LIST },
                            onConfirm = {
                                onLogout()
                                onDismiss()
                            }
                        )
                    }
                }
            }
        }
    }
}

/* ─────────────────────────── Header ─────────────────────────── */

@Composable
private fun SidebarHeader(
    title: String,
    showBack: Boolean,
    onBack: () -> Unit,
    onClose: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (showBack) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = LuxGoldChange)
                }
            } else {
                Box(
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .size(28.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(LuxGoldLight),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Æ",
                        color = LuxGoldChange,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }
            Spacer(Modifier.width(10.dp))
            Text(
                text = title,
                color = LuxIvory,
                style = Typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
        IconButton(onClick = onClose) {
            Icon(Icons.Default.Close, contentDescription = "Close", tint = LuxMuted)
        }
    }
}

private fun titleFor(r: SettingsRoute): String = when (r) {
    SettingsRoute.LIST -> "Settings"
    SettingsRoute.IDENTITY -> "Identity"
    SettingsRoute.INCOME -> "Monthly income"
    SettingsRoute.BUFFER -> "Safety buffer"
    SettingsRoute.CURRENCY -> "Currency"
    SettingsRoute.MODE -> "Budget mode"
    SettingsRoute.OBJECTIVE -> "Wealth objective"
    SettingsRoute.LANGUAGE -> "Language"
    SettingsRoute.THEME -> "Appearance"
    SettingsRoute.WIDGETS -> "Dashboard widgets"
    SettingsRoute.SHAKE -> "Shake to add"
    SettingsRoute.RESET -> "Reset setup"
    SettingsRoute.LOGOUT -> "Sign out"
}

/* ─────────────────────────── List ─────────────────────────── */

@Composable
private fun SettingsListScreen(
    userEmail: String,
    currency: String,
    salary: String,
    buffer: String,
    mode: String,
    objective: String,
    onRoute: (SettingsRoute) -> Unit
) {
    // Identity card — single visual focal point at the top.
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onRoute(SettingsRoute.IDENTITY) },
        colors = CardDefaults.cardColors(containerColor = LuxCardGray),
        border = BorderStroke(1.dp, LuxGoldChange.copy(alpha = 0.18f)),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(LuxGoldChange, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = userEmail.firstOrNull()?.uppercase()?.toString() ?: "U",
                    color = LuxBlack,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = userEmail,
                    color = LuxIvory,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(LuxGreen, CircleShape)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = if (userEmail.contains("guest", true)) "Guest session" else "Firebase synchronized",
                        color = LuxMuted,
                        fontSize = 11.sp
                    )
                }
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = LuxMuted)
        }
    }

    // Section: Vault profile
    SidebarSectionLabel("Vault profile")
    SidebarRow(
        icon = Icons.Default.Savings,
        label = "Monthly income",
        value = "$currency$salary",
        onClick = { onRoute(SettingsRoute.INCOME) }
    )
    SidebarRow(
        icon = Icons.Default.Shield,
        label = "Safety buffer",
        value = "$currency$buffer",
        onClick = { onRoute(SettingsRoute.BUFFER) }
    )
    SidebarRow(
        icon = Icons.Default.CurrencyExchange,
        label = "Currency",
        value = currency,
        onClick = { onRoute(SettingsRoute.CURRENCY) }
    )
    SidebarRow(
        icon = Icons.Default.Tune,
        label = "Budget mode",
        value = mode.replace(" Mode", ""),
        onClick = { onRoute(SettingsRoute.MODE) }
    )
    SidebarRow(
        icon = Icons.Default.Flag,
        label = "Wealth objective",
        value = objective,
        onClick = { onRoute(SettingsRoute.OBJECTIVE) }
    )

    // Section: Preferences
    SidebarSectionLabel("Preferences")
    SidebarRow(
        icon = Icons.Default.Language,
        label = "Language",
        value = langOption,
        onClick = { onRoute(SettingsRoute.LANGUAGE) }
    )
    SidebarRow(
        icon = if (isDarkThemeGlobal) Icons.Default.DarkMode else Icons.Default.LightMode,
        label = "Appearance",
        value = if (isDarkThemeGlobal) "Dark" else "Light",
        onClick = { onRoute(SettingsRoute.THEME) }
    )
    SidebarRow(
        icon = Icons.Default.Tune,
        label = "Dashboard widgets",
        value = "Customize",
        onClick = { onRoute(SettingsRoute.WIDGETS) }
    )
    SidebarRow(
        icon = Icons.Default.PhoneAndroid,
        label = "Shake to add transaction",
        value = "Quick log",
        onClick = { onRoute(SettingsRoute.SHAKE) }
    )

    // Section: Security
    SidebarSectionLabel("Security")
    SidebarRow(
        icon = Icons.Default.VerifiedUser,
        label = "Session",
        value = "Active",
        onClick = { onRoute(SettingsRoute.IDENTITY) }
    )
    SidebarRow(
        icon = Icons.Default.Refresh,
        label = "Re-run setup wizard",
        value = "",
        onClick = { onRoute(SettingsRoute.RESET) }
    )
    SidebarRow(
        icon = Icons.Default.ExitToApp,
        label = "Sign out",
        value = "",
        isDestructive = true,
        onClick = { onRoute(SettingsRoute.LOGOUT) }
    )

    Spacer(Modifier.height(8.dp))
    Text(
        text = "Auren Money OS · v1.0",
        color = LuxMuted.copy(alpha = 0.6f),
        fontSize = 10.sp,
        modifier = Modifier.fillMaxWidth(),
        textAlign = androidx.compose.ui.text.style.TextAlign.Center
    )
}

@Composable
private fun SidebarSectionLabel(text: String) {
    Spacer(Modifier.height(8.dp))
    Text(
        text = text.uppercase(),
        color = LuxGoldChange,
        fontSize = 10.sp,
        letterSpacing = 2.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
    )
}

@Composable
private fun SidebarRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    isDestructive: Boolean = false,
    onClick: () -> Unit
) {
    val labelColor = if (isDestructive) LuxError else LuxIvory
    val iconColor = if (isDestructive) LuxError else LuxGoldChange
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .background(LuxCardGray.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(iconColor.copy(alpha = 0.12f), RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(14.dp))
        Text(
            text = label,
            color = labelColor,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )
        if (value.isNotEmpty()) {
            Text(
                text = value,
                color = LuxMuted,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(end = 8.dp)
            )
        }
        Icon(
            Icons.Default.ChevronRight,
            contentDescription = null,
            tint = LuxMuted.copy(alpha = 0.7f),
            modifier = Modifier.size(18.dp)
        )
    }
}

/* ─────────────────────────── Detail screens ─────────────────────────── */

@Composable
private fun IdentityCard(userEmail: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = LuxCardGray),
        border = BorderStroke(1.dp, LuxGoldChange.copy(alpha = 0.18f)),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .background(LuxGoldChange, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = userEmail.firstOrNull()?.uppercase()?.toString() ?: "U",
                    color = LuxBlack,
                    fontWeight = FontWeight.Bold,
                    fontSize = 28.sp
                )
            }
            Spacer(Modifier.height(14.dp))
            Text(text = userEmail, color = LuxIvory, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(LuxGreen, CircleShape)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = if (userEmail.contains("guest", true)) "Guest shell active" else "Firebase synchronized",
                    color = LuxGreen,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
    InfoTip(text = "Your session is bound to this device. Sign out anytime from Settings → Sign out.")
}

@Composable
private fun SingleNumberEditor(
    label: String,
    helper: String,
    value: String,
    currency: String,
    onChange: (String) -> Unit,
    onSave: () -> Unit,
    testTag: String
) {
    Text(text = label.uppercase(), fontSize = 10.sp, color = LuxMuted, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
    OutlinedTextField(
        value = value,
        onValueChange = { input -> if (input.all { it.isDigit() }) onChange(input) },
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag),
        singleLine = true,
        textStyle = TextStyle(color = LuxIvory, fontSize = 18.sp, fontWeight = FontWeight.SemiBold),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = LuxGoldChange,
            unfocusedBorderColor = LuxCardGray,
            cursorColor = LuxGoldChange
        ),
        leadingIcon = { Text(text = currency, color = LuxGoldChange, fontWeight = FontWeight.Bold, fontSize = 16.sp) },
        shape = RoundedCornerShape(12.dp)
    )
    InfoTip(text = helper)
    Spacer(Modifier.height(4.dp))
    Button(
        onClick = onSave,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(containerColor = LuxGoldChange, contentColor = LuxBlack),
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(text = "Save", fontWeight = FontWeight.Bold, fontSize = 13.sp)
    }
}

@Composable
private fun <T> ChoiceList(
    options: List<T>,
    selected: T,
    display: (T) -> String,
    description: ((T) -> String)? = null,
    onSelect: (T) -> Unit
) {
    options.forEach { opt ->
        val isSel = opt == selected
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onSelect(opt) },
            colors = CardDefaults.cardColors(
                containerColor = if (isSel) LuxGoldChange else LuxCardGray.copy(alpha = 0.4f)
            ),
            border = BorderStroke(
                1.dp,
                if (isSel) LuxGoldChange else LuxCardGray
            ),
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
                        text = display(opt),
                        color = if (isSel) LuxBlack else LuxIvory,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    description?.invoke(opt)?.let { desc ->
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = desc,
                            color = if (isSel) LuxBlack.copy(alpha = 0.7f) else LuxMuted,
                            fontSize = 11.sp,
                            lineHeight = 14.sp
                        )
                    }
                }
                if (isSel) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .background(LuxBlack, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(LuxGoldChange, CircleShape)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ThemeEditor() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { isDarkThemeGlobal = !isDarkThemeGlobal },
        colors = CardDefaults.cardColors(containerColor = LuxCardGray.copy(alpha = 0.4f)),
        border = BorderStroke(1.dp, LuxCardGray),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isDarkThemeGlobal) Icons.Default.DarkMode else Icons.Default.LightMode,
                contentDescription = null,
                tint = LuxGoldChange,
                modifier = Modifier.size(22.dp)
            )
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isDarkThemeGlobal) "Dark" else "Light",
                    color = LuxIvory,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = if (isDarkThemeGlobal) "Deep space — recommended at night" else "High contrast — bright environments",
                    color = LuxMuted,
                    fontSize = 11.sp
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
                modifier = Modifier.scale(0.9f)
            )
        }
    }
}

@Composable
private fun WidgetsEditor(viewModel: FinanceViewModel) {
    val cfg by viewModel.dashboardConfig.collectAsState()
    InfoTip(text = "Toggle widgets on or off. Monetary Matrix is always visible.")
    Spacer(Modifier.height(4.dp))

    // MonetaryMatrix locked row
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
            Icon(
                Icons.Default.Lock,
                contentDescription = null,
                tint = LuxGoldChange,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(com.example.data.WidgetId.MonetaryMatrix.label, color = LuxIvory, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Text(com.example.data.WidgetId.MonetaryMatrix.description, color = LuxMuted, fontSize = 11.sp)
            }
            Text("Always on", color = LuxGoldChange, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    }
    Spacer(Modifier.height(6.dp))

    com.example.data.WidgetId.values().filter { it != com.example.data.WidgetId.MonetaryMatrix }.forEach { w ->
        val visible = cfg.isVisible(w)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = !visible || cfg.hasAnyVisible) {
                    // Only block toggling-off the last visible widget.
                    if (visible && WidgetId_visibleCount(cfg) == 1) return@clickable
                    viewModel.setWidgetVisibility(w, !visible)
                },
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
                    Text(w.label, color = LuxIvory, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Text(w.description, color = LuxMuted, fontSize = 11.sp, lineHeight = 14.sp)
                }
                Switch(
                    checked = visible,
                    onCheckedChange = { newVisible ->
                        if (!newVisible && WidgetId_visibleCount(cfg) == 1) return@Switch
                        viewModel.setWidgetVisibility(w, newVisible)
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = LuxGoldChange,
                        checkedTrackColor = LuxGoldLight,
                        uncheckedThumbColor = LuxMuted,
                        uncheckedTrackColor = LuxCardGray
                    )
                )
            }
        }
        Spacer(Modifier.height(6.dp))
    }
}

private fun WidgetId_visibleCount(cfg: com.example.data.DashboardConfig): Int =
    com.example.data.WidgetId.values()
        .filter { it != com.example.data.WidgetId.MonetaryMatrix }
        .count { cfg.isVisible(it) }

@Composable
private fun ShakeToAddEditor(viewModel: FinanceViewModel, profile: com.example.data.UserProfile?) {
    val enabled = profile?.shakeToAddEnabled ?: false
    InfoTip(text = "Shake your phone to instantly open the Quick Add transaction dialog. Requires motion sensor. Default: off.")
    Spacer(Modifier.height(12.dp))
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = LuxCardGray.copy(alpha = 0.4f)),
        border = androidx.compose.foundation.BorderStroke(1.dp, if (enabled) LuxGoldChange.copy(alpha = 0.5f) else LuxCardGray),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(40.dp)
                    .background(LuxGoldChange.copy(alpha = 0.12f), androidx.compose.foundation.shape.RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.PhoneAndroid, contentDescription = null, tint = LuxGoldChange, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Shake to add", color = LuxIvory, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Text(
                    text = if (enabled) "Shake detected → Quick Add opens instantly" else "Disabled — tap to enable",
                    color = LuxMuted, fontSize = 11.sp
                )
            }
            Switch(
                checked = enabled,
                onCheckedChange = { viewModel.setShakeToAdd(it) },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = LuxGoldChange,
                    checkedTrackColor = LuxGoldLight,
                    uncheckedThumbColor = LuxMuted,
                    uncheckedTrackColor = LuxCardGray
                ),
                modifier = Modifier.scale(0.9f)
            )
        }
    }
    if (enabled) {
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth()
                .background(LuxGoldChange.copy(alpha = 0.08f), androidx.compose.foundation.shape.RoundedCornerShape(10.dp))
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = LuxGoldChange, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(8.dp))
            Text("Active — shake threshold: ~2.5g, cooldown: 1.2 seconds", color = LuxGoldChange, fontSize = 11.sp)
        }
    }
}

@Composable
private fun ResetSetupConfirm(onCancel: () -> Unit, onConfirm: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = LuxCardGray.copy(alpha = 0.4f)),
        border = BorderStroke(1.dp, LuxGoldChange.copy(alpha = 0.4f)),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Refresh, contentDescription = null, tint = LuxGoldChange)
                Spacer(Modifier.width(10.dp))
                Text(
                    text = "Re-run the setup wizard?",
                    color = LuxIvory,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp
                )
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = "Your accounts, transactions, bills and goals stay intact. You'll walk through the 6 onboarding steps again to fine-tune your profile.",
                color = LuxMuted,
                fontSize = 12.sp,
                lineHeight = 16.sp
            )
        }
    }
    Spacer(Modifier.height(4.dp))
    Button(
        onClick = onConfirm,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("sidebar_reset_button"),
        colors = ButtonDefaults.buttonColors(containerColor = LuxGoldChange, contentColor = LuxBlack),
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(text = "Start over", fontWeight = FontWeight.Bold, fontSize = 13.sp)
    }
    OutlinedButton(
        onClick = onCancel,
        modifier = Modifier.fillMaxWidth(),
        border = BorderStroke(1.dp, LuxCardGray),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = LuxMuted),
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(text = "Cancel", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
    }
}

@Composable
private fun LogoutConfirm(onCancel: () -> Unit, onConfirm: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = LuxCardGray.copy(alpha = 0.4f)),
        border = BorderStroke(1.dp, LuxError.copy(alpha = 0.4f)),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.ExitToApp, contentDescription = null, tint = LuxError)
                Spacer(Modifier.width(10.dp))
                Text(
                    text = "Sign out of Auren Money?",
                    color = LuxIvory,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp
                )
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = "Your finance data stays on this device. You'll need to authenticate again to view or edit it.",
                color = LuxMuted,
                fontSize = 12.sp,
                lineHeight = 16.sp
            )
        }
    }
    Spacer(Modifier.height(4.dp))
    OutlinedButton(
        onClick = onConfirm,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("sidebar_logout_button"),
        border = BorderStroke(1.2.dp, LuxError),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = LuxError),
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(text = "Sign out", fontWeight = FontWeight.Bold, fontSize = 13.sp)
    }
    Button(
        onClick = onCancel,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(containerColor = LuxGoldChange, contentColor = LuxBlack),
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(text = "Stay signed in", fontWeight = FontWeight.Bold, fontSize = 13.sp)
    }
}

@Composable
private fun InfoTip(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(LuxCardGray.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(Icons.Default.Info, contentDescription = null, tint = LuxMuted, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(8.dp))
        Text(
            text = text,
            color = LuxMuted,
            fontSize = 11.sp,
            lineHeight = 16.sp
        )
    }
}
