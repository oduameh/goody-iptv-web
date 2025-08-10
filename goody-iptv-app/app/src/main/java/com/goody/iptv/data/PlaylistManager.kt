package com.goody.iptv.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString
import java.util.UUID

private val Context.playlistDataStore by preferencesDataStore("playlist_manager")

@Serializable
data class SavedPlaylist(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val url: String,
    val xmltvUrl: String = "",
    val channelCount: Int = 0,
    val lastUpdated: Long = System.currentTimeMillis(),
    val isActive: Boolean = false
)

class PlaylistManager(private val context: Context) {
    companion object {
        private val PLAYLISTS_KEY = stringPreferencesKey("saved_playlists")
        private val ACTIVE_PLAYLIST_ID = stringPreferencesKey("active_playlist_id")
        
        // Built-in playlists
        val DEFAULT_PLAYLISTS = listOf(
            SavedPlaylist(
                id = "ireland",
                name = "Ireland TV",
                url = "https://iptv-org.github.io/iptv/countries/ie.m3u",
                channelCount = 30
            ),
            SavedPlaylist(
                id = "uk",
                name = "UK TV",
                url = "https://iptv-org.github.io/iptv/countries/uk.m3u",
                channelCount = 50
            ),
            SavedPlaylist(
                id = "us",
                name = "US TV",
                url = "https://iptv-org.github.io/iptv/countries/us.m3u",
                channelCount = 100
            ),
            SavedPlaylist(
                id = "sports",
                name = "Sports Channels",
                url = "https://iptv-org.github.io/iptv/categories/sports.m3u",
                channelCount = 200
            ),
            SavedPlaylist(
                id = "news",
                name = "News Channels",
                url = "https://iptv-org.github.io/iptv/categories/news.m3u",
                channelCount = 150
            )
        )
    }

    val savedPlaylists: Flow<List<SavedPlaylist>> = context.playlistDataStore.data.map { prefs ->
        val playlistsJson = prefs[PLAYLISTS_KEY] ?: ""
        val userPlaylists = if (playlistsJson.isNotEmpty()) {
            try {
                Json.decodeFromString<List<SavedPlaylist>>(playlistsJson)
            } catch (e: Exception) {
                emptyList()
            }
        } else {
            emptyList()
        }
        
        // Combine built-in and user playlists
        DEFAULT_PLAYLISTS + userPlaylists
    }

    val activePlaylistId: Flow<String?> = context.playlistDataStore.data.map { 
        it[ACTIVE_PLAYLIST_ID] 
    }

    suspend fun addPlaylist(name: String, url: String, xmltvUrl: String = ""): SavedPlaylist {
        val newPlaylist = SavedPlaylist(
            name = name,
            url = url,
            xmltvUrl = xmltvUrl
        )

        context.playlistDataStore.edit { prefs ->
            val currentJson = prefs[PLAYLISTS_KEY] ?: "[]"
            val currentPlaylists = try {
                Json.decodeFromString<List<SavedPlaylist>>(currentJson)
            } catch (e: Exception) {
                emptyList()
            }

            val updatedPlaylists = currentPlaylists + newPlaylist
            prefs[PLAYLISTS_KEY] = Json.encodeToString(updatedPlaylists)
        }

        return newPlaylist
    }

    suspend fun removePlaylist(playlistId: String) {
        context.playlistDataStore.edit { prefs ->
            val currentJson = prefs[PLAYLISTS_KEY] ?: "[]"
            val currentPlaylists = try {
                Json.decodeFromString<List<SavedPlaylist>>(currentJson)
            } catch (e: Exception) {
                emptyList()
            }

            val updatedPlaylists = currentPlaylists.filter { it.id != playlistId }
            prefs[PLAYLISTS_KEY] = Json.encodeToString(updatedPlaylists)
            
            // If we're removing the active playlist, clear the active selection
            if (prefs[ACTIVE_PLAYLIST_ID] == playlistId) {
                prefs.remove(ACTIVE_PLAYLIST_ID)
            }
        }
    }

    suspend fun updatePlaylist(playlist: SavedPlaylist) {
        context.playlistDataStore.edit { prefs ->
            val currentJson = prefs[PLAYLISTS_KEY] ?: "[]"
            val currentPlaylists = try {
                Json.decodeFromString<List<SavedPlaylist>>(currentJson)
            } catch (e: Exception) {
                emptyList()
            }

            val updatedPlaylists = currentPlaylists.map { 
                if (it.id == playlist.id) playlist else it 
            }
            prefs[PLAYLISTS_KEY] = Json.encodeToString(updatedPlaylists)
        }
    }

    suspend fun setActivePlaylist(playlistId: String) {
        context.playlistDataStore.edit { prefs ->
            prefs[ACTIVE_PLAYLIST_ID] = playlistId
        }
    }

    suspend fun getPlaylistById(id: String): SavedPlaylist? {
        return savedPlaylists.map { playlists ->
            playlists.find { it.id == id }
        }.kotlinx.coroutines.flow.first()
    }

    suspend fun updateChannelCount(playlistId: String, count: Int) {
        val playlist = getPlaylistById(playlistId) ?: return
        val updatedPlaylist = playlist.copy(
            channelCount = count,
            lastUpdated = System.currentTimeMillis()
        )
        updatePlaylist(updatedPlaylist)
    }

    suspend fun searchPlaylists(query: String): List<SavedPlaylist> {
        return savedPlaylists.map { playlists ->
            playlists.filter { playlist ->
                playlist.name.contains(query, ignoreCase = true) ||
                playlist.url.contains(query, ignoreCase = true)
            }
        }.kotlinx.coroutines.flow.first()
    }

    suspend fun getPlaylistStats(): PlaylistStats {
        val playlists = savedPlaylists.kotlinx.coroutines.flow.first()
        return PlaylistStats(
            totalPlaylists = playlists.size,
            totalChannels = playlists.sumOf { it.channelCount },
            userPlaylists = playlists.count { !DEFAULT_PLAYLISTS.any { default -> default.id == it.id } },
            lastUpdated = playlists.maxOfOrNull { it.lastUpdated } ?: 0L
        )
    }
}

@Serializable
data class PlaylistStats(
    val totalPlaylists: Int,
    val totalChannels: Int,
    val userPlaylists: Int,
    val lastUpdated: Long
) 