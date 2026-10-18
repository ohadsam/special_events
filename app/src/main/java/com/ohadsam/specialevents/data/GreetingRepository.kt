package com.ohadsam.specialevents.data

import com.ohadsam.specialevents.data.db.AppDatabase
import com.ohadsam.specialevents.data.db.GreetingSentEntity
import com.ohadsam.specialevents.data.db.GreetingTemplateEntity
import com.ohadsam.specialevents.domain.EventType
import com.ohadsam.specialevents.domain.greetings.GreetingTemplate
import java.time.Clock
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class SendChannel { WHATSAPP, EMAIL, SMS, SHARE }

/** One greeting that was sent (or at least handed to WhatsApp / mail / SMS). */
data class GreetingSent(val id: Long, val eventId: Long, val occurrenceDay: Long, val channel: SendChannel, val sentAt: Long, val preview: String)

/** The user's own greeting templates and the history of greetings sent. */
class GreetingRepository(db: AppDatabase, private val clock: Clock = Clock.systemUTC()) {
    private val dao = db.greetingDao()

    val customTemplates: Flow<List<GreetingTemplate>> = dao.observeTemplates().map { rows -> rows.map(::toTemplate) }

    suspend fun saveTemplate(t: GreetingTemplate): Long {
        val entity = GreetingTemplateEntity(
            maxOf(t.id, 0), t.name.trim(), t.body, t.forType?.name, clock.millis(),
            folder = t.folder?.trim()?.ifEmpty { null },
            tags = t.tags.joinToString("\n").ifEmpty { null },
            groupIds = t.groupIds.joinToString(",").ifEmpty { null },
        )
        return if (t.id > 0) t.id.also { dao.updateTemplate(entity) } else dao.insertTemplate(entity)
    }

    suspend fun deleteTemplate(id: Long) = dao.deleteTemplate(id)

    fun sentFor(eventId: Long): Flow<List<GreetingSent>> = dao.observeSentFor(eventId).map { rows -> rows.map(::toSent) }

    /** (eventId, occurrence day) pairs that were greeted, for the "greeted" check marks. */
    val greetedKeys: Flow<Set<Pair<Long, Long>>> = dao.observeAllSent().map { rows -> rows.map { it.eventId to it.occurrenceDay }.toSet() }

    /** [recipient] is the [com.ohadsam.specialevents.domain.groups.Recipient.key] when the greeting went to one of several people. */
    suspend fun markSent(eventId: Long, occurrenceDay: Long, channel: SendChannel, text: String, recipient: String? = null) {
        dao.insertSent(
            GreetingSentEntity(eventId = eventId, occurrenceDay = occurrenceDay, channel = channel.name, sentAt = clock.millis(), preview = text.take(PREVIEW_CHARS), recipient = recipient),
        )
    }

    /** Who of a multi-recipient greeting already got it for this occurrence. */
    suspend fun sentRecipients(eventId: Long, occurrenceDay: Long): Set<String> = dao.sentRecipients(eventId, occurrenceDay).toSet()

    suspend fun deleteSent(id: Long) = dao.deleteSent(id)

    private fun toTemplate(e: GreetingTemplateEntity) =
        GreetingTemplate(
            e.id, e.name, e.body,
            forType = e.forType?.let { n -> EventType.entries.firstOrNull { it.name == n } },
            folder = e.folder,
            tags = e.tags?.split('\n')?.map { it.trim() }?.filter { it.isNotEmpty() }.orEmpty(),
            groupIds = e.groupIds?.split(',')?.mapNotNull { it.trim().toLongOrNull() }?.toSet().orEmpty(),
        )

    private fun toSent(e: GreetingSentEntity) = GreetingSent(
        e.id, e.eventId, e.occurrenceDay, SendChannel.entries.firstOrNull { it.name == e.channel } ?: SendChannel.SHARE, e.sentAt, e.preview,
    )

    private companion object {
        const val PREVIEW_CHARS = 200
    }
}
