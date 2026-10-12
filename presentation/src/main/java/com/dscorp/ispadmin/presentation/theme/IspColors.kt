package com.dscorp.ispadmin.presentation.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** Design-system colors that Material's ColorScheme has no role for (hero band, status, charts). */
@Immutable
data class IspColors(
    val hero: Color,
    val onHero: Color,
    val onHeroMuted: Color,
    val heroPositive: Color,
    val heroNegative: Color,
    val heroWarning: Color,
    val selectedContainer: Color,
    val onSelectedContainer: Color,
    val tabSelected: Color,
    val onTabSelected: Color,
    val card: Color,
    val cardBorder: Color,
    val track: Color,
    val accent: Color,
    val accentInk: Color,
    val success: Color,
    val successContainer: Color,
    val warning: Color,
    val warningContainer: Color,
    val info: Color,
    val infoContainer: Color,
    val chartPrimary: Color,
    val chartCollected: Color,
    val chartCategorical: List<Color>,
    val chartOther: Color,
    val chartGrid: Color,
)

val LightIspColors = IspColors(
    // Matches the shared top app bar (primaryContainer) so the band reads as its continuation.
    hero = Color(0xFF1E2A6D),
    onHero = Color.White,
    onHeroMuted = Color(0xFFB6BFE8),
    heroPositive = Color(0xFF7FE0A6),
    heroNegative = Color(0xFFFFB4AB),
    heroWarning = Color(0xFFF5C04A),
    selectedContainer = Color(0xFFDFE3FF),
    onSelectedContainer = Color(0xFF141D48),
    tabSelected = Color.White,
    onTabSelected = Color(0xFF141D48),
    card = Color.White,
    cardBorder = Color.Transparent,
    track = Color(0xFFEAEDF5),
    accent = Color(0xFFFF5722),
    accentInk = Color(0xFFB23A0E),
    success = Color(0xFF17663D),
    successContainer = Color(0xFFD6F2E2),
    warning = Color(0xFF7A5000),
    warningContainer = Color(0xFFFFE8B8),
    info = Color(0xFF2459C9),
    infoContainer = Color(0xFFDCE6FF),
    chartPrimary = Color(0xFF141D48),
    chartCollected = Color(0xFF00796B),
    chartCategorical = listOf(
        Color(0xFF2F4AD0), Color(0xFFE8541C), Color(0xFF00897B), Color(0xFFA87800), Color(0xFFB0368F),
    ),
    chartOther = Color(0xFF9AA0B4),
    chartGrid = Color(0xFFE3E7F0),
)

val DarkIspColors = IspColors(
    hero = Color(0xFF232D6B),
    onHero = Color(0xFFE4E6F2),
    onHeroMuted = Color(0xFFAEB6DD),
    heroPositive = Color(0xFF7FE0A6),
    heroNegative = Color(0xFFFFB4AB),
    heroWarning = Color(0xFFF5C04A),
    // Light chip on the navy band; a darker tint would disappear against the hero color.
    selectedContainer = Color(0xFFB9C3FF),
    onSelectedContainer = Color(0xFF0E1640),
    tabSelected = Color(0xFFB9C3FF),
    onTabSelected = Color(0xFF0E1640),
    card = Color(0xFF161B33),
    cardBorder = Color(0xFF262C4A),
    track = Color(0xFF222846),
    accent = Color(0xFFFF7A4D),
    accentInk = Color(0xFFFF8A5F),
    success = Color(0xFF5FD08E),
    successContainer = Color(0xFF0F3D26),
    warning = Color(0xFFF5C04A),
    warningContainer = Color(0xFF4A3400),
    info = Color(0xFFA9C1FF),
    infoContainer = Color(0xFF1B2E66),
    chartPrimary = Color(0xFFB9C3FF),
    chartCollected = Color(0xFF5FD4C2),
    chartCategorical = listOf(
        Color(0xFF6B82F5), Color(0xFFE2602B), Color(0xFF1FA898), Color(0xFFB88A10), Color(0xFFCC5FAE),
    ),
    chartOther = Color(0xFF5B6283),
    chartGrid = Color(0xFF262C4A),
)

val LocalIspColors = staticCompositionLocalOf { LightIspColors }
