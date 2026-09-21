package com.dscorp.ispadmin.data.utils

import com.google.gson.JsonParser

object ApiErrorBodyParser {
    fun parse(errorBody: String?, fallback: String): String {
        val body = errorBody?.takeIf { it.isNotBlank() } ?: return fallback
        return runCatching {
            val element = JsonParser.parseString(body)
            if (!element.isJsonObject) return@runCatching null
            val obj = element.asJsonObject
            sequenceOf("error", "message", "failureReason")
                .mapNotNull { key ->
                    obj.get(key)?.takeIf { it.isJsonPrimitive }?.asString
                }
                .firstOrNull { it.isNotBlank() }
        }.getOrNull() ?: fallback
    }
}
