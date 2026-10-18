package com.ohadsam.specialevents.data

import com.ohadsam.specialevents.calendar.Tier
import com.ohadsam.specialevents.data.db.AppDatabase
import com.ohadsam.specialevents.data.db.ReminderRuleEntity
import com.ohadsam.specialevents.domain.EventType
import com.ohadsam.specialevents.domain.reminders.Channel
import com.ohadsam.specialevents.domain.reminders.OffsetUnit
import com.ohadsam.specialevents.domain.reminders.ReminderDefaults
import com.ohadsam.specialevents.domain.reminders.ReminderRule
import com.ohadsam.specialevents.domain.reminders.RuleOrigin
import com.ohadsam.specialevents.domain.reminders.ShabbatPolicy
import com.ohadsam.specialevents.domain.reminders.TimeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/** Reminder rules of all events. Rules die with their event (foreign-key cascade). */
class ReminderRepository(db: AppDatabase) {
    private val dao = db.reminderDao()

    val allRules: Flow<List<ReminderRule>> = dao.observeAll().map { rows -> rows.map(ReminderMapper::toDomain) }

    suspend fun snapshot(): List<ReminderRule> = allRules.first()

    suspend fun forEvent(eventId: Long): List<ReminderRule> = dao.forEvent(eventId).map(ReminderMapper::toDomain)

    suspend fun save(rule: ReminderRule): Long =
        if (rule.id == 0L) dao.insert(ReminderMapper.toEntity(rule)) else rule.id.also { dao.update(ReminderMapper.toEntity(rule)) }

    suspend fun delete(id: Long) = dao.delete(id)

    suspend fun replaceForEvent(eventId: Long, rules: List<ReminderRule>) {
        dao.deleteForEvent(eventId)
        dao.insertAll(rules.map { ReminderMapper.toEntity(it.copy(id = 0, eventId = eventId)) })
    }

    /** The reminders a brand-new event of [type] starts with. */
    suspend fun addDefaults(eventId: Long, type: EventType) {
        dao.insertAll(ReminderDefaults.forType(type).map { ReminderMapper.toEntity(it.copy(id = 0, eventId = eventId)) })
    }
}

object ReminderMapper {
    fun toDomain(e: ReminderRuleEntity) = ReminderRule(
        id = e.id,
        eventId = e.eventId,
        offsetValue = e.offsetValue,
        offsetUnit = enumOrDefault(e.offsetUnit, OffsetUnit.DAYS),
        timeMinutes = e.timeMinutes.coerceIn(0, MINUTES_IN_DAY - 1),
        channel = enumOrDefault(e.channel, Channel.NOTIFICATION),
        shabbatPolicy = enumOrDefault(e.shabbatPolicy, ShabbatPolicy.BEFORE),
        profileKey = e.profileKey,
        minTier = enumOrDefault(e.minTier, Tier.NONE),
        enabled = e.enabled,
        origin = enumOrDefault(e.origin, RuleOrigin.USER),
        timeMode = enumOrDefault(e.timeMode, TimeMode.FIXED),
        autoSend = e.autoSend,
        templateId = e.templateId,
    )

    fun toEntity(r: ReminderRule) = ReminderRuleEntity(
        id = r.id,
        eventId = r.eventId,
        offsetValue = r.offsetValue,
        offsetUnit = r.offsetUnit.name,
        timeMinutes = r.timeMinutes,
        channel = r.channel.name,
        shabbatPolicy = r.shabbatPolicy.name,
        profileKey = r.profileKey,
        minTier = r.minTier.name,
        enabled = r.enabled,
        origin = r.origin.name,
        timeMode = r.timeMode.name,
        autoSend = r.autoSend,
        templateId = r.templateId,
    )

    private inline fun <reified E : Enum<E>> enumOrDefault(name: String, default: E): E = enumValues<E>().firstOrNull { it.name == name } ?: default

    private const val MINUTES_IN_DAY = 24 * 60
}
