package com.example.ui.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

// Global theme state for Geometric Balance theme mode
var isDarkThemeGlobal by mutableStateOf(false)
var langOption by mutableStateOf("English") // "English", "Hinglish", "Hindi"

// Geometric Balance Color Palette - Dynamically switches between light and dark versions of design language
val LuxBlack: Color
    get() = if (isDarkThemeGlobal) Color(0xFF0D0B12) else Color(0xFFFDF8FF)

val LuxDarkGray: Color
    get() = if (isDarkThemeGlobal) Color(0xFF1A1424) else Color(0xFFF3EDF7)

val LuxCardGray: Color
    get() = if (isDarkThemeGlobal) Color(0xFF241E30) else Color(0xFFFFFFFF)

val LuxGoldChange: Color
    get() = if (isDarkThemeGlobal) Color(0xFFD0BCFF) else Color(0xFF6750A4)

val LuxGoldLight: Color
    get() = if (isDarkThemeGlobal) Color(0xFF4F378B) else Color(0xFFEADDFF)

val LuxIvory: Color
    get() = if (isDarkThemeGlobal) Color(0xFFE6E1E5) else Color(0xFF1C1B1F)

val LuxMuted: Color
    get() = if (isDarkThemeGlobal) Color(0xFF938F99) else Color(0xFF49454F)

val LuxError: Color
    get() = if (isDarkThemeGlobal) Color(0xFFF2B8B5) else Color(0xFFB3261E)

val LuxGreen: Color
    get() = if (isDarkThemeGlobal) Color(0xFF81C784) else Color(0xFF2E7D32)

val LuxIceBlue: Color
    get() = if (isDarkThemeGlobal) Color(0xFF4FC3F7) else Color(0xFF00639B)

fun t(en: String, hin: String = "", hng: String = ""): String {
    return when (langOption) {
        "Hindi" -> if (hin.isNotBlank()) hin else en
        "Hinglish" -> if (hng.isNotBlank()) hng else en
        else -> en
    }
}

