package com.massapay.android.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// ===========================================
// DARK COLOR SCHEME - Pure black, minimal grays
// ===========================================
private val DarkColorScheme = darkColorScheme(
    primary = AccentBlue,
    onPrimary = Color.White,
    primaryContainer = DarkButtonBackground,
    onPrimaryContainer = Color.White,
    
    secondary = AccentPurple,
    onSecondary = Color.White,
    secondaryContainer = DarkButtonSecondary,
    onSecondaryContainer = Color.White,
    
    tertiary = AccentCyan,
    onTertiary = Color.Black,
    
    background = DarkBackground,
    onBackground = DarkTextPrimary,
    
    surface = DarkSurface,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkCardBackground,
    onSurfaceVariant = DarkTextSecondary,
    
    outline = DarkBorder,
    outlineVariant = DarkDivider,
    
    error = AccentRed,
    onError = Color.White,
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color.White
)

// ===========================================
// LIGHT COLOR SCHEME - Pure white, black buttons
// ===========================================
private val LightColorScheme = lightColorScheme(
    primary = LightButtonBackground,  // Black buttons
    onPrimary = Color.White,
    primaryContainer = LightIconButtonBg,       // Light gray for icon button backgrounds
    onPrimaryContainer = LightIconButtonTint,   // Dark gray icons
    
    secondary = AccentPurple,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE8DEF8),
    onSecondaryContainer = Color(0xFF1D192B),
    
    tertiary = AccentBlue,
    onTertiary = Color.White,
    
    background = LightBackground,
    onBackground = LightTextPrimary,
    
    surface = LightSurface,
    onSurface = LightTextPrimary,
    surfaceVariant = LightCardBackground,
    onSurfaceVariant = LightTextSecondary,
    
    outline = LightBorder,
    outlineVariant = LightDivider,
    
    error = AccentRed,
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002)
)

@Composable
fun MassaPayTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}