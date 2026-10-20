package com.dscorp.ispadmin.data.repository

import com.dscorp.ispadmin.data.remote.RegistrationCatalogRemoteDataSource
import com.dscorp.ispadmin.domain.model.CatalogCoreDevice
import com.dscorp.ispadmin.domain.model.CatalogNapBox
import com.dscorp.ispadmin.domain.model.CatalogOnu
import com.dscorp.ispadmin.domain.model.CatalogPlace
import com.dscorp.ispadmin.domain.model.CatalogPlan
import com.dscorp.ispadmin.domain.repository.RegistrationCatalogRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import java.io.IOException

class RegistrationCatalogRepositoryTest {

    private lateinit var remote: RegistrationCatalogRemoteDataSource
    private lateinit var repository: RegistrationCatalogRepository

    @Before
    fun setUp() {
        remote = mockk()
        repository = RegistrationCatalogRepositoryImpl(remote)
    }

    @Test
    fun `fetchCatalog maps plans, places, nap boxes, onus and core devices`() = runTest {
        stubRemoteCatalog()

        val result = repository.fetchCatalog()

        assertTrue(result.isSuccess)
        val catalog = result.getOrThrow()
        assertEquals(listOf("plan-1"), catalog.plans.map { it.id })
        assertEquals("100 Mbps", catalog.plans.first().name)
        assertEquals(listOf("place-1"), catalog.places.map { it.id })
        assertEquals("Huacho", catalog.places.first().name)
        assertEquals(listOf("nap-1"), catalog.napBoxes.map { it.id })
        assertEquals(listOf("ONU123"), catalog.onus.map { it.sn })
        assertEquals(listOf(10), catalog.coreDevices.map { it.id })
        assertEquals("Core-Huacho", catalog.coreDevices.first().name)
        coVerify(exactly = 1) { remote.fetchPlans() }
        coVerify(exactly = 1) { remote.fetchPlaces() }
        coVerify(exactly = 1) { remote.fetchNapBoxes() }
        coVerify(exactly = 1) { remote.fetchOnus() }
        coVerify(exactly = 1) { remote.fetchCoreDevices() }
    }

    @Test
    fun `a failing plans fetch returns failure`() = runTest {
        stubRemoteCatalog()
        coEvery { remote.fetchPlans() } throws IOException("plans unavailable")

        val result = repository.fetchCatalog()

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IOException)
    }

    @Test
    fun `a failing places fetch returns failure`() = runTest {
        stubRemoteCatalog()
        coEvery { remote.fetchPlaces() } throws IOException("places unavailable")

        val result = repository.fetchCatalog()

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IOException)
    }

    @Test
    fun `a failing nap boxes fetch returns failure`() = runTest {
        stubRemoteCatalog()
        coEvery { remote.fetchNapBoxes() } throws IOException("nap boxes unavailable")

        val result = repository.fetchCatalog()

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IOException)
    }

    @Test
    fun `a failing onu fetch gives an empty onu list and succeeds`() = runTest {
        stubRemoteCatalog()
        coEvery { remote.fetchOnus() } throws IOException("OLT list unavailable")

        val result = repository.fetchCatalog()

        assertTrue(result.isSuccess)
        val catalog = result.getOrThrow()
        assertTrue(catalog.onus.isEmpty())
        assertEquals(listOf("plan-1"), catalog.plans.map { it.id })
        assertEquals(listOf(10), catalog.coreDevices.map { it.id })
    }

    @Test
    fun `a failing core devices fetch gives an empty list and succeeds`() = runTest {
        stubRemoteCatalog()
        coEvery { remote.fetchCoreDevices() } throws IOException("core devices unavailable")

        val result = repository.fetchCatalog()

        assertTrue(result.isSuccess)
        val catalog = result.getOrThrow()
        assertTrue(catalog.coreDevices.isEmpty())
        assertEquals(listOf("plan-1"), catalog.plans.map { it.id })
        assertEquals(listOf("ONU123"), catalog.onus.map { it.sn })
    }

    @Test
    fun `cancellation is rethrown`() = runTest {
        stubRemoteCatalog()
        coEvery { remote.fetchPlans() } throws CancellationException("cancelled")

        try {
            repository.fetchCatalog()
            fail("Expected CancellationException")
        } catch (expected: CancellationException) {
            // expected
        }
    }

    private fun stubRemoteCatalog() {
        coEvery { remote.fetchPlans() } returns listOf(
            CatalogPlan(id = "plan-1", name = "100 Mbps", price = 80.0, type = "FIBER")
        )
        coEvery { remote.fetchPlaces() } returns listOf(
            CatalogPlace(id = "place-1", name = "Huacho")
        )
        coEvery { remote.fetchNapBoxes() } returns listOf(
            CatalogNapBox(id = "nap-1", code = "NAP-01", placeName = "Huacho", placeId = 1)
        )
        coEvery { remote.fetchOnus() } returns listOf(
            CatalogOnu(sn = "ONU123", board = "1", oltId = "olt-1")
        )
        coEvery { remote.fetchCoreDevices() } returns listOf(
            CatalogCoreDevice(id = 10, name = "Core-Huacho", disabled = false)
        )
    }
}
