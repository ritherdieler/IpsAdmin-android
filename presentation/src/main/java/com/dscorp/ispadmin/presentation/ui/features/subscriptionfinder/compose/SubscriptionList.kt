package com.dscorp.ispadmin.presentation.ui.features.subscriptionfinder.compose

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dscorp.ispadmin.domain.model.Place
import com.dscorp.ispadmin.domain.model.ServiceStatus
import com.dscorp.ispadmin.domain.model.SubscriptionResume
import com.dscorp.ispadmin.domain.model.createReminderMessage


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SubscriptionList(
    subscriptions: Map<ServiceStatus, List<SubscriptionResume>>,
    scrollState: LazyListState,
    onMenuItemSelected: (menuItem: SubscriptionMenu, subscriptionResponse: SubscriptionResume) -> Unit = { _, _ -> },
    onSubscriptionExpanded: (SubscriptionResume, Boolean) -> Unit = { _, _ -> },
    expandedSubscriptionId: Int? = null,
    customerFormData: CustomerFormData? = null,
    placesState: PlacesState = PlacesState(),
    saveState: SaveSubscriptionState = SaveSubscriptionState.Success,
    onFieldChange: (String, String) -> Unit = { _, _ -> },
    onPlaceSelected: (Place) -> Unit = {},
    onUpdatePlaceId: (Int, String) -> Unit = { _, _ -> },
    onSaveCustomer: () -> Unit = {}
) {
    LazyColumn(modifier = Modifier.fillMaxWidth(), state = scrollState) {
        subscriptions.forEach { (status, subscriptionList) ->
            stickyHeader {
                Text(
                    text = status.getFormattedStatus(),
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.surface)
                        .fillMaxWidth()
                        .padding(8.dp)
                )
            }
            items(items = subscriptionList, key = { it.id }) { subscription ->
                val isExpanded = expandedSubscriptionId == subscription.id

                SubscriptionCard(
                    subscriptionResume = subscription,
                    onMenuItemSelected = { onMenuItemSelected(it, subscription) },
                    onExpandChange = onSubscriptionExpanded,
                    expanded = isExpanded,
                    customerFormData = customerFormData,
                    placesState = placesState,
                    saveState = saveState,
                    onFieldChange = onFieldChange,
                    onPlaceSelected = onPlaceSelected,
                    onUpdatePlace = onUpdatePlaceId,
                    onSaveClick = onSaveCustomer
                )
            }
        }
    }
}

fun sendWhatsapp(subscriptionResume: SubscriptionResume, context: Context) {
    val message = when {
        subscriptionResume.totalDebt == 0.0 -> "Hola ${subscriptionResume.customerName}! 🌟\n"
        else -> subscriptionResume.createReminderMessage()
    }
    val intent = Intent(Intent.ACTION_VIEW).apply {
        data =
            Uri.parse("http://api.whatsapp.com/send?phone=51${subscriptionResume.customer.phone}&text=$message")
    }
    context.startActivity(intent)
}

fun openInBrowser(ipAddress: String, context: Context) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("http://$ipAddress"))
    context.startActivity(intent)
}

fun openMap(subscriptionResume: SubscriptionResume, context: Context) {
    // Obtener las coordenadas geográficas de la suscripción
    val latitude = subscriptionResume.location.latitude
    val longitude = subscriptionResume.location.longitude

    try {
        // Verificar si las coordenadas son válidas (no son 0,0)
        if (latitude != 0.0 && longitude != 0.0) {
            // Usar las coordenadas directamente para una ubicación precisa
            val gmmIntentUri = Uri.parse("geo:$latitude,$longitude?q=$latitude,$longitude")
            val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
            mapIntent.setPackage("com.google.android.apps.maps") // Especificar la app de Google Maps

            // Si Google Maps está instalado, abrirlo; de lo contrario, abrir en el navegador
            if (mapIntent.resolveActivity(context.packageManager) != null) {
                context.startActivity(mapIntent)
            } else {
                // Alternativa: abrir en el navegador
                val mapUrl = "https://maps.google.com/?q=$latitude,$longitude"
                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(mapUrl))
                context.startActivity(browserIntent)
            }
        } else {
            // Si las coordenadas son 0,0, usar la dirección como respaldo
            val address = subscriptionResume.customer.address
            val place = subscriptionResume.placeName
            val locationQuery = "$address, $place, Peru"

            // Mostrar mensaje informativo
            Toast.makeText(
                context,
                "No hay coordenadas disponibles. Usando dirección alternativa.",
                Toast.LENGTH_SHORT
            ).show()

            val gmmIntentUri = Uri.parse("geo:0,0?q=${Uri.encode(locationQuery)}")
            val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
            mapIntent.setPackage("com.google.android.apps.maps")

            if (mapIntent.resolveActivity(context.packageManager) != null) {
                context.startActivity(mapIntent)
            } else {
                val mapUrl = "https://maps.google.com/?q=${Uri.encode(locationQuery)}"
                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(mapUrl))
                context.startActivity(browserIntent)
            }
        }
    } catch (e: Exception) {
        Toast.makeText(context, "No se pudo abrir el mapa: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}



