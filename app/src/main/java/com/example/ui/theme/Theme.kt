package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val GeometricColorScheme =
  lightColorScheme(
    primary = LuxGoldChange,         // GeoPrimary: #6750A4
    onPrimary = LuxBlack,            // GeoBackground: #FDF8FF
    primaryContainer = LuxGoldLight, // GeoPrimaryContainer: #EADDFF
    onPrimaryContainer = LuxIvory,   // GeoTextMain: #1C1B1F
    secondary = LuxMuted,            // GeoMutedText: #49454F
    onSecondary = LuxIvory,
    background = LuxBlack,           // GeoBackground: #FDF8FF
    onBackground = LuxIvory,         // GeoTextMain: #1C1B1F
    surface = LuxDarkGray,           // GeoSecondaryContainer: #F3EDF7
    onSurface = LuxIvory,            // GeoTextMain: #1C1B1F
    surfaceVariant = LuxCardGray,    // GeoCardGray: #FFFFFF
    onSurfaceVariant = LuxIvory,     // GeoTextMain: #1C1B1F
    error = LuxError,                // GeoError: #B3261E
    onError = LuxBlack
  )

@Composable
fun MyApplicationTheme(
  content: @Composable () -> Unit,
) {
  MaterialTheme(
    colorScheme = GeometricColorScheme,
    typography = Typography,
    content = content
  )
}
