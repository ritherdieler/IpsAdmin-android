package com.dscorp.ispadmin.presentation.ui.features.subscriptionfinder.compose

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.dscorp.ispadmin.domain.model.SubscriptionResume
import com.dscorp.ispadmin.domain.model.createReminderMessage


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



