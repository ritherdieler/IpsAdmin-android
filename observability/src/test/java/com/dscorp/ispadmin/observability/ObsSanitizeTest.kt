package com.dscorp.ispadmin.observability

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ObsSanitizeTest {

    @Test
    fun `strips sensitive keys when sanitizing`() {
        val input = mapOf(
            "username" to "ana",
            "password" to "secret",
            "token" to "abc",
            "nestedSafe" to "ok"
        )

        val result = ObsSanitize.sanitizeMap(input, sanitize = true)

        assertThat(result).containsEntry("username", "ana")
        assertThat(result).containsEntry("nestedSafe", "ok")
        assertThat(result).containsEntry("password", "[redacted]")
        assertThat(result).containsEntry("token", "[redacted]")
    }

    @Test
    fun `redacts keys by pattern at any depth including lists`() {
        val input = mapOf(
            "wifiPassword24" to "clave-real",
            "pppoePassword" to "p1",
            "phone" to "987654321",
            "registration" to mapOf("dni" to "12345678", "planId" to 3),
            "items" to listOf(mapOf("accessToken" to "abc"), "plain"),
            "serial" to "ZTEG1"
        )

        val result = ObsSanitize.sanitizeMap(input, sanitize = true)!!

        assertThat(result).containsEntry("wifiPassword24", "[redacted]")
        assertThat(result).containsEntry("pppoePassword", "[redacted]")
        assertThat(result).containsEntry("phone", "[redacted]")
        assertThat(result).containsEntry("serial", "ZTEG1")
        assertThat(result["registration"]).isEqualTo(mapOf("dni" to "[redacted]", "planId" to 3))
        assertThat(result["items"]).isEqualTo(listOf(mapOf("accessToken" to "[redacted]"), "plain"))
    }

    @Test
    fun `keeps values when sanitize is false`() {
        val input = mapOf("password" to "secret")
        val result = ObsSanitize.sanitizeMap(input, sanitize = false)
        assertThat(result).containsEntry("password", "secret")
    }
}
