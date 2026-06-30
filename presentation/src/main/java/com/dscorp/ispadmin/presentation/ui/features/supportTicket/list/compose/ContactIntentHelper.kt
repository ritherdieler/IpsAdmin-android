package com.dscorp.ispadmin.presentation.ui.features.supportTicket.list.compose

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri

object ContactIntentHelper {

    fun openDialer(
        context: Context,
        phoneNumber: String
    ): Boolean {
        val normalizedPhone = phoneNumber.filter(Char::isDigit)

        if (normalizedPhone.isBlank()) return false

        val intent = Intent(
            Intent.ACTION_DIAL,
            Uri.parse("tel:$normalizedPhone")
        )

        return startSafely(context, intent)
    }

    fun openWhatsApp(
        context: Context,
        phoneNumber: String
    ): Boolean {
        val normalizedPhone = normalizePeruvianPhone(phoneNumber)

        if (normalizedPhone.isBlank()) return false

        val uri = Uri.parse("https://wa.me/$normalizedPhone")

        val intent = Intent(
            Intent.ACTION_VIEW,
            uri
        )

        return startSafely(context, intent)
    }

    private fun normalizePeruvianPhone(phoneNumber: String): String {
        val digits = phoneNumber.filter(Char::isDigit)

        return when {
            digits.startsWith("51") -> digits
            digits.length == 9 -> "51$digits"
            else -> digits
        }
    }

    private fun startSafely(
        context: Context,
        intent: Intent
    ): Boolean {
        return try {
            context.startActivity(intent)
            true
        } catch (_: ActivityNotFoundException) {
            false
        } catch (_: SecurityException) {
            false
        }
    }
}