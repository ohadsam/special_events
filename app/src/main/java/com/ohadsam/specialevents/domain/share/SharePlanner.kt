package com.ohadsam.specialevents.domain.share

import com.ohadsam.specialevents.domain.backup.BackupData

/** What goes along with a shared event. Dates and the title are always shared; everything else is the sender's choice. */
enum class ShareField { NOTES, CONTACT, TAGS, STYLE, REMINDERS, CHECKLIST, GROUPS }

/** Builds the content of a share file from the owner's data. Pure. */
object SharePlanner {
    val DEFAULT_FIELDS: Set<ShareField> = setOf(ShareField.TAGS, ShareField.STYLE, ShareField.REMINDERS)

    /**
     * [all] is the sender's whole data (as a backup would hold it). Photos and contact links never leave: they are paths on this device.
     * The checklist is shared without ticks, so the receiver starts fresh. Rules that point at groups only keep them when the
     * groups are shared too.
     */
    fun build(eventIds: Set<Long>, all: BackupData, fields: Set<ShareField>): BackupData {
        val events = all.events.filter { it.id in eventIds }.map { e ->
            e.copy(
                notes = e.notes.takeIf { ShareField.NOTES in fields },
                contactPhone = e.contactPhone.takeIf { ShareField.CONTACT in fields },
                contactEmail = e.contactEmail.takeIf { ShareField.CONTACT in fields },
                tags = if (ShareField.TAGS in fields) e.tags else emptyList(),
                colorArgb = e.colorArgb.takeIf { ShareField.STYLE in fields },
                iconKey = e.iconKey.takeIf { ShareField.STYLE in fields },
                photoUri = null,
                contactLookupUri = null,
            )
        }
        val chosen = events.map { it.id }.toSet()
        val members = if (ShareField.GROUPS in fields) all.members.filter { (e, _) -> e in chosen } else emptyList()
        val groupIds = members.map { it.second }.toSet()
        return BackupData(
            events = events,
            rules = if (ShareField.REMINDERS in fields) {
                all.rules.filter { it.eventId in chosen }.map { r -> r.copy(toGroupIds = r.toGroupIds.filter { it in groupIds }.toSet()) }
            } else {
                emptyList()
            },
            checklist = if (ShareField.CHECKLIST in fields) all.checklist.filter { it.eventId in chosen }.map { it.copy(done = false) } else emptyList(),
            templates = emptyList(),
            groups = all.groups.filter { it.id in groupIds },
            members = members,
        )
    }
}
