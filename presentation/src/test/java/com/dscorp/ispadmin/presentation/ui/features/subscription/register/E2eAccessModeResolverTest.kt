package com.dscorp.ispadmin.presentation.ui.features.subscription.register

import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Test

class E2eAccessModeResolverTest {

    @After
    fun tearDown() {
        E2eAccessModeResolver.override = null
    }

    @Test
    fun `blank arg leaves backend default`() {
        assertThat(E2eAccessModeResolver.resolve(null)).isNull()
        assertThat(E2eAccessModeResolver.resolve("")).isNull()
        assertThat(E2eAccessModeResolver.resolve("   ")).isNull()
        assertThat(E2eAccessModeResolver.resolve("PPPOE_DYNAMIC")).isNull()
    }

    @Test
    fun `static ip override is accepted`() {
        assertThat(E2eAccessModeResolver.resolve("STATIC_IP")).isEqualTo("STATIC_IP")
        assertThat(E2eAccessModeResolver.resolve(" static_ip ")).isEqualTo("STATIC_IP")
        E2eAccessModeResolver.override = "STATIC_IP"
        assertThat(E2eAccessModeResolver.resolve()).isEqualTo("STATIC_IP")
    }
}
