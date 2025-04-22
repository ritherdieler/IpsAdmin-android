package com.example.data2.data.util

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import java.lang.reflect.Type
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

/**
 * Adaptador personalizado para Gson que maneja la serialización y deserialización
 * de objetos LocalDateTime desde/hacia strings en formato ISO.
 */
class LocalDateTimeAdapter : JsonSerializer<LocalDateTime>, JsonDeserializer<LocalDateTime> {
    
    private val formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME
    
    override fun serialize(
        src: LocalDateTime?,
        typeOfSrc: Type,
        context: JsonSerializationContext
    ): JsonElement {
        return JsonPrimitive(src?.format(formatter) ?: "")
    }
    
    override fun deserialize(
        json: JsonElement,
        typeOfT: Type,
        context: JsonDeserializationContext
    ): LocalDateTime? {
        if (json.isJsonNull) return null
        
        val dateStr = json.asString
        if (dateStr.isBlank()) return null
        
        return try {
            LocalDateTime.parse(dateStr, formatter)
        } catch (e: DateTimeParseException) {
            // En caso de formato inválido, podemos intentar un formato alternativo
            try {
                // Si no tiene segundos
                if (dateStr.contains("T")) {
                    LocalDateTime.parse(dateStr, DateTimeFormatter.ISO_DATE_TIME)
                } else {
                    // Si es solo fecha
                    LocalDateTime.parse("${dateStr}T00:00:00")
                }
            } catch (e: Exception) {
                null
            }
        }
    }
}
