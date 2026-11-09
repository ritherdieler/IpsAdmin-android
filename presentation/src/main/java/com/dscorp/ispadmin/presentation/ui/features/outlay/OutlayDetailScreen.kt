package com.dscorp.ispadmin.presentation.ui.features.outlay

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dscorp.ispadmin.presentation.designsystem.components.BadgeTone
import com.dscorp.ispadmin.presentation.designsystem.components.InlineNote
import com.dscorp.ispadmin.presentation.designsystem.components.ScreenTopBar
import com.dscorp.ispadmin.presentation.designsystem.components.StatusBadge
import com.dscorp.ispadmin.presentation.theme.IspShapes
import com.dscorp.ispadmin.presentation.theme.IspTheme
import com.dscorp.ispadmin.presentation.theme.IspType

/** Egreso (a-caj-09): the amount in the hero band, the data of the outlay and, for a fixed one, why it has no receipt. */
@Composable
fun OutlayDetailScreen(detail: OutlayDetail, onBack: () -> Unit, onOpenReceipt: (List<String>, Int) -> Unit) {
    val colors = IspTheme.colors
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(Modifier.fillMaxWidth().background(colors.hero, IspShapes.heroBottom)) {
            ScreenTopBar("Egreso", Icons.AutoMirrored.Rounded.ArrowBack, "Atrás", onBack, contentColor = colors.onHero)
            Column(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(detail.description, style = IspType.label, color = colors.onHeroMuted)
                Text(detail.amount, style = IspType.kpiHero, color = colors.onHero)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    StatusBadge(if (detail.isFixed) "Fijo" else "Variable", if (detail.isFixed) BadgeTone.Info else BadgeTone.Neutral)
                    Text(detail.registeredAt, fontSize = 13.sp, lineHeight = 18.sp, color = colors.onHeroMuted)
                }
            }
        }
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(top = 16.dp, bottom = 24.dp)) {
            Column(Modifier.padding(horizontal = 16.dp).fillMaxWidth().background(colors.card, IspShapes.card).padding(horizontal = 16.dp, vertical = 4.dp)) {
                val rows = listOfNotNull(
                    "Categoría" to detail.category,
                    "Descripción" to detail.description,
                    "Fecha" to detail.date,
                    detail.documentCode.takeIf { it.isNotBlank() }?.let { "N.º de documento" to it },
                    detail.responsible.takeIf { it.isNotBlank() }?.let { "Registró" to it },
                )
                rows.forEachIndexed { index, (label, value) ->
                    DetailLine(label, value)
                    if (index < rows.lastIndex || detail.receiptUrls.isNotEmpty()) HorizontalDivider(color = colors.track)
                }
                if (detail.receiptUrls.isNotEmpty()) {
                    DetailLine("Comprobante", "Ver (${detail.receiptUrls.size})", Modifier.clickable(role = Role.Button) { onOpenReceipt(detail.receiptUrls, 0) })
                }
            }
            if (detail.isFixed) {
                InlineNote(
                    "Los egresos fijos no llevan comprobante y no cuentan para el límite mensual de variables.",
                    BadgeTone.Info,
                    Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp),
                )
            }
        }
    }
}

@Composable
private fun DetailLine(label: String, value: String, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(label, fontSize = 14.sp, lineHeight = 20.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, modifier = Modifier.weight(1f), fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.End)
    }
}
