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
}
