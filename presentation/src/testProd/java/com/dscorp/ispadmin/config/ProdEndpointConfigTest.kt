package com.dscorp.ispadmin.config

import com.dscorp.ispadmin.BuildConfig
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ProdEndpointConfigTest {

    @Test
    fun `prod flavor usa el host canonico de produccion`() {
        assertThat(BuildConfig.BASE_URL)
            .isEqualTo("https://api.gigafiberperu.tech/ispadmin/")
        assertThat(BuildConfig.OBS_BASE_URL)
            .isEqualTo("https://api.gigafiberperu.tech/ispadmin/")
    }
}
