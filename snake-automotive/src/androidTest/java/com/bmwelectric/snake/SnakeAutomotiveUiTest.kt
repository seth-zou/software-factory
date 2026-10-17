package com.bmwelectric.snake

import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Rule
import org.junit.Test

class SnakeAutomotiveUiTest {
    @get:Rule
    val composeRule = createComposeRule()

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
}
