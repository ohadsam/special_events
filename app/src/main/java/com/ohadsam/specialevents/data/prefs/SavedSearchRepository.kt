package com.ohadsam.specialevents.data.prefs

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.ohadsam.specialevents.domain.search.SavedSearch
import com.ohadsam.specialevents.domain.search.SearchCodec
import com.ohadsam.specialevents.domain.search.SearchStep
import java.net.URLDecoder
import java.net.URLEncoder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Saved searches and the last typed phrases. Kept apart from [SettingsRepository] so "reset settings" never deletes them. */
class SavedSearchRepository(private val store: DataStore<Preferences>) {
    val saved: Flow<List<SavedSearch>> = store.data.map { SearchCodec.decodeSaved(it[SAVED].orEmpty()) }

    val recent: Flow<List<String>> = store.data.map { p ->
        p[RECENT].orEmpty().split(SEP).filter { it.isNotBlank() }.map { URLDecoder.decode(it, "UTF-8") }
    }

    /** Saving under an existing name replaces that search. */
    suspend fun save(name: String, steps: List<SearchStep>) {
        val clean = name.trim().ifEmpty { return }
        store.edit { p ->
            val all = SearchCodec.decodeSaved(p[SAVED].orEmpty()).filterNot { it.name == clean } + SavedSearch(clean, steps)
            p[SAVED] = SearchCodec.encodeSaved(all)
        }
    }

    suspend fun delete(name: String) {
        store.edit { p -> p[SAVED] = SearchCodec.encodeSaved(SearchCodec.decodeSaved(p[SAVED].orEmpty()).filterNot { it.name == name }) }
    }

    suspend fun addRecent(phrase: String) {
        val clean = phrase.trim().ifEmpty { return }
        store.edit { p ->
            val current = p[RECENT].orEmpty().split(SEP).filter { it.isNotBlank() }.map { URLDecoder.decode(it, "UTF-8") }
            p[RECENT] = (listOf(clean) + current.filterNot { it == clean }).take(MAX_RECENT).joinToString(SEP) { URLEncoder.encode(it, "UTF-8") }
        }
    }

    suspend fun clearRecent() {
        store.edit { it.remove(RECENT) }
    }

    private companion object {
        val SAVED = stringPreferencesKey("saved_searches")
        val RECENT = stringPreferencesKey("recent_searches")
        const val SEP = ","
        const val MAX_RECENT = 8
    }
}
