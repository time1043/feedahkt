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
import org.junit.Test
import org.junit.runner.RunWith

/** The bottom-nav strip, in device pixels, delivered through the
 * self-destructing payload (see SearchFieldDeviceScreenshotTest). */
@RunWith(AndroidJUnit4::class)
class NavBarDeviceScreenshotTest {

    private val ui = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

    @Test
    fun device_shot_navbar() {
        ActivityScenario.launch(MainActivity::class.java)
        ui.wait(Until.hasObject(By.desc("Search words")), 8000) // Home is up
        Thread.sleep(800) // insets + the bar settle
        val shot = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()!!
        val h = shot.height
        val strip = Bitmap.createBitmap(shot, 0, h - 340, shot.width, 340)
        val bytes = ByteArrayOutputStream().also { strip.compress(Bitmap.CompressFormat.PNG, 100, it) }.toByteArray()
        val b64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
        throw AssertionError("DEVICE_SHOT[navbar]:$b64")
    }
}
