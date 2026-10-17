package com.dscorp.ispadmin.presentation.designsystem

import android.app.Application
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import com.dscorp.ispadmin.presentation.designsystem.charts.ChartData
import com.dscorp.ispadmin.presentation.designsystem.charts.ChartSeries
import com.dscorp.ispadmin.presentation.designsystem.charts.markerText
import com.dscorp.ispadmin.presentation.designsystem.components.BubbleSide
import com.dscorp.ispadmin.presentation.designsystem.components.KpiModel
import com.dscorp.ispadmin.presentation.designsystem.components.KpiTile
import com.dscorp.ispadmin.presentation.designsystem.components.bubblePlacement
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class, qualifiers = "w411dp-h900dp")
class InfoBubbleTest {
    @get:Rule val compose = createComposeRule()

    private val window = IntSize(1080, 2400)

    @Test
    fun `bubble sits above its anchor, centered and pointing at it`() {
        val placement = bubblePlacement(anchor = IntRect(500, 1000, 560, 1060), bubble = IntSize(400, 200), window = window, margin = 32, gap = 8)

        assertEquals(BubbleSide.ABOVE, placement.side)
        assertEquals(330, placement.x)
        assertEquals(792, placement.y)
        assertEquals(200, placement.tailX)
    }

    @Test
    fun `bubble stays inside the screen and its tail still points at the anchor`() {
        val placement = bubblePlacement(anchor = IntRect(990, 1000, 1050, 1060), bubble = IntSize(400, 200), window = window, margin = 32, gap = 8)

        assertEquals(648, placement.x)
        assertEquals(372, placement.tailX)
    }

    @Test
    fun `bubble opens below when there is no room above`() {
        val placement = bubblePlacement(anchor = IntRect(500, 100, 560, 160), bubble = IntSize(400, 200), window = window, margin = 32, gap = 8)

        assertEquals(BubbleSide.BELOW, placement.side)
        assertEquals(168, placement.y)
    }

    @Test
    fun `chart bubble titles the month and marks each series`() {
        val data = ChartData(
            labels = listOf("ago", "sep"),
            series = listOf(ChartSeries("Neto", Color.Blue, listOf(1.0, 49_000.0)), ChartSeries("Cobrado", Color.Green, listOf(2.0, 45_000.0))),
        )

        assertEquals("sep\n● Neto  49000.0\n● Cobrado  45000.0", markerText(data, 1) { it.toString() }.toString())
        assertEquals("jun\n863", markerText(ChartData(listOf("jun"), listOf(ChartSeries("Suscripciones", Color.Blue, listOf(863.0)))), 0) { "%.0f".format(it) }.toString())
    }

    @Test
    fun `info opens a floating bubble without stretching the tile`() {
        compose.setContent {
            MaterialTheme {
                KpiTile(KpiModel("Deuda por moroso", "S/ 60.58", "debt", info = "Deuda vencida promedio de los clientes morosos."))
            }
        }
        val before = compose.onNodeWithTag("debt").getUnclippedBoundsInRoot().let { it.bottom - it.top }

        compose.onNodeWithTag("debt_info").performClick()

        compose.onNodeWithText("Deuda vencida promedio de los clientes morosos.").assertExists()
        assertEquals(before, compose.onNodeWithTag("debt").getUnclippedBoundsInRoot().let { it.bottom - it.top })

        compose.onNodeWithTag("debt_info").performClick()
        compose.onNodeWithText("Deuda vencida promedio de los clientes morosos.").assertDoesNotExist()
    }
}
