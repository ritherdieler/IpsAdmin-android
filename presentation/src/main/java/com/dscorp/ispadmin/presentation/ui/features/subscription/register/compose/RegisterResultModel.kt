package com.dscorp.ispadmin.presentation.ui.features.subscription.register.compose

import com.dscorp.ispadmin.domain.model.Subscription
import com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.hub.toTitleCase
import java.net.URLEncoder

/** What the technician sends to the client after installing: Wi-Fi name and key, and the client code. */
internal fun installationMessage(subscription: Subscription): String {
    val first = subscription.firstName.orEmpty().toTitleCase().substringBefore(' ')
    val ssid5 = subscription.wifiSsid5?.takeIf { it.isNotBlank() && it != subscription.wifiSsid24 }
    return buildList {
        add("Hola ${first.ifBlank { "" }}".trim() + ", tu servicio de internet quedó instalado.")
        subscription.wifiSsid24?.takeIf { it.isNotBlank() }?.let { add("Wi-Fi: $it") }
        ssid5?.let { add("Wi-Fi 5 GHz: $it") }
        subscription.wifiPassword24?.takeIf { it.isNotBlank() }?.let { add("Clave: $it") }
        subscription.resolvedSubscriptionId()?.let { add("Código de cliente: $it") }
    }.joinToString("\n")
}

/** wa.me link for a Peruvian mobile (9 digits, or with the 51 prefix); null when the phone cannot receive it. */
internal fun whatsappLink(phone: String?, message: String): String? {
    val digits = phone.orEmpty().filter { it.isDigit() }
    val number = when {
        digits.length == 9 -> "51$digits"
        digits.length == 11 && digits.startsWith("51") -> digits
        else -> return null
    }
    return "https://wa.me/$number?text=" + URLEncoder.encode(message, "UTF-8").replace("+", "%20")
}
