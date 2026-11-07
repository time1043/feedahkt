package com.time1043.feedah_kt.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Test

/** The full decision matrix of [resolveDarkTheme], no device needed. */
class ThemeTest {

    @Test
    fun light_forcesLight() {
        assertEquals(false, resolveDarkTheme("light", systemDark = true))
    }

    @Test
    fun dark_forcesDark() {
        assertEquals(true, resolveDarkTheme("dark", systemDark = false))
    }

    @Test
    fun system_followsOS() {
        assertEquals(true, resolveDarkTheme("system", systemDark = true))
        assertEquals(false, resolveDarkTheme("system", systemDark = false))
    }

    @Test
    fun unknown_fallsBackToSystem() {
        assertEquals(true, resolveDarkTheme("gibberish", systemDark = true))
    }
}
