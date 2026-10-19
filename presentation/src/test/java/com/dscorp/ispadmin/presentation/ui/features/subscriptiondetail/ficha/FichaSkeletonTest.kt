package com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.ficha

import android.app.Application
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class, qualifiers = "w390dp-h900dp")
class FichaSkeletonTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun `the ficha keeps its shape while it loads and can be left`() {
        var back = false
        compose.setContent { MaterialTheme { FichaSkeleton(onBack = { back = true }) } }

        compose.onNodeWithContentDescription("Cargando suscripción…").assertExists()
        compose.onNodeWithText("Resumen").assertExists()
        compose.onNodeWithContentDescription("Atrás").performClick()
        assertEquals(true, back)
    }
}
