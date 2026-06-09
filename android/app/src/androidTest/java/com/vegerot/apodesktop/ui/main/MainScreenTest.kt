package com.vegerot.apodesktop.ui.main

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.vegerot.apodesktop.ApodEntry
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/** UI tests for [com.vegerot.apodesktop.ui.main.MainScreen]. */
class MainScreenTest {

    @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Before
    fun setup() {
        composeTestRule.setContent {
            ApodContent(
                entry = FAKE_ENTRY,
                isDailyEnabled = false,
                isUpdatingWallpaper = false,
                onDailyEnabledChange = {},
                onUpdateNowClick = {},
            )
        }
    }

    @Test
    fun firstItem_exists() {
        composeTestRule.onNodeWithText(FAKE_ENTRY.title).assertExists()
        composeTestRule.onNodeWithText(FAKE_ENTRY.explanation).assertExists()
    }
}

private val FAKE_ENTRY = ApodEntry(
    title = "Test Space Picture",
    explanation = "This is a test explanation for the APOD item in the compose test.",
    date = "2026-06-09",
    mediaType = "image",
    url = "https://example.com/image.jpg",
    hdUrl = null,
)
