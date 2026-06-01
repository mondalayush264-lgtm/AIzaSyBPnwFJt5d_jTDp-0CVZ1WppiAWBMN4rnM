package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = BrandAccent,
    onPrimary = Color.Black,
    secondary = BrandAccent,
    onSecondary = Color.Black,
    tertiary = Color.White,
    background = DarkBackground,
    surface = DarkSurface,
    surfaceVariant = DarkSurfaceVariant,
    onBackground = Color.White,
    onSurface = Color.White,
    onSurfaceVariant = Color.LightGray
)

private val LightColorScheme = lightColorScheme(
    primary = BrandAccent,
    onPrimary = Color.Black,
    secondary = BrandAccent,
    onSecondary = Color.Black,
    tertiary = Color.Black,
    background = LightBackground,
    surface = Color.White,
    surfaceVariant = Color(0xFFF5F5F7),
    onBackground = Color.Black,
    onSurface = Color.Black,
    onSurfaceVariant = Color.DarkGray
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
