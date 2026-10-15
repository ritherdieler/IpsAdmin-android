package com.dscorp.ispadmin.presentation.ui.features.login

import com.dscorp.ispadmin.BuildConfig
import com.dscorp.ispadmin.data.repository.IRepository
import com.dscorp.ispadmin.domain.model.AppVersion
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelVersionCheckTest {

    private val testDispatcher = StandardTestDispatcher()
    private val repository: IRepository = mockk(relaxed = true)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `newer remote version forces update with its download url`() = runTest(testDispatcher) {
        coEvery { repository.getRemoteAppVersion() } returns appVersion(
            versionCode = BuildConfig.VERSION_CODE + 1,
            downloadUrl = "https://appdistribution.firebase.dev/i/example",
        )
        val viewModel = LoginViewModel(repository, observabilityClient = mockk(relaxed = true))

        viewModel.checkAppVersion()
        advanceUntilIdle()

        assertEquals(
            CheckVersionState.CheckVersionSuccess(
                forceUpdate = true,
                downloadUrl = "https://appdistribution.firebase.dev/i/example",
            ),
            viewModel.checkVersionFlow.value,
        )
    }

    private fun appVersion(versionCode: Int, downloadUrl: String?) = AppVersion(
        id = 1,
        versionCode = versionCode,
        releaseDate = "2026-10-14",
        downloadUrl = downloadUrl,
    )
}
