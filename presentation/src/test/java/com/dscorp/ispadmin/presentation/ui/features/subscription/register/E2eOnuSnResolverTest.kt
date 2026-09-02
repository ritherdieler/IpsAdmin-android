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
    fun `matches zteg vendor prefix against smartolt hex form`() {
        val displayed = "5A544547DC47BFFD (ZTEG-DC47BFFD)"
        assertThat(E2eOnuSnResolver.matches(displayed, "ZTEGDC47BFFD")).isTrue()
        assertThat(E2eOnuSnResolver.matches(displayed, "5A544547DC47BFFD")).isTrue()
        assertThat(E2eOnuSnResolver.matches(displayed, "HWTCDEADBEEF")).isFalse()
    }
}
