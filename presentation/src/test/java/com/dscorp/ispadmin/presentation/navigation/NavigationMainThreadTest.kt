package com.dscorp.ispadmin.presentation.navigation

import com.dscorp.ispadmin.navigation.runNavigationOnMainThread
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertSame
import org.junit.Test

class NavigationMainThreadTest {

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `navigation after async result is resumed on main dispatcher`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val expectedThread = Thread.currentThread()
            val navigationThread = async(Dispatchers.Default) {
                runNavigationOnMainThread { Thread.currentThread() }
            }.await()

            assertSame("Navigation must resume on the main dispatcher", expectedThread, navigationThread)
        } finally {
            Dispatchers.resetMain()
        }
    }
}
