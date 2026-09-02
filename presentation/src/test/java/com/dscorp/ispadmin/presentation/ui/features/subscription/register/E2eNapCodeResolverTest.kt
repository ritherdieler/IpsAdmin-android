package com.dscorp.ispadmin.presentation.ui.features.subscription.register

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class E2eNapCodeResolverTest {

    @Test
    fun `blank instrumentation arg is ignored`() {
        assertThat(E2eNapCodeResolver.resolve(null)).isNull()
        assertThat(E2eNapCodeResolver.resolve("")).isNull()
        assertThat(E2eNapCodeResolver.resolve("   ")).isNull()
    }

    @Test
    fun `matches staging nap code against autocomplete label`() {
        val displayed = "NO-001 / Cerca al hotel iquitos"
        assertThat(E2eNapCodeResolver.resolve("NO-001")).isEqualTo("NO-001")
        assertThat(E2eNapCodeResolver.matches(displayed, "NO-001")).isTrue()
        assertThat(E2eNapCodeResolver.matches("NO-001", "NO-001")).isTrue()
        assertThat(E2eNapCodeResolver.matches(displayed, "NO-999")).isFalse()
    }
}
