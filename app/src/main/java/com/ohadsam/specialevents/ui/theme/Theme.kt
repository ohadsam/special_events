package com.ohadsam.specialevents.ui.theme

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

// Brand: deep indigo with a warm gold accent (celebration without being loud).
private val LightColors = lightColorScheme(
    primary = Color(0xFF4A3AB8),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE4DFFF),
    onPrimaryContainer = Color(0xFF1A0E66),
    secondary = Color(0xFF8A5A00),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFFFDEA8),
    onSecondaryContainer = Color(0xFF2B1800),
    tertiary = Color(0xFFB3265E),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFD9E3),
    onTertiaryContainer = Color(0xFF3F0020),
    background = Color(0xFFFCF9FF),
    onBackground = Color(0xFF1C1B20),
    surface = Color(0xFFFCF9FF),
    onSurface = Color(0xFF1C1B20),
    surfaceVariant = Color(0xFFE5E0EC),
    onSurfaceVariant = Color(0xFF47454F),
    outline = Color(0xFF78757F),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFC8BFFF),
    onPrimary = Color(0xFF2B1A8C),
    primaryContainer = Color(0xFF3F2FA0),
    onPrimaryContainer = Color(0xFFE4DFFF),
    secondary = Color(0xFFFFBA3E),
    onSecondary = Color(0xFF462B00),
    secondaryContainer = Color(0xFF654000),
    onSecondaryContainer = Color(0xFFFFDEA8),
    tertiary = Color(0xFFFFB1C8),
    onTertiary = Color(0xFF650033),
    tertiaryContainer = Color(0xFF8F0F46),
    onTertiaryContainer = Color(0xFFFFD9E3),
    background = Color(0xFF141318),
    onBackground = Color(0xFFE6E1E9),
    surface = Color(0xFF141318),
    onSurface = Color(0xFFE6E1E9),
    surfaceVariant = Color(0xFF47454F),
    onSurfaceVariant = Color(0xFFC9C4D0),
    outline = Color(0xFF928F99),
)

@Composable
fun SpecialEventsTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colors = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }
    MaterialTheme(colorScheme = colors, content = content)
}
