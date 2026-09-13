package com.dscorp.ispadmin.presentation.ui.features.subscription.register

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.io.File

class E2eLocalPrestagingScriptTest {

    private val script = File("../scripts/e2e_register_fiber_local_prestaging.sh")
    private val localEspresso = File("../scripts/e2e_register_fiber_local_espresso.sh")
    private val stagingEspresso = File("../scripts/e2e_register_fiber_staging_espresso.sh")

    @Test
    fun `prestaging wrapper delegates to backend curl script without breaking espresso`() {
        assertThat(script.exists()).isTrue()
        val text = script.readText()
        assertThat(text).contains("e2e_register_fiber_local_prestaging.sh")
        assertThat(text).contains("ispadmin-backend")
        assertThat(text).doesNotContain("8091")
        assertThat(text).doesNotContain("ispadmin-staging-acs")
        assertThat(text).doesNotContain("ispadmin_dev")
        assertThat(text).doesNotContain("connectedDevDebugAndroidTest")
        assertThat(text).doesNotContain("connectedStagingDebugAndroidTest")
        val local = localEspresso.readText()
        val staging = stagingEspresso.readText()
        assertThat(local).doesNotContain("E2E_PRESTAGING")
        assertThat(staging).doesNotContain("E2E_PRESTAGING")
        assertThat(local).contains("connectedDevDebugAndroidTest")
        assertThat(staging).contains("connectedStagingDebugAndroidTest")
    }
}
