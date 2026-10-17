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
        fontSize = 36.sp,
        lineHeight = 42.sp,
        letterSpacing = (-0.5).sp,
        fontFeatureSettings = TABULAR_NUMBERS,
    )
    val kpi = TextStyle(
        fontFamily = RobotoFlex,
        fontWeight = FontWeight(650),
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
    /** Uppercase label of figures (draw with `Overline`, which uppercases the text). */
    val overline = TextStyle(
        fontFamily = RobotoFlex,
        fontWeight = FontWeight.W600,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.8.sp,
    )
    /** Screen title in the hero band ("Dashboard"). */
    val screenTitle = TextStyle(fontFamily = RobotoFlex, fontWeight = FontWeight.W600, fontSize = 22.sp, lineHeight = 28.sp)
    /** Card titles: 15sp semibold. */
    val cardTitle = TextStyle(fontFamily = RobotoFlex, fontWeight = FontWeight.W600, fontSize = 15.sp, lineHeight = 22.sp)
    /** Tabs, chips and links: 12sp semibold. */
    val label = TextStyle(fontFamily = RobotoFlex, fontWeight = FontWeight.W600, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.3.sp)
    /** Change next to the hero figure. */
    val heroDelta = TextStyle(
        fontFamily = RobotoFlex,
        fontWeight = FontWeight.W600,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        fontFeatureSettings = TABULAR_NUMBERS,
    )
    /** Figures inside list rows of a card (Cobranza operativa). */
    val rowValue = TextStyle(
        fontFamily = RobotoFlex,
        fontWeight = FontWeight(650),
        fontSize = 18.sp,
        lineHeight = 24.sp,
        fontFeatureSettings = TABULAR_NUMBERS,
    )
    /** Labels of list rows inside a card. */
    val rowLabel = TextStyle(fontFamily = RobotoFlex, fontWeight = FontWeight.W400, fontSize = 13.sp, lineHeight = 18.sp)
    val number = TextStyle(
        fontFamily = RobotoFlex,
        fontWeight = FontWeight.W600,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        fontFeatureSettings = TABULAR_NUMBERS,
    )
}
