package com.ohadsam.specialevents

import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.printToString
import androidx.test.ext.junit.rules.ActivityScenarioRule

typealias AppRule = AndroidComposeTestRule<ActivityScenarioRule<MainActivity>, MainActivity>

/** Small helpers shared by the emulator tests: the first-run welcome must not hide the screen under test. */
object AppRobot {
    private const val WELCOME_WAIT_MS = 4_000L
    private const val TREE_LIMIT = 7_000

    fun text(rule: AppRule, res: Int): String = rule.activity.getString(res)

    /** The welcome appears only once settings have loaded from disk, so wait for it briefly; later runs never see it. */
    fun dismissWelcome(rule: AppRule) {
        val start = text(rule, R.string.onboarding_start)
        try {
            rule.waitUntil(WELCOME_WAIT_MS) { rule.onAllNodesWithText(start).fetchSemanticsNodes().isNotEmpty() }
        } catch (e: Throwable) {
            return
        }
        rule.onAllNodesWithText(start).onFirst().performClick()
        rule.waitForIdle()
    }

    fun click(rule: ComposeTestRule, label: String, scroll: Boolean = false) {
        val node = rule.onAllNodesWithText(label).onFirst()
        try {
            (if (scroll) node.performScrollTo() else node).performClick()
        } catch (e: AssertionError) {
            // Put what was on screen into the failure so a CI log is enough to diagnose it.
            throw AssertionError("click '$label' failed; screen: " + screen(rule), e)
        }
        rule.waitForIdle()
    }

    /** Every window's tree (a dialog is a second root), trimmed. */
    private fun screen(rule: ComposeTestRule): String {
        val roots = rule.onAllNodes(isRoot())
        return (0 until roots.fetchSemanticsNodes().size).joinToString(" || ") { roots[it].printToString().take(TREE_LIMIT) }
    }

    fun back(rule: AppRule) {
        rule.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        rule.waitForIdle()
    }
}
