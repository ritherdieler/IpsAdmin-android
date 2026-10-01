package com.dscorp.ispadmin.data.repository

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.dscorp.ispadmin.domain.model.OnuRegistrationCancellationIntent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SharedPreferencesOnuRegistrationCancellationIntentStoreTest {
    private lateinit var preferences: android.content.SharedPreferences
    private lateinit var store: SharedPreferencesOnuRegistrationCancellationIntentStore

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        preferences = context.getSharedPreferences("onu-cancellation-intent-test", Context.MODE_PRIVATE)
        preferences.edit().clear().commit()
        store = SharedPreferencesOnuRegistrationCancellationIntentStore(preferences)
    }

    @Test
    fun `persists cancellation intent per operator and clears only the matching request`() = runTest {
        val first = OnuRegistrationCancellationIntent(11, "request-1", "ZTEGDC47BFFD")
        val second = OnuRegistrationCancellationIntent(12, "request-2", "VSOL0031C0B6", "operation-2")

        store.save(first)
        store.save(second)
        store.clear(first.operatorId, "different-request")

        assertEquals(first, store.get(first.operatorId))
        assertEquals(second, store.get(second.operatorId))

        store.clear(first.operatorId, first.requestKey)

        assertNull(store.get(first.operatorId))
        assertEquals(second, store.get(second.operatorId))
    }
}
