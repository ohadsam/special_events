package com.ohadsam.specialevents.data

import com.ohadsam.specialevents.domain.backup.BackupData
import com.ohadsam.specialevents.domain.backup.BackupPlanner
import com.ohadsam.specialevents.domain.backup.ImportMode
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first

/** What was removed, kept so that "Undo" can put it all back: the event with its reminders, checklist and groups, and its photo file. */
class DeletedEvent(val title: String, val data: BackupData, val photoUri: String?, val contactLookupUri: String?)

/**
 * Delete with Undo, built on the backup machinery: the event is captured as a one-event backup before it is removed and imported
 * again on Undo. The photo file is only deleted once the Undo offer has passed (see the app root).
 */
class UndoDelete(private val backups: BackupRepository, private val events: EventRepository) {
    /** Deleted events waiting for an Undo offer; the app root shows it as a snackbar. */
    val notices = MutableSharedFlow<DeletedEvent>(extraBufferCapacity = 1)

    suspend fun capture(eventId: Long): DeletedEvent? {
        val snapshot = backups.snapshot()
        val event = snapshot.events.firstOrNull { it.id == eventId } ?: return null
        return DeletedEvent(event.title, BackupPlanner.restrict(snapshot, setOf(eventId)), event.photoUri, event.contactLookupUri)
    }

    suspend fun restore(deleted: DeletedEvent) {
        backups.import(deleted.data, ImportMode.MERGE)
        val original = deleted.data.events.firstOrNull() ?: return
        // An import leaves out the photo and the contact link (they point at files / rows of another device); here they are still ours.
        events.allEvents.first().firstOrNull { BackupPlanner.key(it) == BackupPlanner.key(original) }
            ?.let { events.save(it.copy(photoUri = deleted.photoUri, contactLookupUri = deleted.contactLookupUri)) }
    }
}
