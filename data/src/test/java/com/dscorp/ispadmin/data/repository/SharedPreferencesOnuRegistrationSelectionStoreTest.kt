package com.dscorp.ispadmin.data.repository

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SharedPreferencesOnuRegistrationSelectionStoreTest {
    private lateinit var preferences: android.content.SharedPreferences
    private lateinit var store: SharedPreferencesOnuRegistrationSelectionStore

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        preferences = context.getSharedPreferences("onu-registration-selection-test", Context.MODE_PRIVATE)
        preferences.edit().clear().commit()
        store = SharedPreferencesOnuRegistrationSelectionStore(preferences)
    }

    @Test
    fun `persists selected ONU per operator and clears only that operator`() = runTest {
        store.saveSelectedOnuSerial(11, "ONU-11")
        store.saveSelectedOnuSerial(12, "ONU-12")

        val reopenedStore = SharedPreferencesOnuRegistrationSelectionStore(preferences)
        reopenedStore.clearSelectedOnuSerial(11)

        assertNull(reopenedStore.getSelectedOnuSerial(11))
        assertEquals("ONU-12", reopenedStore.getSelectedOnuSerial(12))
    }
}
