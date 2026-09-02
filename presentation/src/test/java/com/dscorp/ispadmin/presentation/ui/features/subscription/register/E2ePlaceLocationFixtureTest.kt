package com.dscorp.ispadmin.presentation.ui.features.subscription.register

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.io.File

class E2ePlaceLocationFixtureTest {

    @Test
    fun `fixture uses stored 9 de octubre coordinates inside prod polygon`() {
        assertThat(E2ePlaceLocationFixture.PLACE_ID).isEqualTo(1)
        assertThat(E2ePlaceLocationFixture.PLACE_NAME).isEqualTo("9 de octubre")
        assertThat(E2ePlaceLocationFixture.LATITUDE).isEqualTo("-11.2156")
        assertThat(E2ePlaceLocationFixture.LONGITUDE).isEqualTo("-77.4107")
        assertThat(E2ePlaceLocationFixture.NEARBY_NAP_CODE).isEqualTo("NO-001")
        assertThat(E2ePlaceLocationFixture.pastedCoordinates()).isEqualTo("-11.2156, -77.4107")
    }

    @Test
    fun `staging espresso script and e2e test default to the polygon fixture`() {
        val script = File("../scripts/e2e_register_fiber_staging_espresso.sh").readText()
        assertThat(script).contains("GEO_LAT=\"\${GEO_LAT:-${E2ePlaceLocationFixture.LATITUDE}}\"")
        assertThat(script).contains("GEO_LON=\"\${GEO_LON:-${E2ePlaceLocationFixture.LONGITUDE}}\"")
        assertThat(script).contains("E2E_PLACE=\"\${E2E_PLACE:-${E2ePlaceLocationFixture.PLACE_NAME}}\"")
        assertThat(script).contains("place/findByLocation")
        assertThat(script).contains("napbox/near")
        assertThat(script).contains("GEO fuera de todo place.area")
        val localScript = File("../scripts/e2e_register_fiber_espresso.sh").readText()
        assertThat(localScript).contains("GEO_LAT=\"\${GEO_LAT:-${E2ePlaceLocationFixture.LATITUDE}}\"")
        assertThat(localScript).contains("GEO_LON=\"\${GEO_LON:-${E2ePlaceLocationFixture.LONGITUDE}}\"")
        assertThat(localScript).doesNotContain("GEO_LAT=\"\${GEO_LAT:--11.2177}\"")
        val e2eSource = File(
            "src/androidTest/java/com/dscorp/ispadmin/presentation/ui/features/subscription/register/FiberRegisterFirstOnuE2ETest.kt"
        ).readText()
        assertThat(e2eSource).contains("E2ePlaceLocationFixture.PLACE_NAME")
        assertThat(e2eSource).contains("E2ePlaceLocationFixture.LATITUDE")
        assertThat(e2eSource).contains("E2ePlaceLocationFixture.LONGITUDE")
    }
}
