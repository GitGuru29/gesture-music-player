package com.example.musicplayer.ui

import android.app.Application
import android.content.ComponentName
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.example.musicplayer.data.*
import com.example.musicplayer.service.PlaybackService
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

// Tab options
enum class LibraryTab {
    ALL, FAVORITES
}

data class MusicState(
    val currentSong: AudioFile? = null,
    val isPlaying: Boolean = false,
    val playlist: List<AudioFile> = emptyList(),
    val currentPosition: Long = 0L,
    val duration: Long = 1L,
    val isShuffleEnabled: Boolean = false,
    val repeatMode: Int = Player.REPEAT_MODE_OFF,
    val paletteColors: Map<String, Color> = emptyMap(),
    // New state
    val searchQuery: String = "",
    val selectedTab: LibraryTab = LibraryTab.ALL,
    val favoriteIds: Set<Long> = emptySet(),
    val isSearchActive: Boolean = false
)

// Downloader state
data class DownloaderState(
    val searchQuery: String = "",
    val searchResults: List<YouTubeSearchResult> = emptyList(),
    val isSearching: Boolean = false,
    val downloadState: DownloadState = DownloadState.Idle
)

class MusicViewModel(application: Application) : AndroidViewModel(application) {

    private val audioRepository = AudioRepository(application)
    private val favoritesRepository = FavoritesRepository(application)
    private val downloadRepository = DownloadRepository(application)
    
    private var player: Player? = null
    
    private val _uiState = MutableStateFlow(MusicState())
    val uiState: StateFlow<MusicState> = _uiState.asStateFlow()
    
    private val _downloaderState = MutableStateFlow(DownloaderState())
    val downloaderState: StateFlow<DownloaderState> = _downloaderState.asStateFlow()
    
    // Filtered playlist based on search and tab
    val filteredPlaylist: StateFlow<List<AudioFile>> = combine(
        _uiState.map { it.playlist },
        _uiState.map { it.searchQuery },
        _uiState.map { it.selectedTab },
        _uiState.map { it.favoriteIds }
    ) { playlist, query, tab, favoriteIds ->
        var filtered = playlist
        
        // Filter by tab
        filtered = when (tab) {
            LibraryTab.ALL -> filtered
            LibraryTab.FAVORITES -> filtered.filter { it.id in favoriteIds }
        }
        
        // Filter by search query
        if (query.isNotBlank()) {
            filtered = filtered.filter { song ->
                song.title.contains(query, ignoreCase = true) ||
                song.artist.contains(query, ignoreCase = true)
            }
        }
        
        filtered
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    init {
        // Bind to Service
        val sessionToken = SessionToken(application, ComponentName(application, PlaybackService::class.java))
        val controllerFuture = MediaController.Builder(application, sessionToken).buildAsync()
        
        controllerFuture.addListener({
            try {
                val p = controllerFuture.get()
                player = p
                
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
        
        // Position update timer
        viewModelScope.launch {
            while (true) {
                if (player?.isPlaying == true) {
                    _uiState.update { it.copy(currentPosition = player?.currentPosition ?: 0L) }
                }
                kotlinx.coroutines.delay(50L)
            }
        }
        
        // Observe favorites
        viewModelScope.launch {
            favoritesRepository.getAllFavoriteIds().collect { ids ->
                _uiState.update { it.copy(favoriteIds = ids) }
            }
        }
        
        // Observe download state
        viewModelScope.launch {
            downloadRepository.downloadState.collect { state ->
                _downloaderState.update { it.copy(downloadState = state) }
                // Refresh library on successful download
                if (state is DownloadState.Success) {
                    kotlinx.coroutines.delay(1000)
                    loadAudioFiles()
                }
            }
        }
    }
    
    // Search
    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }
    
    fun toggleSearch() {
        _uiState.update { 
            it.copy(
                isSearchActive = !it.isSearchActive,
                searchQuery = if (it.isSearchActive) "" else it.searchQuery
            ) 
        }
    }
    
    // Tabs
    fun selectTab(tab: LibraryTab) {
        _uiState.update { it.copy(selectedTab = tab) }
    }
    
    // Favorites
    fun toggleFavorite(songId: Long) {
        favoritesRepository.toggleFavorite(songId)
    }
    
    fun isFavorite(songId: Long): Boolean {
        return favoritesRepository.isFavorite(songId)
    }
    
    // YouTube Search
    fun searchYouTube(query: String) {
        _downloaderState.update { it.copy(searchQuery = query, isSearching = true) }
        viewModelScope.launch {
            val results = downloadRepository.searchYouTube(query)
            _downloaderState.update { it.copy(searchResults = results, isSearching = false) }
        }
    }
    
    fun clearYouTubeSearch() {
        _downloaderState.update { it.copy(searchQuery = "", searchResults = emptyList()) }
    }
    
    // Download
    fun downloadSong(result: YouTubeSearchResult) {
        viewModelScope.launch {
            downloadRepository.downloadAudio(result.videoId, result.title)
        }
    }
    
    fun resetDownloadState() {
        downloadRepository.resetDownloadState()
    }
    
    // Existing methods
    private fun updateCurrentSong() {
        val p = player ?: return
        val currentMediaItemIndex = p.currentMediaItemIndex
        val playlist = _uiState.value.playlist
        
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

    fun loadAudioFiles() {
        viewModelScope.launch {
            val files = audioRepository.getAudioFiles()
            if (files.isNotEmpty()) {
                _uiState.update { it.copy(playlist = files) }
                setupPlayer(files)
            }
        }
    }

    private fun setupPlayer(files: List<AudioFile>) {
        val p = player ?: return
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
        if (p.isPlaying) p.pause() else p.play()
    }

    fun next() {
        val p = player ?: return
        if (p.hasNextMediaItem()) p.seekToNextMediaItem()
    }

    fun previous() {
        val p = player ?: return
        if (p.hasPreviousMediaItem()) p.seekToPreviousMediaItem()
        else p.seekTo(0)
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
