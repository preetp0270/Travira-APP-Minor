package com.example.travira.ui.theme

import android.os.*
import androidx.compose.foundation.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.*

private val DarkColorScheme = darkColorScheme(
    primary = TraviraBlueLight,
    secondary = TraviraSky,
    tertiary = TraviraIce,
    background = TraviraBlueDark,
    surface = Color(0xFF102A43),
    onPrimary = Color.White,
    onSecondary = TraviraBlueDark,
    onBackground = Color.White,
    onSurface = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = TraviraBlue,
    secondary = TraviraBlueMid,
    tertiary = TraviraSky,
    background = TraviraBg,
    surface = TraviraWhite,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = TraviraTextPrimary,
    onSurface = TraviraTextPrimary
)

@Composable
fun TraviraTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Prefer brand blue over system dynamic colors so the app stays consistent
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

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
