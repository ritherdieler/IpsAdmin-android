package com.dscorp.ispadmin.presentation.ui.features.subscriptionfinder.compose

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.dscorp.ispadmin.domain.model.ServiceStatus
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class SharedPrefsRecentSubscriptionsStoreTest {

    private val prefs = ApplicationProvider.getApplicationContext<Application>()
        .getSharedPreferences("recents_test", Context.MODE_PRIVATE)

    @Test
    fun `recents survive a new store instance`() {
        val items = listOf(
            RecentSubscription(1842, "Jorge Ramos Villanueva", "Basico · La Villa", ServiceStatus.CUT_OFF),
            RecentSubscription(7, "Ana Paredes", "Duo basico 80 · 9 De Octubre", ServiceStatus.ACTIVE),
        )

        SharedPrefsRecentSubscriptionsStore(prefs).save(items)

        assertEquals(items, SharedPrefsRecentSubscriptionsStore(prefs).load())
    }

    @Test
    fun `missing or broken data reads as no recents`() {
        assertEquals(emptyList<RecentSubscription>(), SharedPrefsRecentSubscriptionsStore(prefs).load())

        prefs.edit().putString(SharedPrefsRecentSubscriptionsStore.KEY, "{not json").commit()

        assertEquals(emptyList<RecentSubscription>(), SharedPrefsRecentSubscriptionsStore(prefs).load())
    }
}
