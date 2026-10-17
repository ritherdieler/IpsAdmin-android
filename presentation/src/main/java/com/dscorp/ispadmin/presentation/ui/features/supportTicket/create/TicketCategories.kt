package com.dscorp.ispadmin.presentation.ui.features.supportTicket.create

import com.dscorp.ispadmin.presentation.ui.features.supportTicket.detail.sentenceCase

/**
 * Ticket categories as the Core stores them, most used first (a-tic-05/06).
 * The Core has no categories endpoint, so the list and its order live here.
 */
val TICKET_CATEGORIES: List<String> = listOf(
    "Sin Conexión a Internet",
    "Otros",
    "Internet Lento",
    "Instalación de Internet",
    "No tiene señal de Tv Cable",
    "Cambio de Contraseña",
    "Migración a fibra óptica",
    "Ruptura de cable última milla",
    "Instalación de repetidor",
    "Añadir Tv Cable a su plan de internet",
    "Cambio de Domicilio",
    "Instalación de Tv Cable",
    "Cambio de Router",
    "Alineamiento de antena CPE",
    "Evaluar Factibilidad de Servicio",
    "Cambio de Onu",
)

/** Chips shown before "Ver las N categorías". */
const val TOP_CATEGORIES = 5

fun allCategoriesLabel(): String = "Ver las ${TICKET_CATEGORIES.size} categorías"

/** Mirrors AssistanceTicketController: category weight, then >5 Alta, >2 Media, else Baja. */
fun categoryPriority(category: String): String {
    val weight = when (category) {
        "Sin Conexión a Internet" -> 10
        "Cambio de Domicilio" -> 5
        "Otros" -> 3
        "Cambio de Contraseña" -> 2
        else -> 1
    }
    return when {
        weight > 5 -> "Alta"
        weight > 2 -> "Media"
        else -> "Baja"
    }
}

/** Display label: sentence case, with TV, ONU and CPE in capitals. */
fun categoryLabel(category: String): String =
    sentenceCase(category).split(' ').joinToString(" ") { word ->
        when (word.lowercase()) {
            "tv", "onu", "cpe" -> word.uppercase()
            else -> word
        }
    }

object CreateTicketTestTags {
    const val CATEGORY_SHEET = "create_ticket_category_sheet"
}
