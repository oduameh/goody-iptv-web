package com.goody.iptv

import android.app.PictureInPictureParams
import android.content.pm.ActivityInfo
import android.os.Build
import android.os.Bundle
import android.util.Rational
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.TrackSelectionParameters
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.goody.iptv.data.Prefs
import com.goody.iptv.data.PlaylistRepository
import com.goody.iptv.model.Channel
import com.goody.iptv.ui.EpgGrid
import com.goody.iptv.ui.Programme
import com.goody.iptv.ui.fetchXmlTvNowNext
import com.goody.iptv.ui.nowNextFor
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import com.goody.iptv.ui.PlayerControls

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR
        setContent { App() }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (Build.VERSION.SDK_INT >= 26) {
            val params = PictureInPictureParams.Builder()
                .setAspectRatio(Rational(16, 9)).build()
            enterPictureInPictureMode(params)
        }
    }
}

@Composable
fun App() {
    MaterialTheme(colorScheme = MaterialTheme.colorScheme.copy(
        background = Color(0xFF0B0F17),
    )) {
        val context = LocalContext.current
        val prefs = remember { Prefs(context) }
        val repo = remember { PlaylistRepository() }
        val scope = rememberCoroutineScope()
        val snackbar = remember { SnackbarHostState() }

        var playlistUrl by remember { mutableStateOf("") }
        var xmltvUrl by remember { mutableStateOf<String?>(null) }
        var channels by remember { mutableStateOf(listOf<Channel>()) }
        var query by remember { mutableStateOf("") }
        var playing by remember { mutableStateOf<Channel?>(null) }
        var favorites by remember { mutableStateOf(setOf<String>()) }
        var programmes by remember { mutableStateOf<Map<String, List<Programme>>>(emptyMap()) }
        var showEpg by remember { mutableStateOf(false) }

        LaunchedEffect(Unit) {
            playlistUrl = prefs.playlistUrl.first()
            xmltvUrl = prefs.xmltvUrl.first()
            favorites = prefs.favorites.first()
            val last = prefs.lastUrl.first()
            runCatching { channels = repo.load(playlistUrl) }
                .onFailure { snackbar.showSnackbar("Failed to load playlist") }
            if (!xmltvUrl.isNullOrBlank()) {
                runCatching { programmes = fetchXmlTvNowNext(xmltvUrl!!) }
            }
            if (last != null) {
                playing = channels.firstOrNull { it.url == last }
            }
        }

        Scaffold(
            topBar = {
                TopAppBar(title = { Text("Goody IPTV") }, actions = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (!xmltvUrl.isNullOrBlank()) {
                            Text("EPG", modifier = Modifier.padding(end = 6.dp))
                            Switch(checked = showEpg, onCheckedChange = { showEpg = it })
                        }
                    }
                    IconButton(onClick = { TrackDialogController.open() }) { Icon(painterResource(android.R.drawable.ic_menu_sort_by_size), contentDescription = "Tracks") }
                    IconButton(onClick = { SettingsDialogState.open = true }) { Icon(painterResource(android.R.drawable.ic_menu_preferences), contentDescription = "Settings") }
                })
            },
            snackbarHost = { SnackbarHost(hostState = snackbar) }
        ) { inner ->
            Row(Modifier.padding(inner).fillMaxSize()) {
                Column(Modifier.fillMaxHeight().weight(1f).padding(12.dp)) {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Search") }
                    )
                    Spacer(Modifier.height(8.dp))
                    val filtered = channels.filter { c ->
                        val q = query.trim().lowercase()
                        q.isBlank() || c.name.lowercase().contains(q) || (c.group ?: "").lowercase().contains(q)
                    }
                    if (showEpg && programmes.isNotEmpty()) {
                        EpgGrid(channels = filtered, programmes = programmes)
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            itemsIndexed(filtered, key = { _, c -> c.url }) { _, c ->
                                ChannelRow(
                                    channel = c,
                                    isFavorite = favorites.contains(c.url),
                                    now = nowNextFor(c, programmes).first,
                                    next = nowNextFor(c, programmes).second,
                                    onClick = {
                                        playing = c
                                        scope.launch { prefs.setLastUrl(c.url) }
                                    },
                                    onToggleFavorite = {
                                        scope.launch {
                                            if (favorites.contains(c.url)) prefs.removeFavorite(c.url) else prefs.addFavorite(c.url)
                                            favorites = prefs.favorites.first()
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
                Column(Modifier.weight(1.6f).padding(12.dp)) {
                    PlayerPane(channel = playing)
                }
            }
        }

        if (SettingsDialogState.open) {
            SettingsDialog(
                initialPlaylist = playlistUrl,
                initialXmltv = xmltvUrl,
                onDismiss = { SettingsDialogState.open = false },
                onSave = { p, x ->
                    scope.launch {
                        prefs.setPlaylistUrl(p)
                        prefs.setXmltvUrl(x)
                        playlistUrl = p
                        xmltvUrl = x
                        runCatching { channels = repo.load(p) }
                        if (!x.isNullOrBlank()) runCatching { programmes = fetchXmlTvNowNext(x!!) }
                    }
                }
            )
        }
        TrackDialogController.Render()
        PlayerControls.Render()
    }
}

@Composable
fun ChannelRow(
    channel: Channel,
    isFavorite: Boolean,
    now: String,
    next: String,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0x141EA7FF))
            .padding(8.dp)
            .focusable(true),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = channel.logo,
            contentDescription = null,
            modifier = Modifier.height(48.dp),
            contentScale = ContentScale.Fit
        )
        Column(Modifier.padding(start = 8.dp).weight(1f)) {
            Text(channel.name, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(now + " • " + next, maxLines = 1, overflow = TextOverflow.Ellipsis, color = Color(0xFFA7B1C7))
        }
        TextButton(onClick = onToggleFavorite) { Text(if (isFavorite) "★" else "☆") }
        TextButton(onClick = onClick) { Text("Play") }
    }
}

@Composable
fun PlayerPane(channel: Channel?) {
    val context = LocalContext.current
    var player by remember { mutableStateOf<ExoPlayer?>(null) }

    LaunchedEffect(channel) {
        player?.release()
        if (channel == null) return@LaunchedEffect
        val p = ExoPlayer.Builder(context).build()
        val item = MediaItem.Builder()
            .setUri(channel.url)
            .setMimeType(MimeTypes.APPLICATION_M3U8)
            .build()
        p.setMediaItem(item)
        p.prepare()
        p.playWhenReady = true
        player = p
    }

    Column(Modifier.fillMaxSize()) {
        if (player != null) {
            AndroidView(
                modifier = Modifier.fillMaxWidth().weight(1f),
                factory = {
                    PlayerView(it).apply {
                        useController = true
                        this.player = player
                    }
                }
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                TextButton(onClick = { TrackDialogController.showTracks(player) }) { Text("Tracks") }
                TextButton(onClick = { PlayerControls.show(player) }) { Text("Subtitles") }
                TextButton(onClick = { player?.seekTo(Long.MAX_VALUE) }) { Text("Go Live") }
            }
        } else {
            Text("Select a channel", modifier = Modifier.padding(16.dp))
        }
    }
}

object SettingsDialogState { var open by mutableStateOf(false) }

@Composable
fun SettingsDialog(
    initialPlaylist: String,
    initialXmltv: String?,
    onDismiss: () -> Unit,
    onSave: (String, String?) -> Unit
) {
    var p by remember { mutableStateOf(initialPlaylist) }
    var x by remember { mutableStateOf(initialXmltv ?: "") }
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Column(Modifier.background(Color(0xFF121A26)).padding(16.dp)) {
            Text("Settings", fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(value = p, onValueChange = { p = it }, label = { Text("Playlist URL") })
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(value = x, onValueChange = { x = it }, label = { Text("XMLTV URL (optional)") })
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                TextButton(onClick = onDismiss) { Text("Cancel") }
                TextButton(onClick = { onSave(p, x.ifBlank { null }); onDismiss() }) { Text("Save") }
            }
        }
    }
}

object TrackDialogController {
    private val openState: MutableState<Boolean> = mutableStateOf(false)
    private var player: ExoPlayer? = null

    fun open() { openState.value = true }
    fun showTracks(p: ExoPlayer?) { player = p; open() }

    @Composable
    fun Render() {
        if (!openState.value) return
        val p = player
        androidx.compose.ui.window.Dialog(onDismissRequest = { openState.value = false }) {
            Column(Modifier.background(Color(0xFF121A26)).padding(16.dp)) {
                Text("Tracks", fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                if (p == null) {
                    Text("No player")
                } else {
                    val params = p.trackSelectionParameters
                    Text("Audio")
                    Row {
                        TextButton(onClick = {
                            val newParams = params.buildUpon().setPreferredAudioLanguage(null).build()
                            p.trackSelectionParameters = newParams
                        }) { Text("Auto") }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("Subtitles")
                    Row {
                        TextButton(onClick = {
                            val newParams = params.buildUpon()
                                .setPreferredTextLanguage(null)
                                .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
                                .build()
                            p.trackSelectionParameters = newParams
                        }) { Text("Off") }
                    }
                }
                Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                    TextButton(onClick = { openState.value = false }) { Text("Close") }
                }
            }
        }
    }
} 