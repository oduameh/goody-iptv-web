package com.goody.iptv.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.media3.common.C
import androidx.media3.common.TrackGroupArray
import androidx.media3.common.TrackSelectionParameters
import androidx.media3.exoplayer.ExoPlayer

object PlayerControls {
    private val openState: MutableState<Boolean> = mutableStateOf(false)
    private var player: ExoPlayer? = null

    fun show(p: ExoPlayer?) { player = p; openState.value = true }

    @Composable
    fun Render() {
        if (!openState.value) return
        val p = player
        androidx.compose.ui.window.Dialog(onDismissRequest = { openState.value = false }) {
            Column(Modifier.padding(16.dp)) {
                Text("Track selection")
                Spacer(Modifier.height(8.dp))
                if (p == null) {
                    Text("No player")
                } else {
                    val params = p.trackSelectionParameters
                    Text("Subtitles")
                    Row(Modifier.fillMaxWidth()) {
                        TextButton(onClick = {
                            val newParams = params.buildUpon().setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true).build()
                            p.trackSelectionParameters = newParams
                        }) { Text("Off") }
                        TextButton(onClick = {
                            val newParams = params.buildUpon().setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false).build()
                            p.trackSelectionParameters = newParams
                        }) { Text("Auto") }
                    }
                }
                Row(Modifier.fillMaxWidth()) {
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = { openState.value = false }) { Text("Close") }
                }
            }
        }
    }
} 