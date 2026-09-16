package com.dscorp.ispadmin.presentation.ui.features.subscription.register

import com.dscorp.ispadmin.domain.model.AccessMode
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Test

class E2eAccessModeResolverTest {

    @After
    fun tearDown() {
        E2eAccessModeResolver.override = null
    }

    @Test
    fun `blank arg defaults to PPPoE`() {
        assertThat(E2eAccessModeResolver.resolve(null)).isEqualTo(AccessMode.PPPOE_DYNAMIC)
        assertThat(E2eAccessModeResolver.resolve("")).isEqualTo(AccessMode.PPPOE_DYNAMIC)
        assertThat(E2eAccessModeResolver.resolve("   ")).isEqualTo(AccessMode.PPPOE_DYNAMIC)
        assertThat(E2eAccessModeResolver.resolve("pppoe")).isEqualTo(AccessMode.PPPOE_DYNAMIC)
        assertThat(E2eAccessModeResolver.resolve("PPPOE_DYNAMIC")).isEqualTo(AccessMode.PPPOE_DYNAMIC)
    }

    @Test
    fun `static aliases resolve to STATIC_IP`() {
        assertThat(E2eAccessModeResolver.resolve("static")).isEqualTo(AccessMode.STATIC_IP)
        assertThat(E2eAccessModeResolver.resolve("STATIC_IP")).isEqualTo(AccessMode.STATIC_IP)
        assertThat(E2eAccessModeResolver.resolve(" static_ip ")).isEqualTo(AccessMode.STATIC_IP)
        E2eAccessModeResolver.override = "static"
        assertThat(E2eAccessModeResolver.resolve()).isEqualTo(AccessMode.STATIC_IP)
    }

    @Test
    fun `pppoe fixed is accepted when passed`() {
        assertThat(E2eAccessModeResolver.resolve("PPPOE_FIXED")).isEqualTo(AccessMode.PPPOE_FIXED)
    }
}
