package com.bmwelectric.snake

import android.content.Context
import android.content.res.Configuration
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.bmwelectric.snake.theme.THEME_PREFS_NAME
import com.bmwelectric.snake.theme.THEME_PREFERENCE_KEY
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class SnakeAutomotiveUiTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val context: Context get() = ApplicationProvider.getApplicationContext()

    @Before
    fun clearThemePreferences() {
        runCatching { themePreferences().edit().clear().commit() }
    }

    @Test
    fun readyStateShowsParkedStartAndBoard() {
        composeRule.setContent {
            SnakeAutomotiveApp()
        }

        composeRule.onNodeWithText("Snake Parked Mode").assertIsDisplayed()
        composeRule.onNodeWithText("Parked. Game controls available.").assertIsDisplayed()
        composeRule.onNodeWithText("Snake is ready").assertIsDisplayed()
        composeRule.onNodeWithTag("start").assertIsDisplayed()
        composeRule.onNodeWithTag("snake-board").assertIsDisplayed()
        composeRule.onNodeWithTag("leaderboard").assertIsDisplayed()
        composeRule.onNodeWithText("Leaderboard").assertIsDisplayed()
    }

    @Test
    fun drivingLocksGameControls() {
        composeRule.setContent {
            SnakeAutomotiveApp()
        }

        composeRule.onNodeWithTag("vehicle-driving").performClick()

        composeRule.onNodeWithText("Driving. Game unavailable.").assertIsDisplayed()
        composeRule.onNodeWithText("Unavailable while driving").assertIsDisplayed()
        composeRule.onNodeWithTag("start").assertIsNotEnabled()
        composeRule.onNodeWithTag("restart").assertIsNotEnabled()
        composeRule.onNodeWithTag("direction-up").assertIsNotEnabled()
    }

    @Test
    fun themeToggleFlipsDayAndDarkWithoutLeavingReady() {
        composeRule.setContent {
            SnakeAutomotiveApp()
        }

        val initialLabel = currentThemeToggleLabel()
        composeRule.onNodeWithTag("theme-toggle").performClick()
        assertEquals(oppositeThemeLabel(initialLabel), currentThemeToggleLabel())
        composeRule.onNodeWithText("Snake is ready").assertIsDisplayed()

        composeRule.onNodeWithTag("theme-toggle").performClick()
        assertEquals(initialLabel, currentThemeToggleLabel())
        composeRule.onNodeWithText("Snake is ready").assertIsDisplayed()
    }

    @Test
    fun manualThemeSelectionIsPersisted() {
        composeRule.setContent {
            SnakeAutomotiveApp()
        }

        composeRule.onNodeWithTag("theme-toggle").performClick()

        val expected = if (isSystemDarkMode()) "day" else "dark"
        assertEquals(expected, themePreferences().getString(THEME_PREFERENCE_KEY, null))
    }

    @Test
    fun storedManualThemeIsRestoredOnStart() {
        themePreferences().edit().putString(THEME_PREFERENCE_KEY, "day").commit()

        composeRule.setContent {
            SnakeAutomotiveApp()
        }

        // Day is active, so the toggle offers switching to Dark.
        composeRule.onNodeWithText("Dark mode").assertIsDisplayed()
    }

    @Test
    fun malformedStoredThemeFallsBackToSystemTheme() {
        themePreferences().edit().putString(THEME_PREFERENCE_KEY, "neon").commit()

        composeRule.setContent {
            SnakeAutomotiveApp()
        }

        if (isSystemDarkMode()) {
            composeRule.onNodeWithText("Day mode").assertIsDisplayed()
        } else {
            composeRule.onNodeWithText("Dark mode").assertIsDisplayed()
        }
    }

    @Test
    fun themeToggleWorksWhileDrivingLockedAndKeepsGameLocked() {
        composeRule.setContent {
            SnakeAutomotiveApp()
        }

        composeRule.onNodeWithTag("vehicle-driving").performClick()
        composeRule.onNodeWithText("Driving. Game unavailable.").assertIsDisplayed()

        val labelBefore = currentThemeToggleLabel()
        composeRule.onNodeWithTag("theme-toggle").performClick()
        assertEquals(oppositeThemeLabel(labelBefore), currentThemeToggleLabel())

        composeRule.onNodeWithText("Driving. Game unavailable.").assertIsDisplayed()
        composeRule.onNodeWithText("Unavailable while driving").assertIsDisplayed()
        composeRule.onNodeWithTag("start").assertIsNotEnabled()
        composeRule.onNodeWithTag("restart").assertIsNotEnabled()
        composeRule.onNodeWithTag("direction-up").assertIsNotEnabled()
    }

    private fun themePreferences() = context.createDeviceProtectedStorageContext()
        .getSharedPreferences(THEME_PREFS_NAME, Context.MODE_PRIVATE)

    private fun currentThemeToggleLabel(): String {
        if (composeRule.onAllNodesWithText("Day mode").fetchSemanticsNodes().isNotEmpty()) return "Day mode"
        if (composeRule.onAllNodesWithText("Dark mode").fetchSemanticsNodes().isNotEmpty()) return "Dark mode"
        throw AssertionError("Theme toggle label not found")
    }

    private fun oppositeThemeLabel(label: String): String = when (label) {
        "Day mode" -> "Dark mode"
        "Dark mode" -> "Day mode"
        else -> throw AssertionError("Unknown theme toggle label: $label")
    }

    private fun isSystemDarkMode(): Boolean {
        val nightMode = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        return nightMode == Configuration.UI_MODE_NIGHT_YES
    }
}
