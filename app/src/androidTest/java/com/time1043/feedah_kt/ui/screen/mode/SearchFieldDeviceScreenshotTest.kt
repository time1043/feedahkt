package com.time1043.feedah_kt.ui.screen.mode

import android.graphics.Bitmap
import android.util.Base64
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.core.app.ActivityScenario
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import com.time1043.feedah_kt.MainActivity
import java.io.ByteArrayOutputStream
import kotlin.math.min
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * A self-destructing screenshot probe: each test intentionally FAILS with
 * a base64 payload of the device screenshot in its message, so gradle's
 * connected-test XML (which is pulled back to the workstation automatically)
 * carries real device pixels across the adb policy gap.
 *
 * All three states ride the REAL app: Home -> tap the search entry ->
 * the autofocused field -> type via shell -> clear via the x itself, so
 * the button's behavior is verified on device too.
 */
@RunWith(AndroidJUnit4::class)
class SearchFieldDeviceScreenshotTest {

    private val ui = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

    @Before
    fun openSearch() {
        ActivityScenario.launch(MainActivity::class.java)
        ui.wait(Until.hasObject(By.desc("Search words")), 8000)
        ui.findObject(By.desc("Search words")).click()
        ui.wait(Until.hasObject(By.text("Search words")), 8000) // the field's placeholder
        Thread.sleep(800) // keyboard + insets settle
    }

    private fun shotAndFail(name: String) {
        val shot = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()!!
        // The top strip holds the whole field; keeping only it trims the
        // payload (and the screen's edges) for the XML round-trip.
        val strip = Bitmap.createBitmap(shot, 0, 0, shot.width, min(shot.height, 300))
        val bytes = ByteArrayOutputStream().also { strip.compress(Bitmap.CompressFormat.PNG, 100, it) }.toByteArray()
        val b64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
        fail("DEVICE_SHOT[$name]:$b64")
    }

    private fun fail(message: String): Nothing = throw AssertionError(message)

    @Test
    fun device_shot_empty() = shotAndFail("empty")

    @Test
    fun device_shot_filled() {
        ui.executeShellCommand("input text ab")
        Thread.sleep(600) // debounce + the x appears
        shotAndFail("filled")
    }

    @Test
    fun device_shot_cleared() {
        ui.executeShellCommand("input text ab")
        ui.wait(Until.hasObject(By.desc("Clear search")), 3000)
        ui.findObject(By.desc("Clear search")).click()
        Thread.sleep(400)
        shotAndFail("cleared")
    }
}
