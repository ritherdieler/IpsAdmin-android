package com.dscorp.ispadmin.presentation.designsystem.components

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import com.dscorp.ispadmin.presentation.theme.IspType
import java.util.Locale

private val SPANISH = Locale.forLanguageTag("es-PE")

/**
 * Uppercase, tracked label of a figure ("TICKETS ABIERTOS"). Screen readers get [spokenText] (the label in normal
 * case by default), so they do not spell the capitals out.
 */
@Composable
fun Overline(text: String, color: Color, modifier: Modifier = Modifier, maxLines: Int = 2, spokenText: String = text) {
    Text(
        text.uppercase(SPANISH),
        modifier.clearAndSetSemantics { this.text = AnnotatedString(spokenText) },
        style = IspType.overline,
        color = color,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
    )
}
