package com.dscorp.ispadmin.domain.model

import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter

enum class ServiceStatus {
    ACTIVE,
    CUT_OFF,
    CANCELLED,

    /** Any value the Core sends that this app version does not know yet. */
    UNKNOWN;

    fun getFormattedStatus(): String = when (this) {
        ACTIVE -> "Activo"
        CUT_OFF -> "Cortado"
        CANCELLED -> "Cancelado"
        UNKNOWN -> "Desconocido"
    }
}

/** Reads unknown or null statuses as [ServiceStatus.UNKNOWN] so non-null fields never hold null. */
class ServiceStatusAdapter : TypeAdapter<ServiceStatus>() {
    override fun write(out: JsonWriter, value: ServiceStatus?) {
        if (value == null || value == ServiceStatus.UNKNOWN) out.nullValue() else out.value(value.name)
    }

    override fun read(reader: JsonReader): ServiceStatus {
        if (reader.peek() == JsonToken.NULL) {
            reader.nextNull()
            return ServiceStatus.UNKNOWN
        }
        val name = reader.nextString()
        return ServiceStatus.entries.firstOrNull { it.name == name } ?: ServiceStatus.UNKNOWN
    }
}
