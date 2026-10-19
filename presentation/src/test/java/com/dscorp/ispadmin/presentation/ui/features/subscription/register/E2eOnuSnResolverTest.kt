package com.dscorp.ispadmin.presentation.ui.features.subscription.register

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class E2eOnuSnResolverTest {

    @Test
    fun `blank instrumentation arg falls back to first onu`() {
        assertThat(E2eOnuSnResolver.resolve(null)).isNull()
        assertThat(E2eOnuSnResolver.resolve("")).isNull()
        assertThat(E2eOnuSnResolver.resolve("   ")).isNull()
        assertThat(E2eOnuSnResolver.shouldSelectFirstOnu(null)).isTrue()
    }

    @Test
    fun `serial arg selects that onu`() {
        assertThat(E2eOnuSnResolver.resolve("ZTEGDC47BFFD")).isEqualTo("ZTEGDC47BFFD")
        assertThat(E2eOnuSnResolver.resolve("  ZTEGDC47BFFD  ")).isEqualTo("ZTEGDC47BFFD")
        assertThat(E2eOnuSnResolver.shouldSelectFirstOnu("ZTEGDC47BFFD")).isFalse()
    }

    @Test
    fun `matches genieacs vsol serial against olt vendor serial by hex suffix`() {
        assertThat(E2eOnuSnResolver.matches("VSOL0031C0B6", "12345B4641531C0B6")).isTrue()
        assertThat(E2eOnuSnResolver.matches("12345B4641531C0B6 (VSOL-0031C0B6)", "VSOL0031C0B6")).isTrue()
        assertThat(
            E2eOnuSnResolver.matches(
                "TestTag=onu_item_0 Text=VSOL0031C0B6 Enabled=true",
                "12345B4641531C0B6",
            ),
        ).isTrue()
        assertThat(E2eOnuSnResolver.matches("HWTC15F5BB66", "12345B4641531C0B6")).isFalse()
    }

    @Test
    fun `matches zteg vendor prefix against smartolt hex form`() {
        val displayed = "5A544547DC47BFFD (ZTEG-DC47BFFD)"
        assertThat(E2eOnuSnResolver.matches(displayed, "ZTEGDC47BFFD")).isTrue()
        assertThat(E2eOnuSnResolver.matches(displayed, "5A544547DC47BFFD")).isTrue()
        assertThat(E2eOnuSnResolver.matches(displayed, "HWTCDEADBEEF")).isFalse()
    }

    @Test
    fun `matches authorization confirmation when manufacturer serial represents requested hex serial`() {
        assertThat(
            E2eOnuSnResolver.matchesAuthorizedOnuText(
                "ONU HWTC9F4BF950 autorizada y conectada con ACS.",
                "485754439F4BF950",
            ),
        ).isTrue()
        assertThat(
            E2eOnuSnResolver.matchesAuthorizedOnuText(
                "ONU HWTC9F4BF950 autorizada y conectada con ACS.",
                "48575443AABBCCDD",
            ),
        ).isFalse()
    }

    @Test
    fun `confirms authorization when banner and serial are separate nodes`() {
        val waitingForAcs = listOf(
            "Esperando conexión con ACS",
            "La ONU fue autorizada. El formulario estará listo cuando ACS reciba su primer mensaje TR-069.",
            "Serial: VSOL00324576",
        )
        val acsReady = listOf(
            "Equipo conectado",
            "La ONU ya respondió a ACS. Ya puedes completar los datos de la suscripción.",
            "Serial: VSOL00324576",
        )
        assertThat(E2eOnuSnResolver.isAuthorizationConfirmed(waitingForAcs, "VSOL00324576")).isTrue()
        assertThat(E2eOnuSnResolver.isAuthorizationConfirmed(acsReady, "VSOL00324576")).isTrue()
        assertThat(E2eOnuSnResolver.isAuthorizationConfirmed(acsReady, "VSOL00999999")).isFalse()
    }

    @Test
    fun `does not confirm authorization while the OLT check is pending`() {
        val pending = listOf(
            "Comprobando autorización en OLT",
            "La autorización todavía no se confirma. Puedes volver a comprobarla o cancelar y limpiar el intento.",
            "Serial: VSOL00324576",
        )
        assertThat(E2eOnuSnResolver.isAuthorizationConfirmed(pending, "VSOL00324576")).isFalse()
    }

    @Test
    fun `confirms authorization from the client step banners`() {
        val pending = listOf(
            "ONU VSOL00324576",
            "Autorizada en la OLT ✓ · conectando con la gestión remota…",
        )
        val ready = listOf(
            "Equipo conectado con la gestión remota ✓",
            "ONU VSOL00324576 lista para registrar.",
        )
        assertThat(E2eOnuSnResolver.isAuthorizationConfirmed(pending, "VSOL00324576")).isTrue()
        assertThat(E2eOnuSnResolver.isAuthorizationConfirmed(ready, "VSOL00324576")).isTrue()
        assertThat(E2eOnuSnResolver.isAuthorizationConfirmed(ready, "VSOL00999999")).isFalse()
    }
}
