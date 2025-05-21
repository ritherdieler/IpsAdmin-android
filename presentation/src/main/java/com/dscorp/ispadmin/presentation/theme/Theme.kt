package com.dscorp.ispadmin.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val lightColorScheme = ColorScheme(
    primary = Primary,
    onPrimary = OnPrimary,
    primaryContainer = PrimaryContainer,
    onPrimaryContainer = OnPrimaryContainer,
    inversePrimary = InversePrimary,
    secondary = Secondary,
    onSecondary = OnSecondary,
    secondaryContainer = SecondaryContainer,
    onSecondaryContainer = OnSecondaryContainer,
    tertiary = Tertiary,
    onTertiary = OnTertiary,
    tertiaryContainer = TertiaryContainer,
    onTertiaryContainer = OnTertiaryContainer,
    background = Background,
    onBackground = OnBackground,
    surface = Surface,
    onSurface = OnSurface,
    surfaceVariant = SurfaceVariant,
    onSurfaceVariant = OnSurfaceVariant,
    surfaceTint = SurfaceTint,
    inverseSurface = InverseSurface,
    inverseOnSurface = InverseOnSurface,
    error = Error,
    onError = OnError,
    errorContainer = ErrorContainer,
    onErrorContainer = OnErrorContainer,
    outline = Outline,
    outlineVariant = OutlineVariant,
    scrim = Scrim,
    surfaceBright = SurfaceBright,
    surfaceDim = SurfaceDim,
    surfaceContainer = SurfaceContainer,
    surfaceContainerHigh = SurfaceContainerHigh,
    surfaceContainerHighest = SurfaceContainerHighest,
    surfaceContainerLow = SurfaceContainerLow,
    surfaceContainerLowest = SurfaceContainerLowest,
)

val darkColorScheme = ColorScheme(
    primary = Color(0xFF3A4998),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF232D6B),
    onPrimaryContainer = Color.White,
    inversePrimary = Color(0xFF141D48),
    secondary = Color(0xFFFF7043),
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFFB23C1A),
    onSecondaryContainer = Color.White,
    tertiary = Color(0xFF48A999),
    onTertiary = Color.Black,
    tertiaryContainer = Color(0xFF00796B),
    onTertiaryContainer = Color.White,
    background = Color(0xFF121212),
    onBackground = Color.White,
    surface = Color(0xFF1E1E1E),
    onSurface = Color.White,
    surfaceVariant = Color(0xFF333333),
    onSurfaceVariant = Color.White,
    surfaceTint = Color(0xFF3A4998),
    inverseSurface = Color(0xFFECEFF1),
    inverseOnSurface = Color(0xFF121212),
    error = Color(0xFFE57373),
    onError = Color.Black,
    errorContainer = Color(0xFFB00020),
    onErrorContainer = Color.White,
    outline = Color(0xFF757575),
    outlineVariant = Color(0xFF5C5C5C),
    scrim = Color(0x80000000),
    surfaceBright = Color(0xFF3C3C3C),
    surfaceDim = Color(0xFF121212),
    surfaceContainer = Color(0xFF252525),
    surfaceContainerHigh = Color(0xFF333333),
    surfaceContainerHighest = Color(0xFF424242),
    surfaceContainerLow = Color(0xFF1E1E1E),
    surfaceContainerLowest = Color(0xFF151515),
)

@Composable
fun MyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) darkColorScheme else lightColorScheme
    
    MaterialTheme(
        colorScheme = colorScheme,
        typography = myTypography,
        content = content
    )
}
