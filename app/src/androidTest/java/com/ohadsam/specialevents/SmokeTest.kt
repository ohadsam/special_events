package com.ohadsam.specialevents

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Emulator smoke test: the app starts, shows the bottom tabs, and Settings → logs opens. */
@RunWith(AndroidJUnit4::class)
class SmokeTest {
    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    private fun text(res: Int) = rule.activity.getString(res)

    @Test
    fun bottomNavigationShowsAllTabs() {
        listOf(R.string.tab_upcoming, R.string.tab_calendar, R.string.tab_settings).forEach {
            rule.onAllNodesWithText(text(it)).onFirst().assertExists()
        }
    }

    @Test
    fun logsScreenOpensFromSettings() {
        rule.onAllNodesWithText(text(R.string.tab_settings)).onFirst().performClick()
        rule.onAllNodesWithText(text(R.string.settings_logs)).onFirst().performClick()
        rule.waitForIdle()
        rule.onAllNodesWithText(text(R.string.logs_title)).onFirst().assertExists()
    }
}
