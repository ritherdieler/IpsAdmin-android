package com.dscorp.ispadmin.observability

object ObsSanitize {
    private const val REDACTED = "[redacted]"
    private val SENSITIVE_KEY = Regex(
        "(?i)(password|passwd|passphrase|token|authorization|api[_-]?key|secret|dni|phone|ruc|creditcard)"
    )

    fun sanitizeMap(input: Map<String, Any?>?, sanitize: Boolean): Map<String, Any?>? {
        if (input == null) return null
        if (!sanitize) return input
        return input.mapValues { (key, value) -> sanitizeEntry(key, value) }
    }

    private fun sanitizeEntry(key: String?, value: Any?): Any? = when {
        key != null && SENSITIVE_KEY.containsMatchIn(key) -> REDACTED
        else -> sanitizeValue(value)
    }

    private fun sanitizeValue(value: Any?): Any? = when (value) {
        is Map<*, *> -> value.entries.associate { (k, v) -> k to sanitizeEntry(k?.toString(), v) }
        is List<*> -> value.map(::sanitizeValue)
        else -> value
    }
}
