package com.dscorp.ispadmin.presentation.ui.features.supportTicket.list.compose

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun TicketFilterBar(
    selectedDateFilter: TicketDateFilter,
    selectedSortOption: TicketSortOption,
    onDateFilterChange: (TicketDateFilter) -> Unit,
    onSortOptionChange: (TicketSortOption) -> Unit,
    modifier: Modifier = Modifier
) {
    var sortMenuExpanded by remember {
        mutableStateOf(false)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = "Filtrar por fecha",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TicketDateFilter.entries.forEach { filter ->
                FilterChip(
                    selected = selectedDateFilter == filter,
                    onClick = {
                        onDateFilterChange(filter)
                    },
                    label = {
                        Text(text = filter.displayName())
                    }
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Sort,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = "Ordenar por:",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.weight(1f))

            Column {
                TextButton(
                    onClick = {
                        sortMenuExpanded = true
                    }
                ) {
                    Text(text = selectedSortOption.displayName())

                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = "Mostrar opciones de ordenamiento"
                    )
                }

                DropdownMenu(
                    expanded = sortMenuExpanded,
                    onDismissRequest = {
                        sortMenuExpanded = false
                    }
                ) {
                    TicketSortOption.entries.forEach { option ->
                        DropdownMenuItem(
                            text = {
                                Text(text = option.displayName())
                            },
                            onClick = {
                                sortMenuExpanded = false
                                onSortOptionChange(option)
                            }
                        )
                    }
                }
            }
        }
    }

}

private fun TicketDateFilter.displayName(): String {
    return when (this) {
        TicketDateFilter.TODAY -> "Hoy"
        TicketDateFilter.YESTERDAY -> "Ayer"
        TicketDateFilter.TWO_DAYS_AGO -> "Antes de ayer"
        TicketDateFilter.ALL -> "Todos"
    }
}

private fun TicketSortOption.displayName(): String {
    return when (this) {
        TicketSortOption.DATE_DESCENDING -> "Más recientes"
        TicketSortOption.DATE_ASCENDING -> "Más antiguos"
        TicketSortOption.NAME_ASCENDING -> "Nombre A-Z"
        TicketSortOption.NAME_DESCENDING -> "Nombre Z-A"
    }
}
