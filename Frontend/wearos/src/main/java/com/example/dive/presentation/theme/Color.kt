package com.example.dive.presentation.theme

import androidx.compose.ui.graphics.Color
import androidx.wear.compose.material.Colors

val BackgroundPrimary = Color(0xFF121212)
val BackgroundSecondary = Color(0xFF404040)
val TextPrimary = Color(0xFFFFFFFF)
val TextSecondary = Color(0xFFCCCCCC)
val TextTertiary = Color(0xFFAAAAAA)
val AccentRed = Color(0xFFFF4444)
val AccentBlue = Color(0xFF4488FF)
val AccentYellow = Color(0xFFFFD700)
val AccentGreen = Color(0xFF44FF44)

internal val wearColorPalette: Colors = Colors(
    primary = AccentBlue,
    primaryVariant = AccentBlue,
    secondary = BackgroundSecondary,
    secondaryVariant = BackgroundSecondary,
    error = AccentRed,
    background = BackgroundPrimary,
    onPrimary = TextPrimary,
    onSecondary = TextPrimary,
    onError = TextPrimary,
    onBackground = TextPrimary
)