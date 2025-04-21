package com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.compose

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.asFlow
import com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.SubscriptionDetailViewModel
import com.dscorp.ispadmin.domain.model.extensions.toFormattedDateString
import org.koin.androidx.compose.koinViewModel

@Composable
fun SubscriptionDetailForm(
    subscriptionId: Int,
    viewModel: SubscriptionDetailViewModel = koinViewModel()
) {
    val scrollState = rememberScrollState()
    val places by viewModel.places.asFlow().collectAsState(initial = listOf())

    LaunchedEffect(places) {
        viewModel.initForm(subscriptionId)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .verticalScroll(scrollState)
    ) {
        // Customer Information Card
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "Información del Cliente",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Customer basic information
                FormFieldRow(
                    label1 = viewModel.editSubscriptionForm.firstNameField.hint ?: "",
                    value1 = viewModel.editSubscriptionForm.firstNameField.liveData.value ?: "",
                    label2 = viewModel.editSubscriptionForm.lastNameField.hint ?: "",
                    value2 = viewModel.editSubscriptionForm.lastNameField.liveData.value ?: ""
                )

                Spacer(modifier = Modifier.height(8.dp))

                FormFieldRow(
                    label1 = viewModel.editSubscriptionForm.dniField.hint ?: "",
                    value1 = viewModel.editSubscriptionForm.dniField.liveData.value ?: "",
                    label2 = viewModel.editSubscriptionForm.addressField.hint ?: "",
                    value2 = viewModel.editSubscriptionForm.addressField.liveData.value ?: ""
                )

                Spacer(modifier = Modifier.height(8.dp))

                FormFieldRow(
                    label1 = viewModel.editSubscriptionForm.phoneField.hint ?: "",
                    value1 = viewModel.editSubscriptionForm.phoneField.liveData.value ?: "",
                    label2 = viewModel.editSubscriptionForm.couponField.hint ?: "",
                    value2 = viewModel.editSubscriptionForm.couponField.liveData.value ?: ""
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Subscription Details Card
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "Detalles de la Suscripción",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Plan and Place information
                FormFieldRow(
                    label1 = viewModel.editSubscriptionForm.planField.hint ?: "",
                    value1 = viewModel.editSubscriptionForm.planField.liveData.value?.name ?: "",
                    label2 = viewModel.editSubscriptionForm.placeField.hint ?: "",
                    value2 = viewModel.editSubscriptionForm.placeField.liveData.value?.name ?: ""
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Note field
                FormField(
                    label = viewModel.editSubscriptionForm.noteField.hint ?: "",
                    value = viewModel.editSubscriptionForm.noteField.liveData.value ?: ""
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Subscription date
                FormField(
                    label = viewModel.editSubscriptionForm.subscriptionDateField.hint ?: "",
                    value = viewModel.editSubscriptionForm.subscriptionDateField.liveData.value?.toFormattedDateString() ?: ""
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Technician and price
                FormFieldRow(
                    label1 = viewModel.editSubscriptionForm.technicianField.hint ?: "",
                    value1 = viewModel.editSubscriptionForm.technicianField.liveData.value?.name ?: "",
                    label2 = viewModel.editSubscriptionForm.priceField.hint ?: "",
                    value2 = viewModel.editSubscriptionForm.priceField.liveData.value ?: ""
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Technical Details Card
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "Detalles Técnicos",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(16.dp))

                // IP field
                FormField(
                    label = viewModel.editSubscriptionForm.ipField.hint ?: "",
                    value = viewModel.editSubscriptionForm.ipField.liveData.value ?: ""
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Host device field
                FormField(
                    label = viewModel.editSubscriptionForm.hostDeviceField.hint ?: "",
                    value = viewModel.editSubscriptionForm.hostDeviceField.liveData.value?.name ?: ""
                )
            }
        }
    }
}

@Composable
private fun FormFieldRow(
    label1: String,
    value1: String,
    label2: String,
    value2: String,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier.fillMaxWidth()) {
        FormField(
            label = label1,
            value = value1,
            modifier = Modifier.weight(1f)
        )
        
        Spacer(modifier = Modifier.width(16.dp))
        
        FormField(
            label = label2,
            value = value2,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun FormField(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(2.dp))
        
        Text(
            text = value.ifEmpty { "—" },
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
