package com.dscorp.ispadmin.presentation.designsystem.components

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.activity.ComponentActivity
import android.provider.Settings
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import com.dscorp.ispadmin.presentation.theme.LightIspColors
import com.dscorp.ispadmin.presentation.theme.MyTheme
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], application = Application::class)
class SkeletonTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    private val context get() = ApplicationProvider.getApplicationContext<Application>()

    private fun setAnimatorScale(scale: Float) {
        Settings.Global.putFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, scale)
    }

    @After fun restoreAnimations() = setAnimatorScale(1f)

    @Test
    fun `reduce motion is read from the animator duration scale`() {
        setAnimatorScale(0f)
        assertTrue(animationsDisabled(context))
        setAnimatorScale(1f)
        assertFalse(animationsDisabled(context))
    }

    @Test
    fun `with animations off every bone is flat skeleton-base, and card bones keep the card tone`() {
        setAnimatorScale(0f)
        compose.mainClock.autoAdvance = false
        compose.setContent {
            MyTheme(darkTheme = false) {
                SkeletonSweepProvider {
                    Column {
                        Box(Modifier.fillMaxWidth().height(48.dp).testTag("bone").skeleton(shape = RectangleShape))
                        Box(Modifier.fillMaxWidth().height(48.dp).testTag("card").skeleton(card = true, shape = RectangleShape))
                    }
                }
            }
        }
        // Mid-sweep, when the light would be on screen if it were running.
        compose.mainClock.advanceTimeBy(850)

        assertFlat("bone", LightIspColors.skeletonBase)
        assertFlat("card", LightIspColors.card)
    }

    @Test
    fun `with animations on the light moves across the bone`() {
        setAnimatorScale(1f)
        compose.mainClock.autoAdvance = false
        compose.setContent {
            MyTheme(darkTheme = false) {
                Box(Modifier.fillMaxWidth().height(48.dp).testTag("bone").skeleton(shape = RectangleShape))
            }
        }

        val frames = (0..8).map {
            compose.mainClock.advanceTimeBy(200)
            capture("bone").let { (bitmap, box) -> (box.left until box.right step 16).map { x -> bitmap.getPixel(x, box.centerY()) } }
        }
        assertTrue("some frame shows the highlight", frames.flatten().any { it != LightIspColors.skeletonBase.toArgb() })
        assertTrue("the sweep changes between frames", frames.distinct().size > 1)
    }

    private fun assertFlat(tag: String, expected: Color) {
        val (bitmap, box) = capture(tag)
        for (x in box.left until box.right step 8) for (y in box.top until box.bottom step 8) {
            assertEquals("$tag at $x,$y", expected.toArgb(), bitmap.getPixel(x, y))
        }
    }

    /** Robolectric has no PixelCopy, so draw the window ourselves and return the node's pixel box inside it. */
    private fun capture(tag: String): Pair<Bitmap, android.graphics.Rect> {
        compose.waitForIdle()
        val bounds = compose.onNodeWithTag(tag).fetchSemanticsNode().boundsInWindow
        val view = compose.activity.window.decorView
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        compose.runOnUiThread { view.draw(Canvas(bitmap)) }
        val box = android.graphics.Rect(bounds.left.toInt() + 1, bounds.top.toInt() + 1, bounds.right.toInt() - 1, bounds.bottom.toInt() - 1)
        return bitmap to box
    }
}
