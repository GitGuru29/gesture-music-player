package com.example.musicplayer.data

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

// YouTube search result model
data class YouTubeSearchResult(
    val videoId: String,
    val title: String,
    val artist: String,
    val thumbnailUrl: String,
    val duration: String
)

// Download state
sealed class DownloadState {
    object Idle : DownloadState()
    data class Downloading(val progress: Int, val title: String) : DownloadState()
    data class Success(val title: String) : DownloadState()
    data class Error(val message: String) : DownloadState()
}

class DownloadRepository(private val context: Context) {
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()
    
    private val gson = Gson()
    
    private val _downloadState = MutableStateFlow<DownloadState>(DownloadState.Idle)
    val downloadState: StateFlow<DownloadState> = _downloadState
    
    // Search YouTube using Invidious API (privacy-friendly YouTube frontend)
    suspend fun searchYouTube(query: String): List<YouTubeSearchResult> = withContext(Dispatchers.IO) {
        try {
            // Using Invidious public API
            val encodedQuery = java.net.URLEncoder.encode(query, "UTF-8")
            val url = "https://inv.nadeko.net/api/v1/search?q=$encodedQuery&type=video"
            
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "MusicPlayer/1.0")
                .build()
            
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return@withContext emptyList()
            
            val body = response.body?.string() ?: return@withContext emptyList()
            
            // Parse Invidious response
            val results = gson.fromJson(body, Array<InvidiousResult>::class.java)
            
            results.take(20).map { result ->
                YouTubeSearchResult(
                    videoId = result.videoId,
                    title = result.title,
                    artist = result.author,
                    thumbnailUrl = result.videoThumbnails.firstOrNull()?.url ?: "",
                    duration = formatDuration(result.lengthSeconds)
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
    
    // Download audio using cobalt.tools API
    suspend fun downloadAudio(videoId: String, title: String): Boolean = withContext(Dispatchers.IO) {
        try {
            _downloadState.value = DownloadState.Downloading(0, title)
            
            // Get download URL from cobalt.tools
            val cobaltUrl = "https://api.cobalt.tools/api/json"
            val youtubeUrl = "https://www.youtube.com/watch?v=$videoId"
            
            val jsonBody = """{"url":"$youtubeUrl","vCodec":"h264","vQuality":"720","aFormat":"mp3","isAudioOnly":true}"""
            
            val request = Request.Builder()
                .url(cobaltUrl)
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .post(okhttp3.RequestBody.create("application/json".toMediaTypeOrNull(), jsonBody))
                .build()
            
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                _downloadState.value = DownloadState.Error("Failed to get download link")
                return@withContext false
            }
            
            val body = response.body?.string() ?: ""
            val cobaltResponse = gson.fromJson(body, CobaltResponse::class.java)
            
            if (cobaltResponse.status != "stream" && cobaltResponse.status != "redirect") {
                _downloadState.value = DownloadState.Error(cobaltResponse.text ?: "Download failed")
                return@withContext false
            }
            
            val downloadUrl = cobaltResponse.url ?: return@withContext false
            
            // Download the file
            _downloadState.value = DownloadState.Downloading(20, title)
            
            val downloadRequest = Request.Builder()
                .url(downloadUrl)
                .build()
            
            val downloadResponse = client.newCall(downloadRequest).execute()
            if (!downloadResponse.isSuccessful) {
                _downloadState.value = DownloadState.Error("Download failed")
                return@withContext false
            }
            
            _downloadState.value = DownloadState.Downloading(50, title)
            
            // Save to Music folder
            val sanitizedTitle = title.replace(Regex("[^a-zA-Z0-9\\s]"), "").take(50)
            val fileName = "$sanitizedTitle.mp3"
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Use MediaStore for Android 10+
                val contentValues = ContentValues().apply {
                    put(MediaStore.Audio.Media.DISPLAY_NAME, fileName)
                    put(MediaStore.Audio.Media.MIME_TYPE, "audio/mpeg")
                    put(MediaStore.Audio.Media.RELATIVE_PATH, Environment.DIRECTORY_MUSIC + "/MusicPlayer")
                    put(MediaStore.Audio.Media.IS_PENDING, 1)
                }
                
                val uri = context.contentResolver.insert(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    contentValues
                ) ?: return@withContext false
                
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    downloadResponse.body?.byteStream()?.copyTo(outputStream)
                }
                
                _downloadState.value = DownloadState.Downloading(90, title)
                
                // Mark as complete
                contentValues.clear()
                contentValues.put(MediaStore.Audio.Media.IS_PENDING, 0)
                context.contentResolver.update(uri, contentValues, null, null)
            } else {
                // Legacy storage for older Android
                val musicDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)
                val appDir = File(musicDir, "MusicPlayer")
                if (!appDir.exists()) appDir.mkdirs()
                
                val file = File(appDir, fileName)
                FileOutputStream(file).use { outputStream ->
                    downloadResponse.body?.byteStream()?.copyTo(outputStream)
                }
                
                _downloadState.value = DownloadState.Downloading(90, title)
            }
            
            _downloadState.value = DownloadState.Success(title)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            _downloadState.value = DownloadState.Error(e.message ?: "Unknown error")
            false
        }
    }
    
    fun resetDownloadState() {
        _downloadState.value = DownloadState.Idle
    }
    
    private fun formatDuration(seconds: Int): String {
        val minutes = seconds / 60
        val secs = seconds % 60
        return String.format("%d:%02d", minutes, secs)
    }
}

// Invidious API response models
data class InvidiousResult(
    val videoId: String,
    val title: String,
    val author: String,
    val lengthSeconds: Int,
    val videoThumbnails: List<InvidiousThumbnail>
)

data class InvidiousThumbnail(
    val url: String,
    val quality: String
)

// Cobalt API response
data class CobaltResponse(
    val status: String,
    val url: String?,
    val text: String?
)
