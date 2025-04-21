package com.dscorp.ispadmin.presentation.ui.features.fixedCost.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dscorp.ispadmin.presentation.theme.MyTheme
import com.dscorp.ispadmin.presentation.theme.myTypography
import com.dscorp.ispadmin.presentation.ui.features.composecomponents.MyDropDown
import com.dscorp.ispadmin.domain.model.FixedCostType
import com.example.data2.data.apirequestmodel.FixedCostRequest

@Composable
fun RegisterFixedCostForm(
    modifier: Modifier = Modifier,
    onSave: (FixedCostRequest) -> Unit
) {
    var amount by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var type by remember { mutableStateOf<FixedCostType?>(null) }
    var fixedCost by remember { mutableStateOf(FixedCostRequest()) }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            style = myTypography.titleLarge,
            text = "Registro de gasto fijo"
        )
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = amount,
            onValueChange = { amount = it },
            label = { Text("Monto") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)

        )
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = description,
            onValueChange = { description = it },
            label = { Text("Descripcion") }
        )

        MyDropDown(
            modifier = Modifier.fillMaxWidth(),
            items = FixedCostType.values().toList(),
            onTypeSelected = { type = it }
        )

        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),

            value = note,
            onValueChange = { note = it },
            label = { Text("Nota (opcional)") }
        )

        Button(
            modifier = Modifier.fillMaxWidth(),
            enabled = amount.isNotEmpty() && description.isNotEmpty() && type != null,
            onClick = {
                 fixedCost = fixedCost.copy(
                    amount = amount.toDouble(),
                    description = description,
                    note = note,
                    type = type,
                )
                onSave(fixedCost)
            }
        ) {
            Text("Guardar")
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun FixedCostPreview() {

    MyTheme {
        RegisterFixedCostForm {}
    }

}