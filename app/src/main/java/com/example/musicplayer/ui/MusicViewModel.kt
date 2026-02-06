package com.example.musicplayer.ui

import android.app.Application
import android.content.ComponentName
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.example.musicplayer.data.AudioFile
import com.example.musicplayer.data.AudioRepository
import com.example.musicplayer.service.PlaybackService
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MusicState(
    val currentSong: AudioFile? = null,
    val isPlaying: Boolean = false,
    val playlist: List<AudioFile> = emptyList(),
    val currentPosition: Long = 0L,
    val duration: Long = 1L, // Prevent div by zero
    val isShuffleEnabled: Boolean = false,
    val repeatMode: Int = Player.REPEAT_MODE_OFF,
    val paletteColors: Map<String, Color> = emptyMap()
)

class MusicViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = AudioRepository(application)
    private var player: Player? = null // Now generic Player interface from Media3
    
    private val _uiState = MutableStateFlow(MusicState())
    val uiState: StateFlow<MusicState> = _uiState.asStateFlow()

    init {
        // Bind to Service
        val sessionToken = SessionToken(application, ComponentName(application, PlaybackService::class.java))
        val controllerFuture = MediaController.Builder(application, sessionToken).buildAsync()
        
        controllerFuture.addListener({
            try {
                // Connection successful
                val p = controllerFuture.get()
                player = p
                
                // Setup Listener on the Controller
                p.addListener(object : Player.Listener {
                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                        _uiState.update { it.copy(isPlaying = isPlaying) }
                    }

                    override fun onPlaybackStateChanged(playbackState: Int) {
                         if (playbackState == Player.STATE_READY) {
                            _uiState.update { it.copy(duration = p.duration.coerceAtLeast(1L)) }
                        }
                    }
                    
                    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                        super.onMediaItemTransition(mediaItem, reason)
                        updateCurrentSong()
                    }

                    override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
                        _uiState.update { it.copy(isShuffleEnabled = shuffleModeEnabled) }
                    }

                    override fun onRepeatModeChanged(repeatMode: Int) {
                        _uiState.update { it.copy(repeatMode = repeatMode) }
                    }
                })
                
                // sync initial state
                _uiState.update { 
                    it.copy(
                        isPlaying = p.isPlaying,
                        isShuffleEnabled = p.shuffleModeEnabled,
                        repeatMode = p.repeatMode,
                        duration = p.duration.coerceAtLeast(1L)
                    )
                }
                updateCurrentSong()
                
                loadAudioFiles()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, androidx.core.content.ContextCompat.getMainExecutor(application))
        
        // Timer loop for progress
        viewModelScope.launch {
            while (true) {
                if (player?.isPlaying == true) {
                     _uiState.update { it.copy(currentPosition = player?.currentPosition ?: 0L) }
                }
                kotlinx.coroutines.delay(50L)
            }
        }
    }
    
    private fun updateCurrentSong() {
        val p = player ?: return
        val currentMediaItemIndex = p.currentMediaItemIndex
        val playlist = _uiState.value.playlist
        
        // Note: Controller might not have the full playlist "objects", only MediaItems.
        // But we sync via index if the playlist hasn't changed structure.
        if (currentMediaItemIndex in playlist.indices) {
            val song = playlist[currentMediaItemIndex]
            if (song.id != _uiState.value.currentSong?.id) {
                 extractColors(song)
            }
            _uiState.update { it.copy(currentSong = song) }
        }
    }

    private fun extractColors(song: AudioFile) {
        if (song.albumArtUri != null) {
            viewModelScope.launch {
                val colors = PaletteExtractor.extractColorsFromUri(getApplication(), song.albumArtUri)
                _uiState.update { it.copy(paletteColors = colors) }
            }
        } else {
             _uiState.update { it.copy(paletteColors = emptyMap()) }
        }
    }

    private fun loadAudioFiles() {
        viewModelScope.launch {
            val files = repository.getAudioFiles()
            if (files.isNotEmpty()) {
                _uiState.update { it.copy(playlist = files) }
                setupPlayer(files)
            }
        }
    }

    private fun setupPlayer(files: List<AudioFile>) {
        val p = player ?: return
        // Only set items if player is empty to avoid resetting on rotation/service reconnect
        if (p.mediaItemCount == 0) {
            p.clearMediaItems()
            files.forEach { file ->
                val mediaItem = MediaItem.fromUri(file.uri)
                p.addMediaItem(mediaItem)
            }
            p.prepare()
            if (files.isNotEmpty()) {
                 updateCurrentSong()
            }
        }
    }

    fun playSong(song: AudioFile) {
        val p = player ?: return
        val index = _uiState.value.playlist.indexOfFirst { it.id == song.id }
        if (index != -1) {
            p.seekToDefaultPosition(index)
            p.prepare()
            p.play()
        }
    }

    fun playPause() {
        val p = player ?: return
        if (p.isPlaying) {
            p.pause()
        } else {
            p.play()
        }
    }

    fun next() {
        val p = player ?: return
        if (p.hasNextMediaItem()) {
            p.seekToNextMediaItem()
        }
    }

    fun previous() {
        val p = player ?: return
        if (p.hasPreviousMediaItem()) {
            p.seekToPreviousMediaItem()
        } else {
             p.seekTo(0)
        }
    }

    fun seekTo(position: Long) {
        player?.seekTo(position)
    }

    fun toggleShuffle() {
        val p = player ?: return
        p.shuffleModeEnabled = !p.shuffleModeEnabled
    }
    
    fun toggleRepeat() {
        val p = player ?: return
        // OFF -> ONE -> ALL -> OFF
        val newMode = when (p.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ONE
            Player.REPEAT_MODE_ONE -> Player.REPEAT_MODE_ALL
            else -> Player.REPEAT_MODE_OFF
        }
        p.repeatMode = newMode
    }

    fun volumeUp() {
        val audioManager = getApplication<Application>().getSystemService(android.content.Context.AUDIO_SERVICE) as android.media.AudioManager
        audioManager.adjustStreamVolume(android.media.AudioManager.STREAM_MUSIC, android.media.AudioManager.ADJUST_RAISE, android.media.AudioManager.FLAG_SHOW_UI)
    }

    fun volumeDown() {
        val audioManager = getApplication<Application>().getSystemService(android.content.Context.AUDIO_SERVICE) as android.media.AudioManager
        audioManager.adjustStreamVolume(android.media.AudioManager.STREAM_MUSIC, android.media.AudioManager.ADJUST_LOWER, android.media.AudioManager.FLAG_SHOW_UI)
    }
}
