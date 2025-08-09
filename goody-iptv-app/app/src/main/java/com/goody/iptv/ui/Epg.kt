package com.goody.iptv.ui

import android.util.Xml
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.goody.iptv.model.Channel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import java.net.URL
import java.text.SimpleDateFormat
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

@Composable
fun EpgGrid(channels: List<Channel>, programmes: Map<String, List<Programme>>) {
    Column(Modifier.padding(12.dp)) {
        Text("EPG", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        // Header row
        Row(Modifier.fillMaxWidth().background(Color(0x221EA7FF)).padding(8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Channel", modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
            Text("Now", modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
            Text("Next", modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.height(6.dp))
        val scroll = rememberScrollState()
        Column(Modifier.horizontalScroll(scroll)) {
            channels.forEach { ch ->
                val (now, next) = nowNextFor(ch, programmes)
                Row(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                    Text(ch.name, modifier = Modifier.weight(1f).padding(end = 8.dp), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(now, modifier = Modifier.weight(1f).padding(end = 8.dp), maxLines = 1, overflow = TextOverflow.Ellipsis, color = Color(0xFFA7B1C7))
                    Text(next, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis, color = Color(0xFF7EA4D9))
                }
            }
        }
    }
} 