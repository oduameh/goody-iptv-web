package com.goody.iptv.data

import com.goody.iptv.model.Channel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

class PlaylistRepository {
    suspend fun load(url: String): List<Channel> = withContext(Dispatchers.IO) {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 10000
            readTimeout = 15000
            setRequestProperty("Cache-Control", "no-cache")
        }
        conn.inputStream.bufferedReader().use { reader ->
            parseM3U(reader.readText())
        }
    }

    private fun parseM3U(text: String): List<Channel> {
        val lines = text.split("\n")
        val out = mutableListOf<Channel>()
        var meta: MutableMap<String, String>? = null
        for (raw in lines) {
            val line = raw.trim()
            if (line.isEmpty()) continue
            if (line.startsWith("#EXTM3U")) continue
            if (line.startsWith("#EXTINF:")) {
                meta = mutableMapOf("name" to "", "logo" to "", "group" to "", "tvgId" to "")
                val comma = line.indexOf(',')
                val attrs = line.substring(8, if (comma > -1) comma else line.length)
                val name = if (comma > -1) line.substring(comma + 1).trim() else ""
                meta["name"] = name
                val re = Regex("([a-zA-Z0-9-]+)=\"(.*?)\"")
                re.findAll(attrs).forEach { m ->
                    val k = m.groupValues[1]
                    val v = m.groupValues[2]
                    when (k) {
                        "tvg-logo" -> meta["logo"] = v
                        "group-title" -> meta["group"] = v
                        "tvg-id" -> meta["tvgId"] = v
                    }
                }
            } else if (meta != null && !line.startsWith("#")) {
                out += Channel(
                    name = meta["name"].orEmpty().ifEmpty { line },
                    url = line,
                    logo = meta["logo"].orEmpty().ifBlank { null },
                    group = meta["group"].orEmpty().ifBlank { null },
                    tvgId = meta["tvgId"].orEmpty().ifBlank { null }
                )
                meta = null
            }
        }
        return out
    }
} 