package com.ohadsam.specialevents.domain.backup

import com.ohadsam.specialevents.domain.SpecialEvent
import com.ohadsam.specialevents.domain.checklist.ChecklistItem
import com.ohadsam.specialevents.domain.greetings.GreetingTemplate
import com.ohadsam.specialevents.domain.groups.EventGroup
import com.ohadsam.specialevents.domain.reminders.ReminderRule

/**
 * Everything a backup carries. Rules and checklist items point at their event by the event's [SpecialEvent.id] inside the file;
 * ids are remapped on import. Not included on purpose: photos and contact links (they are paths/URIs of this device) and the
 * greeting history.
 */
data class BackupData(
    val events: List<SpecialEvent>,
    val rules: List<ReminderRule>,
    val checklist: List<ChecklistItem>,
    val templates: List<GreetingTemplate>,
    val groups: List<EventGroup> = emptyList(),
    /** (event id, group id) pairs, both as numbered inside the file. */
    val members: List<Pair<Long, Long>> = emptyList(),
)

enum class BackupError { NOT_JSON, WRONG_FORMAT, TOO_NEW, TOO_BIG }

sealed interface DecodeResult {
    /** [skipped] counts entries that could not be read and were left out. */
    data class Ok(val data: BackupData, val exportedAt: String?, val skipped: Int, val kind: String = BackupCodec.KIND_BACKUP) : DecodeResult

    data class Failure(val error: BackupError) : DecodeResult
}

/** How an import treats what is already in the app. */
enum class ImportMode { MERGE, REPLACE }

/** What an import would do, shown before anything is changed. */
data class ImportPreview(val newEvents: Int, val duplicateEvents: Int, val newTemplates: Int, val existingEvents: Int)
