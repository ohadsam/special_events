package com.ohadsam.specialevents.greetings

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.telephony.SmsManager
import androidx.core.content.ContextCompat
import com.ohadsam.specialevents.R
import com.ohadsam.specialevents.data.GreetingRepository
import com.ohadsam.specialevents.data.SendChannel
import com.ohadsam.specialevents.domain.greetings.AutoSend
import com.ohadsam.specialevents.domain.greetings.AutoSendDecision
import com.ohadsam.specialevents.domain.greetings.GreetingCatalog
import com.ohadsam.specialevents.domain.greetings.GreetingRenderer
import com.ohadsam.specialevents.domain.greetings.PhoneNumbers
import com.ohadsam.specialevents.domain.reminders.Channel
import com.ohadsam.specialevents.domain.reminders.PlannedReminder
import com.ohadsam.specialevents.log.AppLogger
import com.ohadsam.specialevents.reminders.NotificationContent
import com.ohadsam.specialevents.reminders.NotificationHelper
import kotlinx.coroutines.flow.first

/**
 * Sends the greeting of an SMS reminder by itself when the rule asks for it. Only SMS can be sent silently (WhatsApp and mail have no
 * such API); every other case returns false and the caller shows the ordinary "time to greet" prompt.
 */
class AutoGreetingSender(
    private val context: Context,
    private val greetings: GreetingRepository,
    private val notifications: NotificationHelper,
    private val logger: AppLogger,
) {
    fun hasPermission(): Boolean = ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED

    /** True when the reminder was dealt with (sent, or already greeted) and no prompt is needed. */
    suspend fun handle(plan: PlannedReminder): Boolean {
        val event = plan.item.event
        val day = plan.item.date.toEpochDay()
        val greeted = greetings.greetedKeys.first().contains(event.id to day)
        val decision = AutoSend.decide(plan.rule.autoSend, plan.rule.channel == Channel.SMS, hasPermission(), event.contactPhone, greeted)
        if (decision != AutoSendDecision.SEND) return decision == AutoSendDecision.ALREADY_GREETED
        val template = AutoSend.chooseTemplate(
            plan.rule.templateId,
            GreetingCatalog.forEvent(event.type, event.gender, plan.item.milestone, greetings.customTemplates.first()),
        ) ?: return false
        val text = GreetingRenderer.render(template.body, GreetingContexts.of(event, plan.item))
        val phone = PhoneNumbers.forDial(event.contactPhone) ?: return false
        return try {
            val sms = smsManager()
            sms.sendMultipartTextMessage(phone, null, sms.divideMessage(text), null, null)
            greetings.markSent(event.id, day, SendChannel.SMS, text)
            val name = event.personName ?: event.title
            notifications.show(
                NotificationContent(NotificationHelper.notificationId(plan.rule.id, plan.item.date), event.id, context.getString(R.string.auto_sent_title, name), text, plan.rule.profileKey),
                snoozeable = false,
            )
            logger.i(TAG, "auto SMS sent for event ${event.id}")
            true
        } catch (e: SecurityException) {
            logger.w(TAG, "auto SMS refused, falling back to a prompt", e)
            false
        } catch (e: IllegalArgumentException) {
            logger.w(TAG, "auto SMS could not be built, falling back to a prompt", e)
            false
        }
    }

    private fun smsManager(): SmsManager =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(SmsManager::class.java)
        } else {
            @Suppress("DEPRECATION")
            SmsManager.getDefault()
        }

    private companion object {
        const val TAG = "AutoGreeting"
    }
}
