package com.dscorp.ispadmin.presentation.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.dscorp.ispadmin.presentation.theme.IspShapes
import com.dscorp.ispadmin.presentation.theme.IspTheme
import com.dscorp.ispadmin.presentation.theme.IspType

/** Context note inside a form: not a snackbar (no action) and not a dialog (does not interrupt). */
@Composable
fun InlineNote(text: String, tone: BadgeTone, modifier: Modifier = Modifier) = InlineNote(AnnotatedString(text), tone, modifier)

/** Same note with styled spans, e.g. the amount left in bold. */
@Composable
fun InlineNote(text: AnnotatedString, tone: BadgeTone, modifier: Modifier = Modifier) {
    val colors = IspTheme.colors
    val container = if (tone == BadgeTone.Warning) colors.warningContainer else colors.infoContainer
    val accent = if (tone == BadgeTone.Warning) colors.warning else colors.info
    Row(
        modifier.fillMaxWidth().background(container, IspShapes.field).padding(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(
            if (tone == BadgeTone.Warning) Icons.Rounded.Warning else Icons.Rounded.Info,
            contentDescription = null,
            tint = accent,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(10.dp))
        Text(text, style = IspType.rowLabel, color = MaterialTheme.colorScheme.onSurface)
    }
}
