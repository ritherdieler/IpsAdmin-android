package com.dscorp.ispadmin.domain.usecase.catalog

import com.dscorp.ispadmin.domain.model.CatalogCoreDevice
import com.dscorp.ispadmin.domain.model.CatalogNapBox
import com.dscorp.ispadmin.domain.model.CatalogOnu
import com.dscorp.ispadmin.domain.model.CatalogPlace
import com.dscorp.ispadmin.domain.model.CatalogPlan
import com.dscorp.ispadmin.domain.model.RegistrationCatalog
import com.dscorp.ispadmin.domain.repository.RegistrationCatalogRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException

class GetRegistrationCatalogUseCaseTest {

    private lateinit var repository: RegistrationCatalogRepository
    private lateinit var useCase: GetRegistrationCatalogUseCase

    @Before
    fun setUp() {
        repository = mockk()
        useCase = GetRegistrationCatalogUseCase(repository)
    }

    @Test
    fun `invoke returns the repository result`() = runTest {
        val catalog = populatedCatalog()
        coEvery { repository.fetchCatalog() } returns Result.success(catalog)

        val success = useCase()

        assertTrue(success.isSuccess)
        assertSame(catalog, success.getOrThrow())

        val error = IOException("api down")
        coEvery { repository.fetchCatalog() } returns Result.failure(error)

        val failure = useCase()

        assertTrue(failure.isFailure)
        assertSame(error, failure.exceptionOrNull())
        coVerify(exactly = 2) { repository.fetchCatalog() }
    }

    @Test
    fun `invoke does not reject a catalog without core devices`() = runTest {
        val catalog = populatedCatalog().copy(coreDevices = emptyList())
        coEvery { repository.fetchCatalog() } returns Result.success(catalog)

        val result = useCase()

        assertTrue(result.isSuccess)
        assertEquals(emptyList<CatalogCoreDevice>(), result.getOrThrow().coreDevices)
    }

    private fun populatedCatalog() = RegistrationCatalog(
        plans = listOf(CatalogPlan(id = "plan-1", name = "100 Mbps")),
        places = listOf(CatalogPlace(id = "place-1", name = "Huacho")),
        napBoxes = listOf(CatalogNapBox(id = "nap-1", code = "NAP-01")),
        onus = listOf(CatalogOnu(sn = "ONU123")),
        coreDevices = listOf(CatalogCoreDevice(id = 1, name = "Core-1"))
    )
}
