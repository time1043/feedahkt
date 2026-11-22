package com.time1043.feedah_kt.ui.screen.mode

import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTextReplacement
import com.time1043.feedah_kt.ui.theme.FeedahktTheme
import java.io.File
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import androidx.compose.ui.test.ExperimentalTestApi

/**
 * Layout screenshots of the search field, rendered on the JVM —
 * Robolectric's native-graphics pipeline draws the real compose tree, and
 * the bitmaps land in build/screenshots for the human (or the agent) to
 * LOOK at. The device stays in the loop only for the final eyeball.
 *
 * configuredScope is a single empty bucket name: it survives as the scope
 * (so the db is never opened), and an empty scope short-circuits the
 * debounced query — the field renders its states without touching Room.
 * autoFocus is off: the blinking cursor is an infinite animation, and an
 * idle-waiting test would never settle on a focused field.
 *
 * The capture is manual (decorView.draw onto a bitmap): compose's own
 * captureToImage waits for the window's redraw callback, which Robolectric's
 * paused main looper never delivers — pumping the looper ourselves and
 * drawing synchronously does.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-480dpi")
@OptIn(ExperimentalTestApi::class)
class SearchScreenScreenshotTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private fun capture(name: String) {
        val decor = composeRule.activity.window.decorView
        val bitmap = Bitmap.createBitmap(
            decor.width.coerceAtLeast(1),
            decor.height.coerceAtLeast(1),
            Bitmap.Config.ARGB_8888,
        )
        val canvas = Canvas(bitmap)
        canvas.drawColor(android.graphics.Color.WHITE)
        decor.draw(canvas)

        val dir = File("build/screenshots").apply { mkdirs() }
        dir.resolve("$name.png").outputStream().use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
    }

    @Test
    fun fieldStates() {
        composeRule.setContent {
            FeedahktTheme {
                SearchScreen(
                    bucketParam = null,
                    searchLocal = false,
                    configuredScope = listOf(""),
                    onBack = {},
                    onOpenWord = { _, _ -> },
                    autoFocus = false,
                )
            }
        }
        composeRule.waitForIdle()
        capture("search_empty")

        // performTextReplacement drives the SetText semantics action — no
        // IME focus needed (the field is unfocused here by design), so the
        // cursor never blinks and no idle wait can hang. The recomposition
        // applies on the TEST frame clock, which never advances on its own
        // — advance one frame before drawing.
        composeRule.onNodeWithTag("search.query").performTextReplacement("ab")
        composeRule.mainClock.advanceTimeByFrame()
        shadowOf(Looper.getMainLooper()).idle()
        capture("search_filled")
    }
}
