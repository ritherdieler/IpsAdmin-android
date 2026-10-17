package com.dscorp.ispadmin.presentation.ui.features.payment.payerFinder

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.dscorp.ispadmin.domain.model.extensions.PayerFinderResult
import com.dscorp.ispadmin.navigation.NavRoutes.FeatureRoutes
import com.dscorp.ispadmin.presentation.theme.IspTheme
import com.dscorp.ispadmin.presentation.theme.MyTheme
import com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.hub.formatSoles
import com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.hub.toTitleCase
import org.koin.androidx.compose.koinViewModel
import java.text.NumberFormat
import java.util.Locale

internal const val MIN_PAYER_QUERY = 3

/** Caja: find a payment by the name shown in Yape or Plin, open its subscription, or start a new charge. */
@Composable
fun FindPayerComposeScreen(
    navController: NavHostController,
    viewModel: PayerFinderViewmodel = koinViewModel()
) {
    val state by viewModel.state.collectAsState()
    MyTheme {
        CashContent(
            state = state,
            onQueryChange = { viewModel.onEvent(PayerFinderEvent.SearchQueryChanged(it)) },
            onOpenSubscription = { navController.navigate(FeatureRoutes.Subscription.Details(it)) },
            onNewCharge = { navController.navigate(FeatureRoutes.Subscription.Find) { launchSingleTop = true } },
        )
    }
}

enum class CashPhase { IDLE, LOADING, ERROR, EMPTY, RESULTS }

val PayerFinderState.phase: CashPhase
    get() = when {
        isLoading -> CashPhase.LOADING
        error != null -> CashPhase.ERROR
        searchQuery.trim().length < MIN_PAYER_QUERY -> CashPhase.IDLE
        electronicPayers.isEmpty() -> CashPhase.EMPTY
        else -> CashPhase.RESULTS
    }

object CashTestTags {
    const val NEW_CHARGE = "cash_new_charge"
    const val QUERY = "cash_query"
    fun result(index: Int) = "cash_result_$index"
}

@Composable
fun CashContent(
    state: PayerFinderState,
    onQueryChange: (String) -> Unit,
    onOpenSubscription: (Int) -> Unit,
    onNewCharge: () -> Unit,
) {
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = onQueryChange,
                placeholder = { Text("Buscar pago o pagador") },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(28.dp),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp).testTag(CashTestTags.QUERY),
            )
            when (state.phase) {
                CashPhase.IDLE -> CashMessage("Escribe al menos 3 letras del nombre que aparece en Yape o Plin")
                CashPhase.LOADING -> Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                CashPhase.ERROR -> CashMessage(state.error.orEmpty(), isError = true)
                CashPhase.EMPTY -> CashMessage("Sin pagos con ese nombre")
                CashPhase.RESULTS -> PaymentResults(state.electronicPayers, onOpenSubscription)
            }
        }
        ExtendedFloatingActionButton(
            onClick = onNewCharge,
            icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
            text = { Text("Nuevo cobro", fontWeight = FontWeight.SemiBold) },
            containerColor = IspTheme.colors.accent,
            contentColor = IspTheme.colors.onAccent,
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp).testTag(CashTestTags.NEW_CHARGE),
        )
    }
}

/** Text on the orange accent: white fails contrast (3.2:1). */

@Composable
private fun CashMessage(text: String, isError: Boolean = false) {
    Text(
        text = text,
        textAlign = TextAlign.Center,
        style = MaterialTheme.typography.bodyMedium,
        color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 24.dp),
    )
}

@Composable
private fun PaymentResults(results: List<PayerFinderResult>, onOpenSubscription: (Int) -> Unit) {
    LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp)) {
        itemsIndexed(results) { index, payment ->
            Column(
                Modifier.fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface, rowShape(index, results.lastIndex))
                    .clickable { onOpenSubscription(payment.subscriptionId) }
                    .testTag(CashTestTags.result(index))
            ) {
                PaymentRow(payment)
                if (index < results.lastIndex) HorizontalDivider(color = MaterialTheme.colorScheme.surfaceContainer)
            }
        }
    }
}

private fun rowShape(index: Int, last: Int): RoundedCornerShape = when {
    last == 0 -> RoundedCornerShape(16.dp)
    index == 0 -> RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
    index == last -> RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp)
    else -> RoundedCornerShape(0.dp)
}

@Composable
private fun PaymentRow(payment: PayerFinderResult) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 64.dp).padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(payment.subscriptionName.toTitleCase(), fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
            Text(
                "${payment.paymentMethod} · pagó: ${payment.electronicPayerName} · ${payment.paymentDate}",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
            )
        }
        Text(formatSoles(payment.amountPaid), fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
    }
}

internal fun formatCurrency(amount: Double): String {
    val format = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("es-PE"))
    return format.format(amount)
}
