package com.time1043.feedah_kt.data.settings

import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.preferencesOf
import org.junit.Assert.assertEquals
import org.junit.Test

/** Preferences -> Settings mapping, pure JVM (no DataStore file needed). */
class UserPreferencesRepositoryTest {

    @Test
    fun theme_key_overrides_default() {
        val prefs = preferencesOf(THEME_KEY to "dark")

        assertEquals("dark", prefs.toSettings().theme)
    }

    @Test
    fun missing_keys_fall_back_to_defaults() {
        assertEquals(Settings(), emptyPreferences().toSettings())
    }
}
