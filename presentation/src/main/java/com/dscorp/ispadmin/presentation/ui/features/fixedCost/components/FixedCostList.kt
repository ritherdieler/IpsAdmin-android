package com.dscorp.ispadmin.presentation.ui.features.fixedCost.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dscorp.ispadmin.presentation.theme.MyTheme
import com.dscorp.ispadmin.presentation.theme.myTypography
import com.dscorp.ispadmin.presentation.ui.features.fixedCost.GetAllFixedCostsState
import com.dscorp.ispadmin.presentation.ui.features.migration.Loader
import com.dscorp.ispadmin.domain.model.FixedCost
import com.dscorp.ispadmin.domain.model.FixedCostType

@Composable
fun FixedCostList(modifier: Modifier = Modifier, fixedCosts: GetAllFixedCostsState) {

    when (fixedCosts) {
        GetAllFixedCostsState.Error -> Text("Error al obtener los gastos fijos")
        GetAllFixedCostsState.Loading -> Loader()
        is GetAllFixedCostsState.Success -> {
            LazyColumn(modifier = modifier.fillMaxSize()) {
                itemsIndexed(fixedCosts.fixedCosts) { index, fixedCost ->
                    FixedCostRow(
                        modifier = Modifier.background(if (index % 2 == 0) Color.LightGray else Color.White).padding(horizontal = 16.dp),
                        fixedCost = fixedCost
                    )
                    HorizontalDivider()
                }

                item {
                    Text(
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                        text = "Total: S/. ${fixedCosts.fixedCosts.sumOf { it.amount }}",
                        style = myTypography.titleLarge,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }

}

@Composable
fun FixedCostRow(modifier: Modifier, fixedCost: FixedCost) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp, top = 16.dp)
    ) {
        Text(
            text = fixedCost.description,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge
        )
        Text(
            textAlign = TextAlign.End,
            text = fixedCost.amount.toString(),
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge
        )
    }
}


@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun FixedCostPreview() {
    MyTheme {
        FixedCostList(
            fixedCosts = GetAllFixedCostsState.Success(
                listOf(
                    FixedCost(100.0, "Pago de personal", "Nota", FixedCostType.STAFF_PAYMENT, 1),
                    FixedCost(200.0, "Pago a proveedor", "Nota", FixedCostType.PROVIDER_PAYMENT, 1),
                    FixedCost(
                        300.0,
                        "Infraestructura de sistema",
                        "Nota",
                        FixedCostType.SYSTEM_INFRASTRUCTURE,
                        1
                    ),
                    FixedCost(400.0, "Oficina", "Nota", FixedCostType.OFFICE, 1),
                    FixedCost(500.0, "Otros", "Nota", FixedCostType.OTHER, 1)
                )
            )
        )
    }
}