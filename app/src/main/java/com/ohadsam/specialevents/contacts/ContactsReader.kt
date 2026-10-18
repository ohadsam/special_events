package com.ohadsam.specialevents.contacts

import android.content.ContentResolver
import android.net.Uri
import android.provider.ContactsContract
import android.provider.ContactsContract.CommonDataKinds.Email
import android.provider.ContactsContract.CommonDataKinds.Event
import android.provider.ContactsContract.CommonDataKinds.Phone
import com.ohadsam.specialevents.domain.EventType

/** What the contact picker gave us about one person. */
data class PickedContact(val lookupUri: String, val name: String, val phone: String?, val email: String?, val photo: ByteArray?)

/** Reads the address book. Needs READ_CONTACTS except for [readPicked], which uses the one-off access the picker grants. */
class ContactsReader(private val resolver: ContentResolver) {
    /** A contact chosen with the system picker: no permission needed, so everything useful is copied now. */
    fun readPicked(contactUri: Uri): PickedContact? {
        var id: Long? = null
        var lookupKey: String? = null
        var name = ""
        resolver.query(contactUri, arrayOf(ContactsContract.Contacts._ID, ContactsContract.Contacts.LOOKUP_KEY, ContactsContract.Contacts.DISPLAY_NAME_PRIMARY), null, null, null)?.use { c ->
            if (c.moveToFirst()) {
                id = c.getLong(0)
                lookupKey = c.getString(1)
                name = c.getString(2).orEmpty()
            }
        }
        val contactId = id ?: return null
        val photo = runCatching {
            ContactsContract.Contacts.openContactPhotoInputStream(resolver, ContentUriFor.contact(contactId), true)?.use { it.readBytes() }
        }.getOrNull()
        return PickedContact(
            lookupUri = ContactImport.lookupUriOf(lookupKey ?: return null),
            name = name,
            phone = firstValue(Phone.CONTENT_URI, Phone.NUMBER, Phone.CONTACT_ID, contactId),
            email = firstValue(Email.CONTENT_URI, Email.ADDRESS, Email.CONTACT_ID, contactId),
            photo = photo,
        )
    }

    /** All birthdays, anniversaries and other dates in the address book, with each person's first phone and e-mail. */
    fun readCandidates(): List<ContactCandidate> {
        val phones = firstByContact(Phone.CONTENT_URI, Phone.CONTACT_ID, Phone.NUMBER)
        val emails = firstByContact(Email.CONTENT_URI, Email.CONTACT_ID, Email.ADDRESS)
        val out = ArrayList<ContactCandidate>()
        val projection = arrayOf(
            ContactsContract.Data.CONTACT_ID, ContactsContract.Data.LOOKUP_KEY, ContactsContract.Data.DISPLAY_NAME_PRIMARY,
            Event.START_DATE, Event.TYPE, Event.LABEL, ContactsContract.Data.PHOTO_THUMBNAIL_URI,
        )
        resolver.query(ContactsContract.Data.CONTENT_URI, projection, "${ContactsContract.Data.MIMETYPE}=?", arrayOf(Event.CONTENT_ITEM_TYPE), null)?.use { c ->
            while (c.moveToNext()) {
                val date = ContactDateParser.parse(c.getString(3)) ?: continue
                val kind = when (c.getInt(4)) {
                    Event.TYPE_BIRTHDAY -> EventType.BIRTHDAY
                    Event.TYPE_ANNIVERSARY -> EventType.ANNIVERSARY
                    else -> EventType.CUSTOM
                }
                val id = c.getLong(0)
                out.add(
                    ContactCandidate(id, c.getString(1).orEmpty(), c.getString(2).orEmpty(), kind, c.getString(5), date, phones[id], emails[id], c.getString(6)),
                )
            }
        }
        return out.filter { it.lookupKey.isNotEmpty() && it.name.isNotBlank() }.distinctBy { Triple(it.contactId, it.kind, it.date) }
    }

    fun readThumb(uri: String): ByteArray? = runCatching { resolver.openInputStream(Uri.parse(uri))?.use { it.readBytes() } }.getOrNull()

    private fun firstValue(uri: Uri, column: String, idColumn: String, contactId: Long): String? =
        resolver.query(uri, arrayOf(column), "$idColumn=?", arrayOf(contactId.toString()), null)?.use { c -> if (c.moveToFirst()) c.getString(0) else null }

    private fun firstByContact(uri: Uri, idColumn: String, valueColumn: String): Map<Long, String> {
        val map = HashMap<Long, String>()
        resolver.query(uri, arrayOf(idColumn, valueColumn), null, null, null)?.use { c ->
            while (c.moveToNext()) {
                val v = c.getString(1)
                if (!v.isNullOrBlank()) map.putIfAbsent(c.getLong(0), v)
            }
        }
        return map
    }

    private object ContentUriFor {
        fun contact(id: Long): Uri = android.content.ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, id)
    }
}
