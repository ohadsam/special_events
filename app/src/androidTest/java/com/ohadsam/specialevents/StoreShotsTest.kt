package com.ohadsam.specialevents

import android.graphics.Bitmap
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.test.platform.app.InstrumentationRegistry
import com.ohadsam.specialevents.calendar.CalendarBasis
import com.ohadsam.specialevents.calendar.OriginDate
import com.ohadsam.specialevents.domain.EventType
import com.ohadsam.specialevents.domain.SpecialEvent
import java.io.File
import java.time.LocalDate
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Not an assertion test: it fills the app with made-up people and saves real screenshots of the main screens for the store listing
 * (`store-shots.yml` runs it and pulls the files). Names are invented, nothing here is anyone's data.
 */
class StoreShotsTest {
    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    private val dir: File by lazy { File(rule.activity.getExternalFilesDir(null), "shots").apply { mkdirs() } }

    @Before
    fun setUp() {
        AppRobot.dismissWelcome(rule)
        val container = (rule.activity.application as SpecialEventsApp).container
        val today = LocalDate.now()
        runBlocking {
            DEMO.forEachIndexed { i, (name, type, yearsAgo) ->
                val day = today.plusDays(FIRST_OFFSET_DAYS + i * STEP_DAYS)
                container.events.save(
                    SpecialEvent(type = type, title = name, personName = name, origin = OriginDate(CalendarBasis.GREGORIAN, day.year - yearsAgo, day.monthValue, day.dayOfMonth)),
                )
            }
        }
        rule.waitUntil(LOAD_MS) { rule.onAllNodesWithText(DEMO.first().first).fetchSemanticsNodes().isNotEmpty() }
    }

    private fun shot(name: String) {
        rule.waitForIdle()
        val bitmap = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot() ?: return
        File(dir, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, PNG_QUALITY, it) }
    }

    @Test
    fun takeShots() {
        // The empty state must be gone, otherwise the first picture shows "no events yet".
        val addFirst = AppRobot.text(rule, R.string.action_add_first_event)
        rule.waitUntil(LOAD_MS) { rule.onAllNodesWithText(addFirst).fetchSemanticsNodes().isEmpty() }
        shot("upcoming")
        AppRobot.click(rule, DEMO.first().first)
        shot("event")
        AppRobot.back(rule)
        AppRobot.click(rule, AppRobot.text(rule, R.string.tab_calendar))
        shot("calendar")
        AppRobot.click(rule, AppRobot.text(rule, R.string.tab_settings))
        AppRobot.click(rule, AppRobot.text(rule, R.string.settings_section_backup), scroll = true)
        AppRobot.click(rule, AppRobot.text(rule, R.string.settings_review), scroll = true)
        rule.onAllNodesWithText(AppRobot.text(rule, R.string.review_title)).onFirst()
        shot("review")
    }

    private companion object {
        const val LOAD_MS = 10_000L
        const val FIRST_OFFSET_DAYS = 3L
        const val STEP_DAYS = 9L
        const val PNG_QUALITY = 100
        val DEMO = listOf(
            Triple("דנה לוי", EventType.BIRTHDAY, 34),
            Triple("רון ומיכל", EventType.ANNIVERSARY, 12),
            Triple("סבא יוסף ז״ל", EventType.YAHRZEIT, 9),
            Triple("נועה כהן", EventType.BIRTHDAY, 8),
            Triple("אורי שמיר", EventType.BIRTHDAY, 41),
        )
    }
}
