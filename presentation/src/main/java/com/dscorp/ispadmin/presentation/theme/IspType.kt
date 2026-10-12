package com.dscorp.ispadmin.presentation.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private const val TABULAR_NUMBERS = "tnum"

/** Dashboard text styles that the Material type scale does not provide. Figures use tabular numerals. */
object IspType {
    val kpiHero = TextStyle(
        fontFamily = RobotoFlex,
        fontWeight = FontWeight.W700,
        fontSize = 34.sp,
        lineHeight = 40.sp,
        letterSpacing = (-0.5).sp,
        fontFeatureSettings = TABULAR_NUMBERS,
    )
    val kpi = TextStyle(
        fontFamily = RobotoFlex,
        fontWeight = FontWeight.W600,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = (-0.2).sp,
        fontFeatureSettings = TABULAR_NUMBERS,
    )
    val kpiDelta = TextStyle(
        fontFamily = RobotoFlex,
        fontWeight = FontWeight.W600,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        fontFeatureSettings = TABULAR_NUMBERS,
    )
    /** Section headers between cards: 18sp semibold, lighter than the Material title scale. */
    val sectionTitle = TextStyle(
        fontFamily = RobotoFlex,
        fontWeight = FontWeight.W600,
        fontSize = 18.sp,
        lineHeight = 24.sp,
    )
    val overline = TextStyle(
        fontFamily = RobotoFlex,
        fontWeight = FontWeight.W600,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.2.sp,
    )
    val number = TextStyle(
        fontFamily = RobotoFlex,
        fontWeight = FontWeight.W600,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        fontFeatureSettings = TABULAR_NUMBERS,
    )
}
