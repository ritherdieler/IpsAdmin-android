package com.dscorp.ispadmin.data.utils

import com.google.gson.JsonParser

object ApiErrorBodyParser {
    /** What Spring Boot's default error page says when nothing filled the message. */
    private const val SPRING_NO_MESSAGE = "No message available"

    /**
     * True for a 404 that does not say what was missing: Spring Boot's default error page for a route this
     * Core does not map (the production Core before the new one is deployed). The Core's own 404s carry a message.
     */
    fun isMissingEndpoint(code: Int, errorBody: String?): Boolean {
        if (code != HttpCodes.NOT_FOUND) return false
        val message = runCatching {
            val element = JsonParser.parseString(errorBody.orEmpty())
            if (!element.isJsonObject) return@runCatching null
            element.asJsonObject.get("message")?.takeIf { it.isJsonPrimitive }?.asString
        }.getOrNull()
        return message.isNullOrBlank() || message == SPRING_NO_MESSAGE
    }

    fun errorCode(errorBody: String?): String? {
        val body = errorBody?.takeIf { it.isNotBlank() } ?: return null
        return runCatching {
            val element = JsonParser.parseString(body)
            if (!element.isJsonObject) return@runCatching null
            element.asJsonObject.get("errorCode")?.takeIf { it.isJsonPrimitive }?.asString?.takeIf { it.isNotBlank() }
        }.getOrNull()
    }

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
