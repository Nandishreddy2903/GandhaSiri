package com.gandhasiri.app.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = Sandalwood,
    onPrimary = PureWhite,
    secondary = LightGold,
    onSecondary = DarkWood,
    tertiary = MutedSandalwood,
    onTertiary = DarkWood,
    background = DarkWood,
    surface = DarkWood,
    onBackground = PureWhite,
    onSurface = PureWhite,
    error = PanicRed,
    outline = PaleWood
)

private val LightColorScheme = lightColorScheme(
    primary = Sandalwood,
    onPrimary = PureWhite,
    secondary = DarkSandalwood,
    onSecondary = PureWhite,
    tertiary = LightGold,
    onTertiary = DarkWood,
    background = WarmCream,
    surface = PureWhite,
    onBackground = NearBlackBrown,
    onSurface = NearBlackBrown,
    onSurfaceVariant = DarkWood,
    surfaceVariant = PureWhite,
    error = PanicRed,
    outline = LightWood
)

@Composable
fun GandhaSiriTheme(
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
            window.statusBarColor = DarkWood.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
