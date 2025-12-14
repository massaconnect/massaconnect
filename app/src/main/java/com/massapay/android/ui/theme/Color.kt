package com.massapay.android.ui.theme

import androidx.compose.ui.graphics.Color

// ===========================================
// DARK THEME - Pure black with subtle grays
// ===========================================
val DarkBackground = Color(0xFF000000)          // Pure black
val DarkSurface = Color(0xFF0A0A0A)             // Slightly lighter black for cards
val DarkCardBackground = Color(0xFF121212)      // Card background
val DarkButtonBackground = Color(0xFF1A1A1A)    // Button gray, barely visible
val DarkButtonSecondary = Color(0xFF252525)     // Secondary button
val DarkTextPrimary = Color(0xFFFFFFFF)         // Pure white text
val DarkTextSecondary = Color(0xFFB0B0B0)       // Gray text
val DarkBorder = Color(0xFF2A2A2A)              // Subtle borders
val DarkDivider = Color(0xFF1F1F1F)             // Dividers

// ===========================================
// LIGHT THEME - Pure white with black accents
// ===========================================
val LightBackground = Color(0xFFFFFFFF)         // Pure white
val LightSurface = Color(0xFFFAFAFA)            // Slightly off-white for cards
val LightCardBackground = Color(0xFFF5F5F5)     // Light gray cards for contrast
val LightButtonBackground = Color(0xFF000000)   // Pure black buttons
val LightButtonSecondary = Color(0xFF1A1A1A)    // Secondary button
val LightTextPrimary = Color(0xFF000000)        // Pure black text
val LightTextSecondary = Color(0xFF666666)      // Dark gray text
val LightBorder = Color(0xFFE0E0E0)             // Light gray borders
val LightDivider = Color(0xFFEEEEEE)            // Dividers
val LightIconButtonBg = Color(0xFFEEEEEE)       // Light gray for icon buttons
val LightIconButtonTint = Color(0xFF333333)     // Dark gray icon tint

// ===========================================
// ACCENT COLORS - For special highlights only
// ===========================================
val AccentPurple = Color(0xFF7B3FE4)            // Web3 purple
val AccentCyan = Color(0xFF00D4FF)              // Cyan highlight
val AccentGreen = Color(0xFF00C853)             // Success green
val AccentRed = Color(0xFFFF3B30)               // Error/Send red
val AccentOrange = Color(0xFFFF9500)            // Swap orange
val AccentBlue = Color(0xFF007AFF)              // Primary blue
val AccentYellow = Color(0xFFFFCC00)            // Warning yellow

// Legacy colors (keeping for compatibility)
val CardBackground = DarkCardBackground
val IconColor = DarkTextPrimary
val TextColor = DarkTextPrimary
val Primary = AccentBlue
val Background = DarkBackground
val Surface = DarkSurface
val Error = AccentRed