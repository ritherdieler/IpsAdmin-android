package com.dscorp.ispadmin.presentation.utils

/**
 * Clase que contiene expresiones regulares para validaciones de formularios
 */
object FormValidations {
    /**
     * Regex para nombres y apellidos: solo letras, espacios y caracteres acentuados
     */
    val NAME_REGEX = Regex("^[a-zA-ZáéíóúÁÉÍÓÚüÜñÑ ]+$")
    
    /**
     * Regex para números de teléfono: solo dígitos
     */
    val PHONE_REGEX = Regex("^[0-9]+$")
    
    /**
     * Regex para DNI: solo dígitos (de 1 a 8)
     */
    val DNI_REGEX = Regex("^[0-9]{1,8}$")

    /**
     * Límites de caracteres comunes
     */
    object Limits {
        const val NAME_MAX_LENGTH = 100
        const val ADDRESS_MAX_LENGTH = 150
        const val PHONE_MAX_LENGTH = 9
        const val DNI_MAX_LENGTH = 8
    }
}