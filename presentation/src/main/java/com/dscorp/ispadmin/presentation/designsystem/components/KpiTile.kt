package com.dscorp.ispadmin.presentation.designsystem.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dscorp.ispadmin.presentation.theme.IspShapes
import com.dscorp.ispadmin.presentation.theme.IspSpacing
import com.dscorp.ispadmin.presentation.theme.IspTheme
import com.dscorp.ispadmin.presentation.theme.IspType
import com.dscorp.ispadmin.presentation.theme.MyTheme

/** Whether a change is good, bad or neutral for the business; decides color, not arrow direction. */
enum class DeltaTone { Positive, Negative, Neutral }

/** "▲ 6.1%" plus the comparison period, e.g. "vs sep". */
data class KpiDelta(val text: String, val tone: DeltaTone, val comparison: String? = null)

data class KpiModel(
    val label: String,
    val value: String,
    val tag: String,
    val delta: KpiDelta? = null,
    val supporting: String? = null,
    val info: String? = null,
    val progress: Float? = null,
    /** Short qualifier shown as an info badge under the value, e.g. "⇄ Plan superior: 5". */
    val badge: String? = null,
)

/** One business figure: label, value, optional delta, progress and an expandable explanation. */
@Composable
fun KpiTile(model: KpiModel, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null) {
    val colors = IspTheme.colors
    val border = colors.cardBorder.takeIf { it != Color.Transparent }?.let { BorderStroke(1.dp, it) }
    val content: @Composable () -> Unit = { KpiTileContent(model) }
    val tileModifier = modifier.fillMaxWidth().testTag(model.tag)
    if (onClick == null) {
        Surface(tileModifier, shape = IspShapes.card, color = colors.card, border = border, shadowElevation = 1.dp, content = content)
    } else {
        Surface(onClick, tileModifier, shape = IspShapes.card, color = colors.card, border = border, shadowElevation = 1.dp, content = content)
    }
}

@Composable
private fun KpiTileContent(model: KpiModel) {
    var showInfo by rememberSaveable(model.tag) { mutableStateOf(false) }
    Column(Modifier.padding(IspSpacing.lg), verticalArrangement = Arrangement.spacedBy(IspSpacing.xs)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Overline(model.label, MaterialTheme.colorScheme.onSurfaceVariant, Modifier.weight(1f))
            if (model.info != null) {
                IconButton(onClick = { showInfo = !showInfo }, modifier = Modifier.size(28.dp).testTag("${model.tag}_info")) {
                    Icon(Icons.Outlined.Info, contentDescription = "Qué es ${model.label.lowercase()}",
                        Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        Text(model.value, Modifier.testTag("${model.tag}_value"), style = IspType.kpi, color = MaterialTheme.colorScheme.onSurface)
        model.delta?.let { DeltaText(it) }
        model.badge?.let { StatusBadge(it, BadgeTone.Info) }
        model.progress?.let { ProgressTrack(it, IspTheme.colors.chartCollected, Modifier.padding(top = IspSpacing.xs)) }
        model.supporting?.let {
            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        AnimatedVisibility(showInfo && model.info != null) {
            Surface(Modifier.padding(top = IspSpacing.xs), shape = IspShapes.badge, color = IspTheme.colors.track) {
                Text(model.info.orEmpty(), Modifier.padding(IspSpacing.sm), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
fun DeltaText(delta: KpiDelta, modifier: Modifier = Modifier, onHero: Boolean = false, style: androidx.compose.ui.text.TextStyle = IspType.kpiDelta) {
    val colors = IspTheme.colors
    val color = when (delta.tone) {
        DeltaTone.Positive -> if (onHero) colors.heroPositive else colors.success
        DeltaTone.Negative -> if (onHero) colors.heroNegative else MaterialTheme.colorScheme.error
        DeltaTone.Neutral -> if (onHero) colors.onHeroMuted else MaterialTheme.colorScheme.onSurfaceVariant
    }
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(IspSpacing.xs)) {
        Text(delta.text, style = style, color = color)
        delta.comparison?.let {
            Text(it, style = MaterialTheme.typography.bodySmall,
                color = if (onHero) colors.onHeroMuted else MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Preview(widthDp = 360)
@Composable
private fun KpiTilePreview() {
    MyTheme(darkTheme = false) {
        Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            KpiTile(KpiModel("Churn mensual", "1.3%", "churn", KpiDelta("▼ 0.3 pp", DeltaTone.Positive, "vs sep"),
                info = "Cancelaciones ÷ activos al inicio del mes."), Modifier.weight(1f))
            KpiTile(KpiModel("Efectividad", "87.4%", "eff", progress = 0.874f), Modifier.weight(1f))
        }
    }
}

@Preview(widthDp = 360)
@Composable
private fun KpiTileDarkPreview() {
    MyTheme(darkTheme = true) {
        Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            KpiTile(KpiModel("ARPU", "S/ 32.61", "arpu", KpiDelta("▲ S/ 0.48", DeltaTone.Positive, "vs sep")), Modifier.weight(1f))
            KpiTile(KpiModel("Reapertura", "6.2%", "reopen", KpiDelta("▲ 0.8 pp", DeltaTone.Negative, "vs sep")), Modifier.weight(1f))
        }
    }
}
