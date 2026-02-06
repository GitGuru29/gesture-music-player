package com.example.musicplayer.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.ui.graphics.Color
import androidx.palette.graphics.Palette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object PaletteExtractor {
    suspend fun extractColorsFromUri(context: Context, uri: Uri): Map<String, Color> {
        return withContext(Dispatchers.IO) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()

                if (bitmap != null) {
                    val palette = Palette.from(bitmap).generate()
                    
                    // Extract colors with defaults
                    val vibrant = palette.getVibrantColor(0xFF000000.toInt())
                    val darkVibrant = palette.getDarkVibrantColor(0xFF000000.toInt())
                    val lightVibrant = palette.getLightVibrantColor(0xFFFFFFFF.toInt())
                    val muted = palette.getMutedColor(0xFF000000.toInt())

                    mapOf(
                        "vibrant" to Color(vibrant),
                        "darkVibrant" to Color(darkVibrant),
                        "lightVibrant" to Color(lightVibrant),
                        "muted" to Color(muted)
                    )
                } else {
                    emptyMap()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                emptyMap()
            }
        }
    }
}
