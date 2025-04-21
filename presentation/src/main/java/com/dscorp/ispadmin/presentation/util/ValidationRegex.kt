package com.dscorp.ispadmin.presentation.util

object ValidationRegex {
    /**
     * Patrón para validar solo letras, espacios y caracteres acentuados
     * Válido para nombres, apellidos y otros campos que requieran solo texto
     */
    val ONLY_LETTERS_AND_SPACES = Regex("^[a-zA-ZáéíóúÁÉÍÓÚüÜñÑ ]+$")
    
    /**
     * Patrón para validar solo números
     * Útil para campos de teléfono, códigos, etc.
     */
    val ONLY_NUMBERS = Regex("^[0-9]+$")
    
    /**
     * Patrón para validar direcciones de correo electrónico
     */
    val EMAIL = Regex("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,6}$")
    
    /**
     * Patrón para validar DNI/Cédula (números y letras)
     */
    val DNI = Regex("^[0-9A-Za-z-]+$")
    
    /**
     * Patrón para validar direcciones (permite más caracteres especiales)
     */
    val ADDRESS = Regex("^[a-zA-Z0-9áéíóúÁÉÍÓÚüÜñÑ ,.#\\-/°]+$")
}