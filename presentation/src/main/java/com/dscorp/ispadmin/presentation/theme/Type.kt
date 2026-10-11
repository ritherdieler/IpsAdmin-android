package com.dscorp.ispadmin.presentation.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.dscorp.ispadmin.R

@OptIn(ExperimentalTextApi::class)
private fun robotoFlex(weight: Int) = Font(
    resId = R.font.robotoflex,
    weight = FontWeight(weight),
    variationSettings = FontVariation.Settings(FontVariation.weight(weight))
)

/** Roboto Flex is a variable font; each weight maps to its own variation instance. */
val RobotoFlex = FontFamily(robotoFlex(400), robotoFlex(500), robotoFlex(600), robotoFlex(700))

val myTypography = Typography(
    displayLarge = TextStyle(
        fontWeight = FontWeight.W400,
        fontSize = 57.sp,
        letterSpacing = (-0.25).sp,
        lineHeight = 64.sp,
        fontFamily = RobotoFlex
    ),
    displayMedium = TextStyle(
        fontWeight = FontWeight.W400,
        fontSize = 45.sp,
        letterSpacing = 0.sp,
        lineHeight = 52.sp,
        fontFamily = RobotoFlex

    ),
    displaySmall = TextStyle(
        fontWeight = FontWeight.W400,
        fontSize = 36.sp,
        letterSpacing = 0.sp,
        lineHeight = 44.sp,
        fontFamily = RobotoFlex

    ),
    headlineLarge = TextStyle(
        fontWeight = FontWeight.W400,
        fontSize = 32.sp,
        letterSpacing = 0.sp,
        lineHeight = 40.sp,
        fontFamily = RobotoFlex

    ),
    headlineMedium = TextStyle(
        fontWeight = FontWeight.W400,
        fontSize = 28.sp,
        letterSpacing = 0.sp,
        lineHeight = 36.sp,
        fontFamily = RobotoFlex

    ),
    headlineSmall = TextStyle(
        fontWeight = FontWeight.W400,
        fontSize = 24.sp,
        letterSpacing = 0.sp,
        lineHeight = 32.sp,
        fontFamily = RobotoFlex

    ),
    titleLarge = TextStyle(
        fontWeight = FontWeight.W400,
        fontSize = 22.sp,
        letterSpacing = 0.sp,
        lineHeight = 28.sp,
        fontFamily = RobotoFlex

    ),
    titleMedium = TextStyle(
        fontWeight = FontWeight.W500,
        fontSize = 16.sp,
        letterSpacing = 0.15.sp,
        lineHeight = 24.sp,
        fontFamily = RobotoFlex

    ),
    titleSmall = TextStyle(
        fontWeight = FontWeight.W500,
        fontSize = 14.sp,
        letterSpacing = 0.1.sp,
        lineHeight = 20.sp,
        fontFamily = RobotoFlex

    ),
    labelLarge = TextStyle(
        fontWeight = FontWeight.W500,
        fontSize = 14.sp,
        letterSpacing = 0.1.sp,
        lineHeight = 20.sp,
        fontFamily = RobotoFlex

    ),
    labelMedium = TextStyle(
        fontWeight = FontWeight.W500,
        fontSize = 12.sp,
        letterSpacing = 0.5.sp,
        lineHeight = 16.sp,
        fontFamily = RobotoFlex

    ),
    labelSmall = TextStyle(
        fontWeight = FontWeight.W500,
        fontSize = 11.sp,
        letterSpacing = 0.5.sp,
        lineHeight = 16.sp,
        fontFamily = RobotoFlex

    ),
    bodyLarge = TextStyle(
        fontWeight = FontWeight.W400,
        fontSize = 16.sp,
        letterSpacing = 0.5.sp,
        lineHeight = 24.sp,
        fontFamily = RobotoFlex

    ),
    bodyMedium = TextStyle(
        fontWeight = FontWeight.W400,
        fontSize = 14.sp,
        letterSpacing = 0.25.sp,
        lineHeight = 20.sp,
        fontFamily = RobotoFlex

    ),
    bodySmall = TextStyle(
        fontWeight = FontWeight.W400,
        fontSize = 12.sp,
        letterSpacing = 0.4.sp,
        lineHeight = 16.sp,
        fontFamily = RobotoFlex

    )
)
