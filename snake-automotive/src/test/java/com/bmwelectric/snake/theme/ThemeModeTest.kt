package com.bmwelectric.snake.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemeModeTest {
    @Test
    fun fromStorageParsesDayAndDark() {
        assertEquals(ThemeMode.Day, ThemeMode.fromStorage("day"))
        assertEquals(ThemeMode.Dark, ThemeMode.fromStorage("DARK"))
        assertEquals(ThemeMode.Dark, ThemeMode.fromStorage(" Dark "))
    }

    @Test
    fun fromStorageTreatsMissingOrUnknownValueAsNoPreference() {
        assertNull(ThemeMode.fromStorage(null))
        assertNull(ThemeMode.fromStorage(""))
        assertNull(ThemeMode.fromStorage("   "))
        assertNull(ThemeMode.fromStorage("auto"))
        assertNull(ThemeMode.fromStorage("night"))
    }

    @Test
    fun storedValuesRoundTrip() {
        ThemeMode.entries.forEach { mode ->
            assertEquals(mode, ThemeMode.fromStorage(mode.storageValue))
        }
    }

    @Test
    fun noPreferenceFollowsSystemTheme() {
        assertFalse(resolveDayMode(manual = null, systemDarkMode = true))
        assertTrue(resolveDayMode(manual = null, systemDarkMode = false))
    }

    @Test
    fun manualPreferenceOverridesSystemTheme() {
        assertTrue(resolveDayMode(manual = ThemeMode.Day, systemDarkMode = true))
        assertTrue(resolveDayMode(manual = ThemeMode.Day, systemDarkMode = false))
        assertFalse(resolveDayMode(manual = ThemeMode.Dark, systemDarkMode = true))
        assertFalse(resolveDayMode(manual = ThemeMode.Dark, systemDarkMode = false))
    }
}
