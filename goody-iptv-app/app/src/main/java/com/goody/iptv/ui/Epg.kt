package com.goody.iptv.ui

import android.util.Xml
import com.goody.iptv.model.Channel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

data class Programme(val channelId: String, val title: String, val start: Long, val stop: Long)

suspend fun fetchXmlTvNowNext(xmlUrl: String): Map<String, List<Programme>> = withContext(Dispatchers.IO) {
    val map = mutableMapOf<String, MutableList<Programme>>()
    runCatching {
        val parser = Xml.newPullParser()
        parser.setInput(URL(xmlUrl).openStream(), null)
        val sdf = SimpleDateFormat("yyyyMMddHHmmss Z", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }
        var event = parser.eventType
        var current: Programme? = null
        var title: String? = null
        var channelAttr: String? = null
        var start: Long = 0
        var stop: Long = 0
        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG -> {
                    when (parser.name) {
                        "programme" -> {
                            channelAttr = parser.getAttributeValue(null, "channel")
                            start = sdf.parse(parser.getAttributeValue(null, "start").replace("Z", " +0000"))?.time ?: 0
                            stop = sdf.parse(parser.getAttributeValue(null, "stop").replace("Z", " +0000"))?.time ?: 0
                        }
                        "title" -> title = null
                    }
                }
                XmlPullParser.TEXT -> {
                    if (parser.text != null && title == null) title = parser.text
                }
                XmlPullParser.END_TAG -> {
                    when (parser.name) {
                        "programme" -> {
                            val p = Programme(channelAttr ?: "", title ?: "", start, stop)
                            map.getOrPut(p.channelId) { mutableListOf() }.add(p)
                            title = null
                        }
                    }
                }
            }
            event = parser.next()
        }
    }.onFailure { /* ignore: optional */ }
    map
}

fun nowNextFor(channel: Channel, programmes: Map<String, List<Programme>>): Pair<String, String> {
    val list = programmes[channel.tvgId]?.sortedBy { it.start } ?: return "Now" to "Next"
    val now = System.currentTimeMillis()
    val current = list.lastOrNull { it.start <= now && it.stop > now }
    val next = list.firstOrNull { it.start > now }
    return (current?.title ?: "Now") to (next?.title ?: "Next")
} 