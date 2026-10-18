package com.ohadsam.specialevents

import androidx.compose.ui.test.ComposeTestRule
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.rules.ActivityScenarioRule

typealias AppRule = AndroidComposeTestRule<ActivityScenarioRule<MainActivity>, MainActivity>

/** Small helpers shared by the emulator tests: the first-run welcome must not hide the screen under test. */
object AppRobot {
    fun text(rule: AppRule, res: Int): String = rule.activity.getString(res)

    fun dismissWelcome(rule: AppRule) {
        rule.waitForIdle()
        val start = text(rule, R.string.onboarding_start)
        if (rule.onAllNodesWithText(start).fetchSemanticsNodes().isNotEmpty()) {
            rule.onAllNodesWithText(start).onFirst().performClick()
            rule.waitForIdle()
        }
    }

    fun click(rule: ComposeTestRule, label: String, scroll: Boolean = false) {
        val node = rule.onAllNodesWithText(label).onFirst()
        (if (scroll) node.performScrollTo() else node).performClick()
        rule.waitForIdle()
    }

    fun back(rule: AppRule) {
        rule.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        rule.waitForIdle()
    }
}
