package com.ohadsam.specialevents.domain.backup

import com.ohadsam.specialevents.calendar.CalendarBasis
import com.ohadsam.specialevents.calendar.OriginDate
import com.ohadsam.specialevents.calendar.Tier
import com.ohadsam.specialevents.domain.EventType
import com.ohadsam.specialevents.domain.Gender
import com.ohadsam.specialevents.domain.Recurrence
import com.ohadsam.specialevents.domain.SpecialEvent
import com.ohadsam.specialevents.domain.Tag
import com.ohadsam.specialevents.domain.checklist.ChecklistItem
import com.ohadsam.specialevents.domain.checklist.ChecklistKind
import com.ohadsam.specialevents.domain.greetings.GreetingTemplate
import com.ohadsam.specialevents.domain.groups.EventGroup
import com.ohadsam.specialevents.domain.reminders.Channel
import com.ohadsam.specialevents.domain.reminders.OffsetUnit
import com.ohadsam.specialevents.domain.reminders.ReminderRule
import com.ohadsam.specialevents.domain.reminders.RuleOrigin
import com.ohadsam.specialevents.domain.reminders.ShabbatPolicy
import com.ohadsam.specialevents.domain.reminders.SoundProfile
import com.ohadsam.specialevents.domain.reminders.TimeMode
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put

/**
 * The versioned JSON backup format. Reading is defensive because a file can come from anywhere: unknown fields are ignored, an
 * entry that cannot be read is skipped (and counted) instead of failing the whole file, text is clamped, and settings that could
 * act on their own (automatic SMS, device-specific sounds and template ids) are not carried over.
 */
object BackupCodec {
    const val FORMAT = "special-events-backup"
    const val VERSION = 1
    const val KIND_BACKUP = "backup"

    /** A file made to hand a few events to someone else (see `SharePlanner`). */
    const val KIND_SHARE = "share"
    const val MAX_CHARS = 5_000_000
    private const val MAX_ENTRIES = 50_000
    private const val MAX_TITLE = 200
    private const val MAX_TEXT = 5_000
    private const val MAX_HEBREW_MONTH = 13
    private const val MAX_DAY = 31
    private const val MAX_MINUTES = 24 * 60

    private val json = Json { prettyPrint = true }

    fun encode(data: BackupData, exportedAt: String, appVersion: String, kind: String = KIND_BACKUP): String = json.encodeToString(
        JsonElement.serializer(),
        buildJsonObject {
            put("format", FORMAT)
            put("version", VERSION)
            put("exportedAt", exportedAt)
            put("kind", kind)
            put("app", appVersion)
            put("events", JsonArray(data.events.map(::eventJson)))
            put("rules", JsonArray(data.rules.map(::ruleJson)))
            put("checklist", JsonArray(data.checklist.map(::itemJson)))
            put("templates", JsonArray(data.templates.map(::templateJson)))
            put("groups", JsonArray(data.groups.map(::groupJson)))
            put("members", JsonArray(data.members.map { (e, g) -> buildJsonObject { put("eventId", e); put("groupId", g) } }))
        },
    )

    fun decode(text: String): DecodeResult {
        if (text.length > MAX_CHARS) return DecodeResult.Failure(BackupError.TOO_BIG)
        val root = runCatching { Json.parseToJsonElement(text).jsonObject }.getOrNull() ?: return DecodeResult.Failure(BackupError.NOT_JSON)
        if (root.str("format") != FORMAT) return DecodeResult.Failure(BackupError.WRONG_FORMAT)
        if ((root.int("version") ?: 0) > VERSION) return DecodeResult.Failure(BackupError.TOO_NEW)
        var skipped = 0
        fun <T> read(key: String, parse: (JsonObject) -> T?): List<T> {
            val array = (root[key] as? JsonArray) ?: return emptyList()
            if (array.size > MAX_ENTRIES) return emptyList<T>().also { skipped += array.size }
            return array.mapNotNull { el -> runCatching { parse(el.jsonObject) }.getOrNull().also { if (it == null) skipped++ } }
        }
        val data = BackupData(
            read<SpecialEvent>("events") { parseEvent(it) },
            read<ReminderRule>("rules") { parseRule(it) },
            read<ChecklistItem>("checklist") { parseItem(it) },
            read<GreetingTemplate>("templates") { parseTemplate(it) },
            read<EventGroup>("groups") { parseGroup(it) },
        ).copy(members = read<Pair<Long, Long>>("members") { m -> (m.long("eventId") ?: return@read null) to (m.long("groupId") ?: return@read null) })
        return DecodeResult.Ok(data, root.str("exportedAt"), skipped, root.str("kind") ?: KIND_BACKUP)
    }

    private fun eventJson(e: SpecialEvent) = buildJsonObject {
        put("id", e.id)
        put("type", e.type.name)
        put("title", e.title)
        put("personName", e.personName)
        put("gender", e.gender?.name)
        put("relation", e.relation)
        put("basis", e.origin.basis.name)
        put("year", e.origin.year)
        put("month", e.origin.month)
        put("day", e.origin.day)
        put("recurrence", e.recurrence.name)
        put("afterSunset", e.afterSunset)
        put("timeMinutes", e.timeMinutes)
        put("notes", e.notes)
        put("colorArgb", e.colorArgb)
        put("iconKey", e.iconKey)
        put("contactPhone", e.contactPhone)
        put("contactEmail", e.contactEmail)
        put("archived", e.archived)
        put("tags", buildJsonArray { e.tags.forEach { add(buildJsonObject { put("name", it.name); put("color", it.colorArgb) }) } })
    }

    private fun parseEvent(o: JsonObject): SpecialEvent? {
        val type = enumOf<EventType>(o.str("type")) ?: return null
        val title = o.str("title")?.trim()?.take(MAX_TITLE)?.takeIf { it.isNotEmpty() } ?: return null
        val month = o.int("month") ?: return null
        val day = o.int("day") ?: return null
        val basis = enumOf<CalendarBasis>(o.str("basis")) ?: return null
        if (month !in 1..(if (basis == CalendarBasis.HEBREW) MAX_HEBREW_MONTH else 12) || day !in 1..MAX_DAY) return null
        return SpecialEvent(
            id = o.long("id") ?: 0,
            type = type,
            title = title,
            personName = o.str("personName")?.take(MAX_TITLE),
            gender = enumOf<Gender>(o.str("gender")),
            relation = o.str("relation")?.take(MAX_TITLE),
            origin = OriginDate(basis, o.int("year"), month, day),
            recurrence = enumOf<Recurrence>(o.str("recurrence")) ?: if (type.yearly) Recurrence.YEARLY else Recurrence.ONCE,
            afterSunset = o.bool("afterSunset") ?: false,
            timeMinutes = o.int("timeMinutes")?.takeIf { it in 0 until MAX_MINUTES },
            notes = o.str("notes")?.take(MAX_TEXT),
            colorArgb = o.int("colorArgb"),
            iconKey = o.str("iconKey")?.take(MAX_TITLE),
            contactPhone = o.str("contactPhone")?.take(MAX_TITLE),
            contactEmail = o.str("contactEmail")?.take(MAX_TITLE),
            archived = o.bool("archived") ?: false,
            tags = (o["tags"] as? JsonArray).orEmpty().mapNotNull { t ->
                val obj = t as? JsonObject
                obj?.str("name")?.trim()?.take(MAX_TITLE)?.takeIf { it.isNotEmpty() }?.let { Tag(name = it, colorArgb = obj.int("color")) }
            },
        )
    }

    /** A list of rules as JSON text, for places that store a reminder template (groups). */
    fun encodeRules(rules: List<ReminderRule>): String = JsonArray(rules.map(::ruleJson)).toString()

    fun decodeRules(text: String): List<ReminderRule> {
        val array = runCatching { Json.parseToJsonElement(text) as? JsonArray }.getOrNull() ?: return emptyList()
        return array.take(MAX_ENTRIES).mapNotNull { el -> runCatching { parseRule(el.jsonObject) }.getOrNull() }
    }

    private fun ruleJson(r: ReminderRule) = buildJsonObject {
        put("eventId", r.eventId)
        put("offsetValue", r.offsetValue)
        put("offsetUnit", r.offsetUnit.name)
        put("timeMinutes", r.timeMinutes)
        put("channel", r.channel.name)
        put("shabbatPolicy", r.shabbatPolicy.name)
        put("profileKey", r.profileKey)
        put("minTier", r.minTier.name)
        put("enabled", r.enabled)
        put("origin", r.origin.name)
        put("timeMode", r.timeMode.name)
        put("toPerson", r.toPerson)
        put("toSelf", r.toSelf)
        put("toGroups", JsonArray(r.toGroupIds.map { JsonPrimitive(it) }))
        put("toExtra", JsonArray(r.toExtra.map { JsonPrimitive(it) }))
    }

    private fun parseRule(o: JsonObject): ReminderRule? {
        val profile = o.str("profileKey")?.takeIf { key -> SoundProfile.BUILT_IN.any { it.key == key } } ?: SoundProfile.DEFAULT_KEY
        return ReminderRule(
            eventId = o.long("eventId") ?: return null,
            offsetValue = (o.int("offsetValue") ?: return null).coerceIn(0, MAX_OFFSET),
            offsetUnit = enumOf<OffsetUnit>(o.str("offsetUnit")) ?: return null,
            timeMinutes = (o.int("timeMinutes") ?: ReminderRule.DEFAULT_TIME_MINUTES).coerceIn(0, MAX_MINUTES - 1),
            channel = enumOf<Channel>(o.str("channel")) ?: Channel.NOTIFICATION,
            shabbatPolicy = enumOf<ShabbatPolicy>(o.str("shabbatPolicy")) ?: ShabbatPolicy.BEFORE,
            profileKey = profile,
            minTier = enumOf<Tier>(o.str("minTier")) ?: Tier.NONE,
            enabled = o.bool("enabled") ?: true,
            origin = enumOf<RuleOrigin>(o.str("origin")) ?: RuleOrigin.USER,
            timeMode = enumOf<TimeMode>(o.str("timeMode")) ?: TimeMode.FIXED,
            autoSend = false,
            templateId = null,
            toPerson = o.bool("toPerson") ?: true,
            toSelf = o.bool("toSelf") ?: false,
            toGroupIds = (o["toGroups"] as? JsonArray).orEmpty().mapNotNull { (it as? JsonPrimitive)?.longOrNull }.take(MAX_TAGS).toSet(),
            toExtra = (o["toExtra"] as? JsonArray).orEmpty().mapNotNull { (it as? JsonPrimitive)?.takeIf { p -> p.isString }?.contentOrNull?.trim()?.take(MAX_TITLE)?.takeIf { s -> s.isNotEmpty() } }.take(MAX_TAGS),
        )
    }

    private fun itemJson(i: ChecklistItem) = buildJsonObject {
        put("eventId", i.eventId)
        put("kind", i.kind.name)
        put("title", i.title)
        put("note", i.note)
        put("amount", i.amount)
        put("daysBefore", i.daysBefore)
        put("done", i.done)
        put("sortOrder", i.sortOrder)
    }

    private fun parseItem(o: JsonObject): ChecklistItem? {
        val title = o.str("title")?.trim()?.take(MAX_TITLE)?.takeIf { it.isNotEmpty() } ?: return null
        return ChecklistItem(
            eventId = o.long("eventId") ?: return null,
            kind = enumOf<ChecklistKind>(o.str("kind")) ?: ChecklistKind.TASK,
            title = title,
            note = o.str("note")?.take(MAX_TEXT),
            amount = o.int("amount"),
            daysBefore = o.int("daysBefore"),
            done = o.bool("done") ?: false,
            sortOrder = o.int("sortOrder") ?: 0,
        )
    }

    private fun groupJson(g: EventGroup) = buildJsonObject {
        put("id", g.id)
        put("name", g.name)
        put("emoji", g.emoji)
        put("color", g.colorArgb)
        put("builtInKey", g.builtInKey)
        put("sortOrder", g.sortOrder)
        put("tags", JsonArray(g.tags.map { JsonPrimitive(it) }))
        put("channel", g.defaultChannel?.name)
        put("rules", JsonArray(g.reminderRules.map(::ruleJson)))
        put("extra", JsonArray(g.extraRecipients.map { JsonPrimitive(it) }))
    }

    private fun parseGroup(o: JsonObject): EventGroup? {
        val name = o.str("name")?.trim()?.take(MAX_TITLE)?.takeIf { it.isNotEmpty() } ?: return null
        fun strings(key: String) = (o[key] as? JsonArray).orEmpty().mapNotNull { (it as? JsonPrimitive)?.takeIf { p -> p.isString }?.contentOrNull?.trim()?.take(MAX_TITLE)?.takeIf { s -> s.isNotEmpty() } }.take(MAX_ENTRIES)
        return EventGroup(
            id = o.long("id") ?: 0,
            name = name,
            emoji = o.str("emoji")?.take(MAX_EMOJI),
            colorArgb = o.int("color"),
            builtInKey = o.str("builtInKey")?.take(MAX_TITLE),
            sortOrder = o.int("sortOrder") ?: 0,
            tags = strings("tags"),
            defaultChannel = enumOf<Channel>(o.str("channel")),
            reminderRules = (o["rules"] as? JsonArray).orEmpty().mapNotNull { r -> runCatching { parseRule(r.jsonObject) }.getOrNull() },
            extraRecipients = strings("extra"),
        )
    }

    private fun templateJson(t: GreetingTemplate) = buildJsonObject {
        put("name", t.name)
        put("body", t.body)
        put("forType", t.forType?.name)
        put("folder", t.folder)
        put("tags", JsonArray(t.tags.map { JsonPrimitive(it) }))
        put("groups", JsonArray(t.groupIds.map { JsonPrimitive(it) }))
    }

    private fun parseTemplate(o: JsonObject): GreetingTemplate? {
        val name = o.str("name")?.trim()?.take(MAX_TITLE)?.takeIf { it.isNotEmpty() } ?: return null
        val body = o.str("body")?.take(MAX_TEXT)?.takeIf { it.isNotBlank() } ?: return null
        return GreetingTemplate(
            0, name, body,
            forType = enumOf<EventType>(o.str("forType")),
            folder = o.str("folder")?.trim()?.take(MAX_TITLE)?.ifEmpty { null },
            tags = (o["tags"] as? JsonArray).orEmpty().mapNotNull { (it as? JsonPrimitive)?.takeIf { p -> p.isString }?.contentOrNull?.trim()?.take(MAX_TITLE)?.takeIf { s -> s.isNotEmpty() } }.take(MAX_TAGS),
            groupIds = (o["groups"] as? JsonArray).orEmpty().mapNotNull { (it as? JsonPrimitive)?.longOrNull }.take(MAX_TAGS).toSet(),
        )
    }

    private const val MAX_OFFSET = 365
    private const val MAX_EMOJI = 16
    private const val MAX_TAGS = 50

    private inline fun <reified E : Enum<E>> enumOf(name: String?): E? = name?.let { n -> enumValues<E>().firstOrNull { it.name == n } }

    private fun JsonObject.prim(key: String): JsonPrimitive? = (this[key] as? JsonPrimitive)?.takeIf { it !is JsonNull }

    private fun JsonObject.str(key: String): String? = prim(key)?.takeIf { it.isString }?.contentOrNull

    private fun JsonObject.int(key: String): Int? = prim(key)?.intOrNull

    private fun JsonObject.long(key: String): Long? = prim(key)?.longOrNull

    private fun JsonObject.bool(key: String): Boolean? = prim(key)?.booleanOrNull
}
