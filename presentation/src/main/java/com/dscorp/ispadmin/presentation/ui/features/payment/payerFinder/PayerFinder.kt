package com.dscorp.ispadmin.presentation.ui.features.payment.payerFinder

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dscorp.ispadmin.presentation.theme.MyTheme
import com.dscorp.ispadmin.presentation.theme.Primary

@Composable
fun PayerFinder(
    modifier: Modifier = Modifier,
    results: List<String>,
    onTextChanged: (String) -> Unit = {}
) {
    var text by remember { mutableStateOf("") }

    Column(modifier = modifier
        .fillMaxSize()
        .padding(16.dp)) {

        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = text,
            label = { Text("Buscar pagador") },
            onValueChange = {
                text = it
                onTextChanged(it)
            }
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(results) { payerName ->
                Text(
                    text = payerName,
                    color = Primary
                )
                HorizontalDivider()
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PayerFinderPreview() {
    MyTheme {
        PayerFinder(
            results = listOf(
                "Pagador 1", 
                "Pagador 2", 
                "Pagador 3"
            )
        )
    }
}