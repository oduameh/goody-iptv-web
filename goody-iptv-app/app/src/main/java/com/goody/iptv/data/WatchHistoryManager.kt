package com.goody.iptv.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.goody.iptv.model.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString

private val Context.watchHistoryDataStore by preferencesDataStore("watch_history")

@Serializable
data class WatchHistoryItem(
    val channelId: String,
    val channelName: String,
    val channelUrl: String,
    val lastWatchedTime: Long,
    val resumePosition: Long = 0L, // For future use with VOD content
    val watchDuration: Long = 0L,
    val channelGroup: String = ""
)

class WatchHistoryManager(private val context: Context) {
    companion object {
        private val WATCH_HISTORY = stringPreferencesKey("watch_history_json")
        private val LAST_CHANNEL = stringPreferencesKey("last_channel_id")
        private val TOTAL_WATCH_TIME = longPreferencesKey("total_watch_time")
        
        private const val MAX_HISTORY_ITEMS = 50
    }

    val watchHistory: Flow<List<WatchHistoryItem>> = context.watchHistoryDataStore.data.map { prefs ->
        val historyJson = prefs[WATCH_HISTORY] ?: "[]"
        try {
            Json.decodeFromString<List<WatchHistoryItem>>(historyJson)
        } catch (e: Exception) {
            emptyList()
        }
    }

    val lastChannel: Flow<String?> = context.watchHistoryDataStore.data.map { 
        it[LAST_CHANNEL] 
    }

    val totalWatchTime: Flow<Long> = context.watchHistoryDataStore.data.map { 
        it[TOTAL_WATCH_TIME] ?: 0L 
    }

    suspend fun recordChannelWatch(channel: Channel, watchDuration: Long = 0L) {
        context.watchHistoryDataStore.edit { prefs ->
            // Update watch history
            val currentHistoryJson = prefs[WATCH_HISTORY] ?: "[]"
            val currentHistory = try {
                Json.decodeFromString<List<WatchHistoryItem>>(currentHistoryJson)
            } catch (e: Exception) {
                emptyList()
            }

            val newItem = WatchHistoryItem(
                channelId = channel.tvgId.ifEmpty { channel.name },
                channelName = channel.name,
                channelUrl = channel.url,
                lastWatchedTime = System.currentTimeMillis(),
                watchDuration = watchDuration,
                channelGroup = channel.group
            )

            // Remove existing entry for this channel and add new one at the front
            val updatedHistory = (listOf(newItem) + currentHistory.filter { 
                it.channelId != newItem.channelId 
            }).take(MAX_HISTORY_ITEMS)

            prefs[WATCH_HISTORY] = Json.encodeToString(updatedHistory)
            prefs[LAST_CHANNEL] = newItem.channelId
            
            // Update total watch time
            val currentTotal = prefs[TOTAL_WATCH_TIME] ?: 0L
            prefs[TOTAL_WATCH_TIME] = currentTotal + watchDuration
        }
    }

    suspend fun getRecentChannels(limit: Int = 10): List<WatchHistoryItem> {
        return watchHistory.map { it.take(limit) }.kotlinx.coroutines.flow.first()
    }

    suspend fun getMostWatchedChannels(limit: Int = 10): List<WatchHistoryItem> {
        return watchHistory.map { history ->
            // Group by channel and sum watch durations
            history.groupBy { it.channelId }
                .map { (_, items) ->
                    items.maxByOrNull { it.lastWatchedTime }?.copy(
                        watchDuration = items.sumOf { it.watchDuration }
                    )
                }
                .filterNotNull()
                .sortedByDescending { it.watchDuration }
                .take(limit)
        }.kotlinx.coroutines.flow.first()
    }

    suspend fun clearHistory() {
        context.watchHistoryDataStore.edit { prefs ->
            prefs.remove(WATCH_HISTORY)
            prefs.remove(LAST_CHANNEL)
        }
    }

    suspend fun getWatchStats(): WatchStats {
        val history = watchHistory.kotlinx.coroutines.flow.first()
        val totalTime = totalWatchTime.kotlinx.coroutines.flow.first()
        
        return WatchStats(
            totalChannelsWatched = history.distinctBy { it.channelId }.size,
            totalWatchTimeMs = totalTime,
            mostWatchedCategory = history.groupBy { it.channelGroup }
                .maxByOrNull { it.value.sumOf { item -> item.watchDuration } }?.key ?: "",
            averageSessionLength = if (history.isNotEmpty()) totalTime / history.size else 0L
        )
    }
}

@Serializable
data class WatchStats(
    val totalChannelsWatched: Int,
    val totalWatchTimeMs: Long,
    val mostWatchedCategory: String,
    val averageSessionLength: Long
) {
    val totalWatchTimeHours: Double
        get() = totalWatchTimeMs / (1000.0 * 60.0 * 60.0)
} 