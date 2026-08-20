package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme =
  darkColorScheme(
    primary = ElectricGold,
    onPrimary = StadiumNightDark,
    primaryContainer = TurfAccent,
    onPrimaryContainer = Color.White,
    secondary = ElectricCyan,
    onSecondary = StadiumNightDark,
    secondaryContainer = CyberBlue,
    onSecondaryContainer = Color.White,
    tertiary = FlameOrange,
    onTertiary = Color.White,
    background = StadiumNightDark,
    onBackground = TextPrimaryLight,
    surface = StadiumCardBg,
    onSurface = TextPrimaryLight,
    surfaceVariant = StadiumCardLight,
    onSurfaceVariant = TextSecondaryLight
  )

private val LightColorScheme =
  lightColorScheme(
    primary = PitchGreen,
    onPrimary = Color.White,
    primaryContainer = PitchLightGreen,
    onPrimaryContainer = Color.White,
    secondary = CyberBlue,
    onSecondary = Color.White,
    tertiary = FlameOrange,
    onTertiary = Color.White,
    background = Color(0xFFF1F5F9),
    onBackground = Color(0xFF0F172A),
    surface = Color.White,
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFE2E8F0),
    onSurfaceVariant = Color(0xFF475569)
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = true, // Default to stadium night dark theme for sports arcade immersion
  dynamicColor: Boolean = false, // Use our handcrafted sporty color scheme
  content: @Composable () -> Unit,
) {
  val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
