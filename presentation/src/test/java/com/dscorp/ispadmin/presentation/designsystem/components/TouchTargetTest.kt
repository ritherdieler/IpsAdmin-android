package com.dscorp.ispadmin.presentation.designsystem.components

import android.app.Application
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasParent
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.dscorp.ispadmin.presentation.theme.MyTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Design system rule: every tappable control offers at least a 48 dp touch target, even when it is drawn smaller.
 * Touch bounds are the ones Compose uses for hit testing and for the accessibility node bounds.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class, qualifiers = "w411dp-h900dp")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class TouchTargetTest {
    @get:Rule val compose = createComposeRule()

    private var taps = 0

    private fun show(content: @Composable () -> Unit) = compose.setContent {
        MyTheme { Box(Modifier.padding(MARGIN)) { content() } }
    }

    @Test
    fun `hero tabs are drawn compact but take 48dp touches`() {
        show { HeroTabs(listOf("Resumen", "Comercial"), selected = 0, onSelect = { taps++ }) }

        compose.onNodeWithTag("dashboard_tab_1").assertTouchTargetIsAtLeast48().assertTapJustOutsideTheDrawingCounts()
    }

    @Test
    fun `hero chips are drawn compact but take 48dp touches`() {
        show { HeroChips(listOf(HeroChip("Mes", selected = true, tag = "chip_month") { taps++ }, HeroChip("Año", selected = false, tag = "chip_year") {})) }

        compose.onNodeWithTag("chip_month").assertTouchTargetIsAtLeast48().assertTapJustOutsideTheDrawingCounts()
        compose.onNodeWithTag("chip_year").assertTouchTargetIsAtLeast48()
    }

    @Test
    fun `info button takes 48dp touches without growing the row that holds it`() {
        show {
            Row(Modifier.testTag("info_row")) {
                Text("Deuda")
                InfoBubbleButton(text = "Detalle", contentDescription = "Qué es deuda", modifier = Modifier.testTag("info"))
            }
        }

        compose.onNode(hasClickAction() and (hasTestTag("info") or hasParent(hasTestTag("info")))).assertTouchTargetIsAtLeast48()
        compose.onNodeWithTag("info").performTouchInput { click(Offset(centerX, -(MIN_TOUCH_TARGET - INFO_SIZE).toPx() / 2 + 1.dp.toPx())) }
        compose.onNodeWithTag(INFO_BUBBLE_TAG).assertExists()
        assertTrue("row grew to hold the touch target", compose.onNodeWithTag("info_row").heightDp() <= INFO_SIZE)
    }

    private fun SemanticsNodeInteraction.assertTouchTargetIsAtLeast48(): SemanticsNodeInteraction {
        val node = fetchSemanticsNode()
        with(node.layoutInfo.density) {
            val touch = node.touchBoundsInRoot
            assertTrue("touch width ${touch.width.toDp()} < $MIN_TOUCH_TARGET", touch.width.toDp() >= MIN_TOUCH_TARGET)
            assertTrue("touch height ${touch.height.toDp()} < $MIN_TOUCH_TARGET", touch.height.toDp() >= MIN_TOUCH_TARGET)
        }
        return this
    }

    /** A tap 1 dp inside the 48 dp target but outside the drawn control still reaches it; one past it does not. */
    private fun SemanticsNodeInteraction.assertTapJustOutsideTheDrawingCounts() {
        val slack = (MIN_TOUCH_TARGET - heightDp()) / 2
        assertTrue("control is already 48dp tall; nothing to check", slack > 1.dp)
        performTouchInput { click(Offset(centerX, -slack.toPx() - 2.dp.toPx())) }
        assertEquals("tap beyond the 48dp target counted", 0, taps)
        performTouchInput { click(Offset(centerX, -slack.toPx() + 1.dp.toPx())) }
        assertEquals("tap inside the 48dp target missed", 1, taps)
    }

    private fun SemanticsNodeInteraction.heightDp(): Dp {
        val node = fetchSemanticsNode()
        return with(node.layoutInfo.density) { node.boundsInRoot.height.toDp() }
    }

    private companion object {
        val MIN_TOUCH_TARGET = 48.dp
        val MARGIN = 24.dp
        /** Drawn size of the info button; its touch target overflows instead of stretching the row. */
        val INFO_SIZE = 28.dp
    }
}
