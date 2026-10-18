package com.dscorp.ispadmin.presentation.ui.features.payment.cash

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dscorp.ispadmin.presentation.theme.IspTheme
import com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.hub.BadgeTone
import com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.hub.HubBadge
import com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.hub.StatusBadge

object PaymentSearchTestTags {
    const val QUERY = "payment_search_query"
    const val METHOD = "payment_search_method"
    const val PERIOD = "payment_search_period"
}

private val CARD = RoundedCornerShape(16.dp)

/** a-caj-06 «Buscar pagos». */
@Composable
fun PaymentSearchContent(
    state: PaymentSearchUiState,
    onIntent: (PaymentSearchIntent) -> Unit,
    onOpen: (Int) -> Unit,
    onBack: () -> Unit,
) {
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    LazyColumn(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).statusBarsPadding(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item("search") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Volver") }
                SearchField(state.query, onChange = { onIntent(PaymentSearchIntent.QueryChanged(it)) }, modifier = Modifier.weight(1f))
            }
        }
        item("filters") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MenuChip(
                    label = state.method ?: "Todos los métodos",
                    options = listOf<Pair<String, () -> Unit>>("Todos los métodos" to { onIntent(PaymentSearchIntent.MethodSelected(null)) }) +
                        state.methods.map { method -> method to { onIntent(PaymentSearchIntent.MethodSelected(method)) } },
                    tag = PaymentSearchTestTags.METHOD,
                )
                MenuChip(
                    label = state.period.label,
                    options = PaymentPeriod.entries.map { period -> period.label to { onIntent(PaymentSearchIntent.PeriodSelected(period)) } },
                    tag = PaymentSearchTestTags.PERIOD,
                )
            }
        }
        item("hint") { Text("Busca por titular o por el nombre que aparece en Yape o Plin", fontSize = 13.sp, color = muted) }
        state.error?.let { error ->
            item("error") {
                Column {
                    Text(error, fontSize = 14.sp, color = MaterialTheme.colorScheme.error)
                    TextButton(onClick = { onIntent(PaymentSearchIntent.Retry) }, contentPadding = PaddingValues(0.dp)) { Text("Reintentar") }
                }
            }
        }
        if (state.loading && state.result == null) {
            item("loading") { Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() } }
        }
        state.result?.let { result ->
            result.groups.forEach { group ->
                item("day-${group.label}") {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(group.label, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = muted)
                        Column(Modifier.fillMaxWidth().background(IspTheme.colors.card, CARD)) {
                            group.hits.forEachIndexed { index, hit ->
                                if (index > 0) HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = IspTheme.colors.cardBorder)
                                HitRow(hit, state.words) { onOpen(hit.receiptId) }
                            }
                        }
                    }
                }
            }
            item("footer") {
                Text(result.footer, fontSize = 13.sp, color = muted, modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            }
        }
    }
}

@Composable
private fun SearchField(value: String, onChange: (String) -> Unit, modifier: Modifier = Modifier) {
    // The field owns the text (the view model state arrives a frame later and would drop fast keystrokes);
    // it starts from the saved query with the cursor at the end.
    var field by remember { mutableStateOf(TextFieldValue(value, TextRange(value.length))) }
    fun edit(next: TextFieldValue) {
        val changed = next.text != field.text
        field = next
        if (changed) onChange(next.text)
    }
    Row(
        modifier.heightIn(min = 52.dp).border(2.dp, MaterialTheme.colorScheme.onSurface, RoundedCornerShape(26.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow, RoundedCornerShape(26.dp)).padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Rounded.Search, contentDescription = null, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(10.dp))
        Box(Modifier.weight(1f)) {
            if (field.text.isEmpty()) Text("Titular o nombre del pagador", fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            BasicTextField(
                value = field,
                onValueChange = ::edit,
                singleLine = true,
                textStyle = TextStyle(fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.onSurface),
                modifier = Modifier.fillMaxWidth().testTag(PaymentSearchTestTags.QUERY),
            )
        }
        if (field.text.isNotEmpty()) {
            IconButton(onClick = { edit(TextFieldValue("")) }) { Icon(Icons.Rounded.Close, contentDescription = "Borrar búsqueda") }
        }
    }
}

@Composable
private fun MenuChip(label: String, options: List<Pair<String, () -> Unit>>, tag: String) {
    var open by remember { mutableStateOf(false) }
    Box {
        Row(
            Modifier.heightIn(min = 36.dp).border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                .clickable(role = Role.DropdownList) { open = true }.padding(horizontal = 12.dp).testTag(tag),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(label, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Icon(Icons.Rounded.ExpandMore, contentDescription = null, modifier = Modifier.size(18.dp))
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            options.forEach { (text, select) ->
                DropdownMenuItem(text = { Text(text) }, onClick = { open = false; select() })
            }
        }
    }
}

@Composable
private fun HitRow(hit: PaymentHit, words: List<String>, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 64.dp).clickable(role = Role.Button, onClick = onClick).padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(highlighted(hit.name, words), fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Text(highlighted(hit.detail, words), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(hit.amount, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun highlighted(text: String, words: List<String>): AnnotatedString {
    val mark = SpanStyle(background = IspTheme.colors.warningContainer)
    return buildAnnotatedString {
        append(text)
        matchRanges(text, words).forEach { addStyle(mark, it.first, it.last + 1) }
    }
}

/** a-caj-05 «Detalle de pago». */
@Composable
fun PaymentDetailContent(
    state: PaymentReceiptUiState,
    onBack: () -> Unit,
    onShare: () -> Unit,
    onOpenSubscription: (Int) -> Unit,
    onRetry: () -> Unit,
) {
    val colors = IspTheme.colors
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val detail = state.detail
    Column(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).statusBarsPadding()
            .verticalScroll(rememberScrollState()).padding(bottom = 24.dp),
    ) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Volver") }
            Text(detail?.title ?: "Detalle de pago", fontSize = 20.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            if (detail != null) IconButton(onClick = onShare) { Icon(Icons.Rounded.Share, contentDescription = "Compartir comprobante") }
        }
        when {
            detail == null && state.error != null -> Column(Modifier.padding(16.dp)) {
                Text(state.error, fontSize = 14.sp, color = MaterialTheme.colorScheme.error)
                TextButton(onClick = onRetry, contentPadding = PaddingValues(0.dp)) { Text("Reintentar") }
            }
            detail == null -> Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            else -> Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(detail.amount, fontSize = 34.sp, fontWeight = FontWeight.Bold)
                        HubBadge(StatusBadge("Pagado", BadgeTone.SUCCESS))
                    }
                    if (detail.holder.isNotBlank()) Text(detail.holder, fontSize = 14.sp, color = muted)
                }
                Column(Modifier.fillMaxWidth().background(colors.card, CARD).padding(horizontal = 16.dp)) {
                    detail.rows.forEachIndexed { index, (label, value) ->
                        if (index > 0) HorizontalDivider(color = colors.cardBorder)
                        Row(Modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(label, fontSize = 14.sp, color = muted, modifier = Modifier.weight(1f))
                            Text(value, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
                if (detail.hasProof) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Comprobante", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                        Row(
                            Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceContainer, CARD).padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Outlined.ReceiptLong, contentDescription = null, tint = muted)
                            Spacer(Modifier.width(12.dp))
                            Text("Captura de ${detail.method} recibida. Se ve en el back office.", fontSize = 14.sp, color = muted)
                        }
                    }
                }
                OutlinedButton(onClick = onShare, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
                    Icon(Icons.Rounded.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Compartir comprobante", fontWeight = FontWeight.SemiBold)
                }
                detail.subscriptionId?.let { id ->
                    TextButton(onClick = { onOpenSubscription(id) }, modifier = Modifier.fillMaxWidth()) {
                        Text("Ver suscripción", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}
