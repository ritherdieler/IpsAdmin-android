package com.dscorp.ispadmin.domain.usecase.subscription

import com.dscorp.ispadmin.domain.repository.PendingSubscriptionRepository
import com.dscorp.ispadmin.domain.storage.FacadePhotoStorage
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test

class DiscardPendingSubscriptionUseCaseTest {
    private val repository = mockk<PendingSubscriptionRepository>()
    private val photos = mockk<FacadePhotoStorage>()
    private val useCase = DiscardPendingSubscriptionUseCase(repository, photos)

    @Test
    fun `discarding removes the queued registration and its facade photo`() = runTest {
        coEvery { repository.deleteByLocalId("local-1") } returns Result.success(Unit)
        every { photos.delete("local-1") } returns true

        assertTrue(useCase("local-1").isSuccess)

        coVerify { repository.deleteByLocalId("local-1") }
        verify { photos.delete("local-1") }
    }

    @Test
    fun `the photo stays when the registration could not be removed`() = runTest {
        coEvery { repository.deleteByLocalId("local-1") } returns Result.failure(IllegalStateException("db"))

        assertTrue(useCase("local-1").isFailure)

        verify(exactly = 0) { photos.delete(any()) }
    }
}
