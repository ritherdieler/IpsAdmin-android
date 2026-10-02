package com.dscorp.ispadmin.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.dscorp.ispadmin.data.local.IspAdminDatabase
import com.dscorp.ispadmin.data.remote.RegistrationCatalogRemoteDataSource
import com.dscorp.ispadmin.domain.exception.CatalogNotAvailableOffline
import com.dscorp.ispadmin.domain.model.CatalogCoreDevice
import com.dscorp.ispadmin.domain.model.CatalogNapBox
import com.dscorp.ispadmin.domain.model.CatalogOnu
import com.dscorp.ispadmin.domain.model.CatalogPlace
import com.dscorp.ispadmin.domain.model.CatalogPlan
import com.dscorp.ispadmin.domain.repository.RegistrationCatalogRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import java.io.IOException
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RegistrationCatalogRepositoryTest {

    private lateinit var database: IspAdminDatabase
    private lateinit var remote: RegistrationCatalogRemoteDataSource
    private lateinit var repository: RegistrationCatalogRepository

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext<android.content.Context>(),
            IspAdminDatabase::class.java
        ).allowMainThreadQueries().build()
        remote = mockk()
        repository = RegistrationCatalogRepositoryImpl(database.catalogDao(), remote)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `getCachedCatalog fails when room has never been synced`() = runTest {
        val result = repository.getCachedCatalog()

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is CatalogNotAvailableOffline)
    }

    @Test
    fun `refreshFromRemote persists the five remote sources including core devices`() = runTest {
        stubRemoteCatalog()

        val result = repository.refreshFromRemote()

        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { remote.fetchPlans() }
        coVerify(exactly = 1) { remote.fetchPlaces() }
        coVerify(exactly = 1) { remote.fetchNapBoxes() }
        coVerify(exactly = 1) { remote.fetchOnus() }
        coVerify(exactly = 1) { remote.fetchCoreDevices() }

        val cached = repository.getCachedCatalog().getOrThrow()
        assertEquals(listOf("plan-1"), cached.plans.map { it.id })
        assertEquals(listOf("place-1"), cached.places.map { it.id })
        assertEquals(listOf("nap-1"), cached.napBoxes.map { it.id })
        assertEquals(listOf("ONU123"), cached.onus.map { it.sn })
        assertEquals(listOf(10), cached.coreDevices.map { it.id })
        assertEquals("Core-Huacho", cached.coreDevices.first().name)
    }

    @Test
    fun `OLT ONU outage still persists independent catalogs and no stale ONU candidates`() = runTest {
        coEvery { remote.fetchPlans() } returns listOf(
            CatalogPlan(id = "plan-1", name = "100 Mbps", type = "FIBER")
        )
        coEvery { remote.fetchPlaces() } returns listOf(CatalogPlace(id = "place-1", name = "Huacho"))
        coEvery { remote.fetchNapBoxes() } returns listOf(
            CatalogNapBox(id = "nap-1", code = "NAP-01", placeName = "Huacho", placeId = 1)
        )
        coEvery { remote.fetchOnus() } throws IOException("OLT list unavailable")
        coEvery { remote.fetchCoreDevices() } returns listOf(
            CatalogCoreDevice(id = 10, name = "Core-Huacho", disabled = false)
        )

        val refresh = repository.refreshFromRemote()

        assertTrue(refresh.isFailure)
        val cached = repository.getCachedCatalog().getOrThrow()
        assertEquals(listOf("plan-1"), cached.plans.map { it.id })
        assertEquals(listOf("place-1"), cached.places.map { it.id })
        assertEquals(listOf("nap-1"), cached.napBoxes.map { it.id })
        assertTrue(cached.onus.isEmpty())
        assertEquals(listOf(10), cached.coreDevices.map { it.id })
    }

    @Test
    fun `OLT ONU outage clears a previously cached serial from available candidates`() = runTest {
        stubRemoteCatalog()
        repository.refreshFromRemote()
        coEvery { remote.fetchOnus() } throws IOException("OLT list unavailable")

        val refresh = repository.refreshFromRemote()

        assertTrue(refresh.isFailure)
        val cached = repository.getCachedCatalog().getOrThrow()
        assertTrue(cached.onus.isEmpty())
        assertEquals(listOf("plan-1"), cached.plans.map { it.id })
        assertEquals(listOf(10), cached.coreDevices.map { it.id })
    }

    @Test
    fun `getCachedCatalog reads previously persisted room data`() = runTest {
        stubRemoteCatalog()
        repository.refreshFromRemote()

        val result = repository.getCachedCatalog()

        assertTrue(result.isSuccess)
        val catalog = result.getOrThrow()
        assertTrue(catalog.coreDevices.isNotEmpty())
        assertEquals("100 Mbps", catalog.plans.first().name)
        assertEquals("Huacho", catalog.places.first().name)
    }

    @Test
    fun `observeLastSync emits timestamp after successful refresh`() = runTest {
        stubRemoteCatalog()
        assertEquals(null, repository.observeLastSync().first())

        repository.refreshFromRemote()

        val lastSync = repository.observeLastSync().first()
        assertTrue(lastSync != null && lastSync > 0L)
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
