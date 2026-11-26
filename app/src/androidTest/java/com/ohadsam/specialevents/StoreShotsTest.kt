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
        // The first frame can still show the empty state although the data is there, so the list picture is taken after opening an event
        // and coming back, when the list has certainly been drawn with its items.
        AppRobot.click(rule, DEMO.first().first)
        shot("event")
        AppRobot.back(rule)
        Thread.sleep(SETTLE_MS)
        shot("upcoming")
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
        const val SETTLE_MS = 600L
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
