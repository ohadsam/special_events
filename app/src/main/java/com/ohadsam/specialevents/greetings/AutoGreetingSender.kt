package com.ohadsam.specialevents.greetings

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.telephony.SmsManager
import androidx.core.content.ContextCompat
import com.ohadsam.specialevents.R
import com.ohadsam.specialevents.data.EventRepository
import com.ohadsam.specialevents.data.GreetingRepository
import com.ohadsam.specialevents.data.GroupRepository
import com.ohadsam.specialevents.data.SendChannel
import com.ohadsam.specialevents.data.prefs.SettingsRepository
import com.ohadsam.specialevents.domain.SpecialEvent
import com.ohadsam.specialevents.domain.greetings.AutoSend
import com.ohadsam.specialevents.domain.greetings.AutoSendDecision
import com.ohadsam.specialevents.domain.greetings.GreetingCatalog
import com.ohadsam.specialevents.domain.greetings.GreetingRenderer
import com.ohadsam.specialevents.domain.greetings.GreetingTemplate
import com.ohadsam.specialevents.domain.greetings.PhoneNumbers
import com.ohadsam.specialevents.domain.groups.EventGroup
import com.ohadsam.specialevents.domain.groups.GroupLogic
import com.ohadsam.specialevents.domain.groups.Recipient
import com.ohadsam.specialevents.domain.groups.RecipientResolver
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
    private val groups: GroupRepository,
    private val events: EventRepository,
    private val settings: SettingsRepository,
    private val notifications: NotificationHelper,
    private val logger: AppLogger,
) {
    fun hasPermission(): Boolean = ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED

    /** True when the reminder was dealt with (sent to everyone who has a phone, or all already greeted) and no prompt is needed. */
    suspend fun handle(plan: PlannedReminder): Boolean {
        val rule = plan.rule
        if (!rule.autoSend || rule.channel != Channel.SMS || !hasPermission()) return false
        val event = plan.item.event
        val day = plan.item.date.toEpochDay()
        val eventGroups = groups.groupsOf(event.id)
        val greetedByHand = greetings.greetedKeys.first().contains(event.id to day) && greetings.sentRecipients(event.id, day).isEmpty()
        if (greetedByHand) return true
        val template = pickTemplate(plan, eventGroups) ?: return false
        val cfg = settings.settings.first()
        val all = events.allEvents.first()
        val membership = groups.membership.first()
        val recipients = RecipientResolver.resolve(rule, event, groups.groups.first(), { id -> all.filter { id in membership[it.id].orEmpty() } }, cfg.selfPhone, cfg.selfEmail)
        val alreadySent = greetings.sentRecipients(event.id, day)
        val base = GreetingContexts.of(event, plan.item)
        var sent = 0
        var withPhone = 0
        for (r in recipients) {
            val decision = AutoSend.decide(rule.autoSend, true, true, r.phone, r.key in alreadySent)
            if (r.phone != null && PhoneNumbers.forDial(r.phone) != null) withPhone++
            if (decision != AutoSendDecision.SEND) continue
            val text = GreetingRenderer.render(template.body, base.copy(recipient = r.label))
            if (send(r, text, event.id, day)) sent++
        }
        if (sent > 0) notify(plan, event, template.body, sent, recipients.size)
        // Handled when everyone reachable by SMS has it; otherwise the ordinary prompt remains for the rest.
        return withPhone > 0 && sent + recipients.count { it.key in alreadySent } >= withPhone
    }

    private suspend fun pickTemplate(plan: PlannedReminder, eventGroups: List<EventGroup>): GreetingTemplate? {
        val event = plan.item.event
        val candidates = GreetingCatalog.preferGroups(
            GreetingCatalog.forEvent(event.type, event.gender, plan.item.milestone, greetings.customTemplates.first()),
            eventGroups.map { it.id }.toSet(),
        )
        return AutoSend.chooseTemplate(GroupLogic.greetingTemplateId(plan.rule.templateId, eventGroups), candidates)
    }

    private suspend fun send(r: Recipient, text: String, eventId: Long, day: Long): Boolean {
        val phone = PhoneNumbers.forDial(r.phone) ?: return false
        return try {
            val sms = smsManager()
            sms.sendMultipartTextMessage(phone, null, sms.divideMessage(text), null, null)
            greetings.markSent(eventId, day, SendChannel.SMS, text, r.key)
            true
        } catch (e: SecurityException) {
            logger.w(TAG, "auto SMS refused", e)
            false
        } catch (e: IllegalArgumentException) {
            logger.w(TAG, "auto SMS could not be built", e)
            false
        }
    }

    private fun notify(plan: PlannedReminder, event: SpecialEvent, body: String, sent: Int, total: Int) {
        val name = event.personName ?: event.title
        val title = if (total <= 1) context.getString(R.string.auto_sent_title, name) else context.getString(R.string.auto_sent_many_title, name, sent)
        notifications.show(
            NotificationContent(NotificationHelper.notificationId(plan.rule.id, plan.item.date), event.id, title, body, plan.rule.profileKey),
            snoozeable = false,
        )
        logger.i(TAG, "auto SMS sent to $sent of $total for event ${event.id}")
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
