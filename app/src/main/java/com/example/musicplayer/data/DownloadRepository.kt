package com.example.musicplayer.data

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

// Search result model
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

    companion object {
        private const val TAG = "DownloadRepo"
        // JioSaavn API instances (fallback chain)
        private val API_INSTANCES = listOf(
            "https://jiosaavn-api-privatecvc2.vercel.app",
            "https://jiosavan-api-with-playlist.vercel.app/api"
        )
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    private val gson = Gson()

    private val _downloadState = MutableStateFlow<DownloadState>(DownloadState.Idle)
    val downloadState: StateFlow<DownloadState> = _downloadState

    // Search songs using JioSaavn API
    suspend fun searchYouTube(query: String): List<YouTubeSearchResult> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Searching for: $query")
            val encodedQuery = java.net.URLEncoder.encode(query, "UTF-8")

            for (baseUrl in API_INSTANCES) {
                try {
                    val url = "$baseUrl/search/songs?query=$encodedQuery&limit=20"
                    Log.d(TAG, "Trying: $url")

                    val request = Request.Builder()
                        .url(url)
                        .header("User-Agent", "MusicPlayer/1.0")
                        .build()

                    val response = client.newCall(request).execute()
                    if (!response.isSuccessful) {
                        Log.w(TAG, "$baseUrl returned ${response.code}")
                        response.close()
                        continue
                    }

                    val body = response.body?.string()
                    if (body.isNullOrEmpty()) continue

                    Log.d(TAG, "Response size: ${body.length}")
                    val json = gson.fromJson(body, JsonObject::class.java)

                    // Handle both old schema (status: SUCCESS) and new schema (success: true)
                    val isOldSchema = json.has("status")
                    val isNewSchema = json.has("success")

                    if (isOldSchema && json.get("status")?.asString != "SUCCESS") {
                        Log.w(TAG, "API status: ${json.get("status")?.asString}")
                        continue
                    }
                    if (isNewSchema && json.get("success")?.asBoolean != true) {
                        Log.w(TAG, "API success: false")
                        continue
                    }
                    if (!isOldSchema && !isNewSchema) {
                        Log.w(TAG, "Unknown API response schema")
                        continue
                    }

                    val data = json.getAsJsonObject("data")
                    val resultsArray = data?.getAsJsonArray("results") ?: continue

                    val results = mutableListOf<YouTubeSearchResult>()
                    for (item in resultsArray) {
                        try {
                            val song = item.asJsonObject

                            val id = song.get("id")?.asString ?: continue
                            val name = song.get("name")?.asString ?: continue

                            // Parse artist name — old schema has "primaryArtists" string,
                            // new schema has "artists.primary" array
                            val artists = if (song.has("primaryArtists")) {
                                song.get("primaryArtists")?.asString ?: "Unknown"
                            } else {
                                song.getAsJsonObject("artists")
                                    ?.getAsJsonArray("primary")
                                    ?.joinToString(", ") {
                                        it.asJsonObject.get("name")?.asString ?: ""
                                    } ?: "Unknown"
                            }

                            // Duration may be string (old) or int (new)
                            val durationSecs = try {
                                song.get("duration")?.asInt ?: 0
                            } catch (e: Exception) {
                                song.get("duration")?.asString?.toIntOrNull() ?: 0
                            }
                            val duration = formatDuration(durationSecs)

                            // Get best available thumbnail — old uses "link", new uses "url"
                            val images = song.getAsJsonArray("image")
                            val thumbnailUrl = images?.lastOrNull()?.asJsonObject?.let { img ->
                                img.get("link")?.asString
                                    ?: img.get("url")?.asString
                                    ?: ""
                            } ?: ""

                            // Get best download URL (320kbps preferred) — old uses "link", new uses "url"
                            val downloadUrls = song.getAsJsonArray("downloadUrl")
                            val bestDownloadUrl = downloadUrls?.lastOrNull()?.asJsonObject?.let { dl ->
                                dl.get("link")?.asString
                                    ?: dl.get("url")?.asString
                                    ?: ""
                            } ?: ""

                            if (bestDownloadUrl.isNotEmpty()) {
                                results.add(
                                    YouTubeSearchResult(
                                        videoId = bestDownloadUrl, // Store download URL as videoId
                                        title = name,
                                        artist = artists,
                                        thumbnailUrl = thumbnailUrl,
                                        duration = duration
                                    )
                                )
                            }
                        } catch (e: Exception) {
                            Log.w(TAG, "Parse song error: ${e.message}")
                        }
                    }

                    Log.d(TAG, "Found ${results.size} results from $baseUrl")
                    return@withContext results

                } catch (e: Exception) {
                    Log.w(TAG, "Instance $baseUrl failed: ${e.message}")
                }
            }

            Log.e(TAG, "All API instances failed")
            emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Search error", e)
            emptyList()
        }
    }

    // Download audio directly from CDN URL
    suspend fun downloadAudio(videoId: String, title: String): Boolean = withContext(Dispatchers.IO) {
        try {
            // videoId is actually the direct download URL from JioSaavn
            val downloadUrl = videoId
            _downloadState.value = DownloadState.Downloading(0, title)
            Log.d(TAG, "Downloading: $title")
            Log.d(TAG, "URL: $downloadUrl")

            val request = Request.Builder()
                .url(downloadUrl)
                .header("User-Agent", "MusicPlayer/1.0")
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                Log.e(TAG, "Download failed: ${response.code}")
                _downloadState.value = DownloadState.Error("Download failed (${response.code})")
                return@withContext false
            }

            val contentLength = response.body?.contentLength() ?: -1L
            Log.d(TAG, "Content length: $contentLength bytes")
            _downloadState.value = DownloadState.Downloading(10, title)

            // Save to Music folder as m4a (JioSaavn provides AAC in mp4 container)
            val sanitizedTitle = title.replace(Regex("[^a-zA-Z0-9\\s\\-_]"), "").trim().take(80)
            val fileName = "$sanitizedTitle.m4a"

            Log.d(TAG, "Saving as: $fileName")

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.Audio.Media.DISPLAY_NAME, fileName)
                    put(MediaStore.Audio.Media.MIME_TYPE, "audio/mp4")
                    put(MediaStore.Audio.Media.RELATIVE_PATH, Environment.DIRECTORY_MUSIC + "/MusicPlayer")
                    put(MediaStore.Audio.Media.IS_PENDING, 1)
                }

                val uri = context.contentResolver.insert(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    contentValues
                ) ?: run {
                    _downloadState.value = DownloadState.Error("Failed to create file")
                    return@withContext false
                }

                try {
                    context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                        val inputStream = response.body?.byteStream()
                            ?: throw Exception("No response body")
                        val buffer = ByteArray(16384)
                        var totalBytesRead = 0L
                        var bytesRead: Int

                        while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                            outputStream.write(buffer, 0, bytesRead)
                            totalBytesRead += bytesRead
                            if (contentLength > 0) {
                                val progress = 10 + ((totalBytesRead.toFloat() / contentLength) * 85).toInt()
                                _downloadState.value = DownloadState.Downloading(
                                    progress.coerceAtMost(95), title
                                )
                            }
                        }
                        Log.d(TAG, "Written $totalBytesRead bytes")
                    }

                    // Mark as complete
                    contentValues.clear()
                    contentValues.put(MediaStore.Audio.Media.IS_PENDING, 0)
                    context.contentResolver.update(uri, contentValues, null, null)
                } catch (e: Exception) {
                    context.contentResolver.delete(uri, null, null)
                    throw e
                }
            } else {
                val musicDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)
                val appDir = File(musicDir, "MusicPlayer")
                if (!appDir.exists()) appDir.mkdirs()

                val file = File(appDir, fileName)
                FileOutputStream(file).use { outputStream ->
                    val inputStream = response.body?.byteStream()
                        ?: throw Exception("No response body")
                    val buffer = ByteArray(16384)
                    var totalBytesRead = 0L
                    var bytesRead: Int

                    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                        outputStream.write(buffer, 0, bytesRead)
                        totalBytesRead += bytesRead
                        if (contentLength > 0) {
                            val progress = 10 + ((totalBytesRead.toFloat() / contentLength) * 85).toInt()
                            _downloadState.value = DownloadState.Downloading(
                                progress.coerceAtMost(95), title
                            )
                        }
                    }
                    Log.d(TAG, "Written $totalBytesRead bytes to ${file.absolutePath}")
                }
            }

            Log.d(TAG, "Download complete: $fileName")
            _downloadState.value = DownloadState.Success(title)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Download error", e)
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
