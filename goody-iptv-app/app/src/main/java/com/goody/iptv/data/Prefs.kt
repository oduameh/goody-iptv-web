package com.goody.iptv.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.remove
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore("goody_prefs")

class Prefs(private val context: Context) {
    companion object Keys {
        val PLAYLIST_URL = stringPreferencesKey("playlist_url")
        val XMLTV_URL = stringPreferencesKey("xmltv_url")
        val FAVORITES = stringSetPreferencesKey("favorites")
        val LAST_URL = stringPreferencesKey("last_url")
    }

    val playlistUrl: Flow<String> = context.dataStore.data.map { it[PLAYLIST_URL] ?: "https://iptv-org.github.io/iptv/countries/ie.m3u" }
    val xmltvUrl: Flow<String?> = context.dataStore.data.map { it[XMLTV_URL] }
    val favorites: Flow<Set<String>> = context.dataStore.data.map { it[FAVORITES] ?: emptySet() }
    val lastUrl: Flow<String?> = context.dataStore.data.map { it[LAST_URL] }

    suspend fun setPlaylistUrl(url: String) { context.dataStore.edit { it[PLAYLIST_URL] = url } }
    suspend fun setXmltvUrl(url: String?) { context.dataStore.edit { if (url.isNullOrBlank()) it.remove(XMLTV_URL) else it[XMLTV_URL] = url } }
    suspend fun addFavorite(url: String) { context.dataStore.edit { it[FAVORITES] = (it[FAVORITES] ?: emptySet()) + url } }
    suspend fun removeFavorite(url: String) { context.dataStore.edit { it[FAVORITES] = (it[FAVORITES] ?: emptySet()) - url } }
    suspend fun setLastUrl(url: String?) { context.dataStore.edit { if (url.isNullOrBlank()) it.remove(LAST_URL) else it[LAST_URL] = url } }
} 