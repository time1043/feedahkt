package com.time1043.feedah_kt.ui.screen.mode

import android.graphics.Bitmap
import android.util.Base64
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.core.app.ActivityScenario
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.Until
import com.time1043.feedah_kt.MainActivity
import java.io.ByteArrayOutputStream
import kotlin.math.max
import kotlin.math.min
import org.junit.Test
import org.junit.runner.RunWith

/** The theme segmented control, in device pixels, delivered through the
 * self-destructing payload (see SearchFieldDeviceScreenshotTest). The
 * crop anchors on the segment's own bounds; if the control is not on
 * screen at all the whole page comes back (scaled), so the miss is
 * visible too. */
@RunWith(AndroidJUnit4::class)
class SettingsDeviceScreenshotTest {

    private val ui = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

    @Test
    fun device_shot_settings() {
        ActivityScenario.launch(MainActivity::class.java)
        // A previous probe may have parked the app on a stack route — pop
        // back to the tabs (force-stop is not an option: the
        // instrumentation lives in the app's process and would kill
        // itself).
        var guard = 0
        while (ui.findObject(By.text("Search words")) != null && guard++ < 3) {
            ui.pressBack()
            Thread.sleep(300)
        }
        ui.wait(Until.hasObject(By.text("Settings")), 8000) // the bar's label
        ui.findObject(By.text("Settings")).click()
        Thread.sleep(800)

        if (ui.findObject(By.text("System")) == null) {
            ui.swipe(ui.displayWidth / 2, (ui.displayHeight * 3 / 4), ui.displayWidth / 2, ui.displayHeight / 4, 12)
            Thread.sleep(600)
        }

        val shot = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()!!
        val segment: UiObject2? =
            ui.findObject(By.text("Dark")) ?: ui.findObject(By.text("Light")) ?: ui.findObject(By.text("System"))
        val bitmap = if (segment != null) {
            val b = segment.visibleBounds
            val top = max(b.top - 160, 0)
            val bottom = min(b.bottom + 160, shot.height)
            val crop = Bitmap.createBitmap(shot, 0, top, shot.width, bottom - top)
            Bitmap.createScaledBitmap(crop, (crop.width * 0.6).toInt(), (crop.height * 0.6).toInt(), true)
        } else {
            Bitmap.createScaledBitmap(shot, (shot.width * 0.3).toInt(), (shot.height * 0.3).toInt(), true)
        }
        val bytes = ByteArrayOutputStream().also { bitmap.compress(Bitmap.CompressFormat.JPEG, 85, it) }.toByteArray()
        val b64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
        val tag = if (segment != null) "toggle" else "nosegments"
        throw AssertionError("DEVICE_SHOT[$tag]:$b64")
    }
}
