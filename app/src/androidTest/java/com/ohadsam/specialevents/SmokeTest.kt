package com.ohadsam.specialevents

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Emulator smoke tests: the app starts, shows the bottom tabs and the first-run welcome, and the main screens open. */
@RunWith(AndroidJUnit4::class)
class SmokeTest {
    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    private fun text(res: Int) = AppRobot.text(rule, res)

    @Before
    fun setUp() = AppRobot.dismissWelcome(rule)

    @Test
    fun bottomNavigationShowsAllTabs() {
        listOf(R.string.tab_upcoming, R.string.tab_calendar, R.string.tab_settings).forEach {
            rule.onAllNodesWithText(text(it)).onFirst().assertExists()
        }
    }

    @Test
    fun addEventOpensTheEditorAndBackReturns() {
        // On a fresh install the list is empty, and the empty state carries the add button.
        AppRobot.click(rule, text(R.string.action_add_first_event))
        rule.onAllNodesWithText(text(R.string.editor_title_new)).onFirst().assertIsDisplayed()
        AppRobot.back(rule)
        rule.onAllNodesWithText(text(R.string.action_add_first_event)).onFirst().assertExists()
    }

    @Test
    fun calendarTabOpens() {
        AppRobot.click(rule, text(R.string.tab_calendar))
        rule.onAllNodesWithText(text(R.string.calendar_level_month)).onFirst().assertExists()
    }

    @Test
    fun settingsScreensOpen() {
        AppRobot.click(rule, text(R.string.tab_settings))
        // Settings sections are collapsed accordions: open the one that holds the entry first.
        listOf(
            Triple(R.string.settings_groups, R.string.settings_section_greetings, R.string.groups_title),
            Triple(R.string.settings_print, R.string.settings_section_backup, R.string.print_title),
            Triple(R.string.settings_backup, R.string.settings_section_backup, R.string.backup_title),
            Triple(R.string.settings_guide, R.string.settings_section_general, R.string.guide_title),
            Triple(R.string.settings_logs, R.string.settings_section_general, R.string.logs_title),
        ).forEach { (entry, section, title) ->
            if (rule.onAllNodesWithText(text(entry)).fetchSemanticsNodes().isEmpty()) AppRobot.click(rule, text(section), scroll = true)
            AppRobot.click(rule, text(entry), scroll = true)
            rule.onAllNodesWithText(text(title)).onFirst().assertExists()
            AppRobot.back(rule)
        }
    }

    @Test
    fun welcomeIsShownOnlyOnce() {
        // setUp already dismissed it; it must not come back after the activity is recreated.
        rule.activityRule.scenario.recreate()
        rule.waitForIdle()
        check(rule.onAllNodesWithText(text(R.string.onboarding_start)).fetchSemanticsNodes().isEmpty()) { "welcome shown again" }
    }
}
