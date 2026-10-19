package com.dscorp.ispadmin.presentation.theme

import android.app.Application
import android.content.Context
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class ThemeModeTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun `the chosen mode wins over the system`() {
        assertEquals(true, ThemeMode.SYSTEM.isDark(systemDark = true))
        assertEquals(false, ThemeMode.LIGHT.isDark(systemDark = true))
        assertEquals(true, ThemeMode.DARK.isDark(systemDark = false))
    }

    @Test
    fun `the preference is kept on the phone`() {
        val prefs = ApplicationProvider.getApplicationContext<Context>().getSharedPreferences("theme_test", Context.MODE_PRIVATE)
        val store = ThemePreferences(prefs)
        assertEquals(ThemeMode.SYSTEM, store.mode.value)

        store.set(ThemeMode.DARK)

        assertEquals(ThemeMode.DARK, store.mode.value)
        assertEquals(ThemeMode.DARK, ThemePreferences(prefs).mode.value)
    }

    @Test
    fun `every MyTheme below the root follows the chosen mode`() {
        compose.setContent {
            CompositionLocalProvider(LocalThemeMode provides ThemeMode.DARK) {
                MyTheme { Text(if (IspTheme.colors == DarkIspColors) "oscuro" else "claro") }
            }
        }

        compose.onNodeWithText("oscuro").assertExists()
    }
}
