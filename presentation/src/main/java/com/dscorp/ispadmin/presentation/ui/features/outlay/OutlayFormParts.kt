package com.dscorp.ispadmin.presentation.ui.features.outlay

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.dscorp.ispadmin.presentation.theme.IspShapes
import com.dscorp.ispadmin.presentation.theme.IspTheme
import com.dscorp.ispadmin.presentation.theme.IspType
import com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.hub.formatSoles
import kotlinx.coroutines.delay

private const val SNACKBAR_MILLIS = 4_000L

/** Chip of the outlay screens (design system «Elegir»): 32 dp high, ✓ and lavender when chosen. */
@Composable
internal fun OutlayChoiceChip(label: String, selected: Boolean, enabled: Boolean = true, onClick: () -> Unit) {
    val colors = IspTheme.colors
    Row(
        Modifier.height(32.dp)
            .clip(IspShapes.badge)
            .then(if (selected) Modifier.background(colors.selectedContainer) else Modifier.border(1.dp, MaterialTheme.colorScheme.outline, IspShapes.badge))
            .clickable(enabled = enabled, role = Role.RadioButton, onClick = onClick)
            .padding(start = if (selected) 8.dp else 12.dp, end = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (selected) {
            Icon(Icons.Rounded.Check, contentDescription = null, tint = colors.onSelectedContainer, modifier = Modifier.size(16.dp))
            Spacer(Modifier.size(4.dp))
        }
        Text(
            label,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            color = if (selected) colors.onSelectedContainer else MaterialTheme.colorScheme.onSurface,
        )
    }
}

/** Amount of the outlay: label above, 34 sp centred figure over a 2 dp line, error below in red. */
@Composable
internal fun AmountField(value: String, onValueChange: (String) -> Unit, error: String?, enabled: Boolean, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    val line = if (error != null) scheme.error else scheme.primary
    Column(modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("Monto *", style = IspType.label, color = if (error != null) scheme.error else scheme.onSurfaceVariant)
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            enabled = enabled,
            singleLine = true,
            textStyle = IspType.kpiHero.copy(color = if (error != null) scheme.error else scheme.onSurface, textAlign = TextAlign.Center),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            cursorBrush = SolidColor(line),
            visualTransformation = SolesPrefix,
            modifier = Modifier.fillMaxWidth().height(64.dp).drawBehind {
                drawLine(line, Offset(0f, size.height), Offset(size.width, size.height), strokeWidth = 2.dp.toPx())
            },
            decorationBox = { field ->
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    if (value.isEmpty()) Text("S/ 0.00", style = IspType.kpiHero, color = scheme.onSurfaceVariant.copy(alpha = 0.4f))
                    field()
                }
            },
        )
        error?.let { Text(it, style = IspType.rowLabel, color = scheme.error, textAlign = TextAlign.Center) }
    }
}

private object SolesPrefix : VisualTransformation {
    private const val PREFIX = "S/ "

    override fun filter(text: AnnotatedString): TransformedText {
        if (text.isEmpty()) return TransformedText(text, OffsetMapping.Identity)
        return TransformedText(
            AnnotatedString(PREFIX) + text,
            object : OffsetMapping {
                override fun originalToTransformed(offset: Int) = offset + PREFIX.length
                override fun transformedToOriginal(offset: Int) = (offset - PREFIX.length).coerceAtLeast(0)
            },
        )
    }
}

/** Receipt of a variable outlay: a dashed drop zone with the two ways to attach it, or the attached photo. */
@Composable
internal fun ReceiptPhotoZone(
    photo: Uri?,
    enabled: Boolean,
    onTakePhoto: () -> Unit,
    onPickFromGallery: () -> Unit,
    onOpen: (Uri) -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    if (photo != null) {
        Row(
            modifier.fillMaxWidth().border(1.dp, IspTheme.colors.cardBorder, IspShapes.field).background(IspTheme.colors.card, IspShapes.field).padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AsyncImage(
                model = photo,
                contentDescription = "Comprobante",
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(48.dp).clip(IspShapes.badge).clickable { onOpen(photo) },
            )
            Column(Modifier.weight(1f).clickable { onOpen(photo) }) {
                Text("Comprobante adjunto", style = IspType.cardTitle, color = scheme.onSurface)
                Text("Toca para verlo", style = IspType.rowLabel, color = scheme.onSurfaceVariant)
            }
            IconButton(onClick = onRemove, enabled = enabled, modifier = Modifier.size(48.dp)) {
                Icon(Icons.Rounded.Close, contentDescription = "Quitar comprobante", tint = scheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
            }
        }
        return
    }
    Column(
        modifier.fillMaxWidth().dashedBorder(scheme.outline, 12.dp).padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(Icons.Outlined.PhotoCamera, contentDescription = null, tint = scheme.onSurfaceVariant, modifier = Modifier.size(28.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PillButton("Tomar foto", enabled, onTakePhoto, container = IspTheme.colors.selectedContainer, content = IspTheme.colors.onSelectedContainer)
            PillButton("Galería", enabled, onPickFromGallery, container = null, content = scheme.primary)
        }
    }
}

@Composable
private fun PillButton(label: String, enabled: Boolean, onClick: () -> Unit, container: Color?, content: Color) {
    Box(
        Modifier.height(48.dp).clip(IspShapes.pill)
            .then(if (container != null) Modifier.background(container) else Modifier.border(1.dp, MaterialTheme.colorScheme.outline, IspShapes.pill))
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = content, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
    }
}

private fun Modifier.dashedBorder(color: Color, radius: Dp, width: Dp = 1.5.dp) = drawBehind {
    drawRoundRect(
        color = color,
        cornerRadius = CornerRadius(radius.toPx()),
        style = Stroke(width = width.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f))),
    )
}

/** Result of an action (design system «Feedback»): a snackbar that closes itself. */
@Composable
internal fun OutlaySnackbar(
    message: AnnotatedString,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: () -> Unit = {},
) {
    val scheme = MaterialTheme.colorScheme
    LaunchedEffect(message) {
        delay(SNACKBAR_MILLIS)
        onDismiss()
    }
    Row(
        modifier.fillMaxWidth().widthIn(max = 600.dp)
            .background(scheme.inverseSurface, RoundedCornerShape(12.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(message, modifier = Modifier.weight(1f), color = scheme.inverseOnSurface, fontSize = 14.sp, lineHeight = 20.sp)
        actionLabel?.let {
            Text(
                it,
                modifier = Modifier.clickable(role = Role.Button, onClick = onAction).padding(vertical = 4.dp),
                color = scheme.inversePrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

/** «Egreso registrado.» and, when the Core alerted the team, in bold that it did and why. */
@Composable
internal fun outlaySavedMessage(teamNotified: Boolean, threshold: Double?): AnnotatedString {
    val accent = MaterialTheme.colorScheme.inversePrimary
    return buildAnnotatedString {
        append("Egreso registrado.")
        if (teamNotified) {
            append(" ")
            withStyle(SpanStyle(fontWeight = FontWeight.SemiBold, color = accent)) { append("Se avisó al equipo") }
            if (threshold != null) append(" porque el mes pasó ${formatSoles(threshold)}.")
        }
    }
}
