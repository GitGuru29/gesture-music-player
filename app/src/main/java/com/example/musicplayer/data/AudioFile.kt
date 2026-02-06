package com.example.musicplayer.data

import android.net.Uri

data class AudioFile(
    val id: Long,
    val title: String,
    val artist: String,
    val uri: Uri,
    val albumArtUri: Uri? = null
)
