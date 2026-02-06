package com.example.musicplayer.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.musicplayer.data.AudioFile
import com.example.musicplayer.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    viewModel: MusicViewModel,
    onNavigateToPlayer: () -> Unit,
    onNavigateToDownloader: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsState()
    val filteredPlaylist by viewModel.filteredPlaylist.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = if (state.currentSong != null) 100.dp else 16.dp)
        ) {
            // Header with search
            item {
                LibraryHeader(
                    songCount = state.playlist.size,
                    isSearchActive = state.isSearchActive,
                    searchQuery = state.searchQuery,
                    onSearchClick = { viewModel.toggleSearch() },
                    onSearchQueryChange = { viewModel.setSearchQuery(it) },
                    onDownloadClick = onNavigateToDownloader
                )
            }
            
            // Tabs
            item {
                TabRow(
                    selectedTab = state.selectedTab,
                    onTabSelected = { viewModel.selectTab(it) },
                    favoritesCount = state.favoriteIds.size
                )
            }

            // Song list
            itemsIndexed(
                items = filteredPlaylist,
                key = { _, song -> song.id }
            ) { index, song ->
                SongListItem(
                    song = song,
                    isPlaying = song.id == state.currentSong?.id,
                    isFavorite = song.id in state.favoriteIds,
                    index = index,
                    onFavoriteClick = { viewModel.toggleFavorite(song.id) },
                    onClick = {
                        viewModel.playSong(song)
                        onNavigateToPlayer()
                    }
                )
            }

            // Empty state
            if (filteredPlaylist.isEmpty()) {
                item {
                    EmptyState(selectedTab = state.selectedTab, searchQuery = state.searchQuery)
                }
            }
        }

        // Mini Player
        AnimatedVisibility(
            visible = state.currentSong != null,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            state.currentSong?.let { song ->
                MiniPlayer(
                    song = song,
                    isPlaying = state.isPlaying,
                    progress = state.currentPosition.toFloat() / state.duration.coerceAtLeast(1L).toFloat(),
                    accentColor = state.paletteColors["vibrant"] ?: AccentPrimary,
                    onPlayPause = { viewModel.playPause() },
                    onClick = onNavigateToPlayer
                )
            }
        }
    }
}

@Composable
fun LibraryHeader(
    songCount: Int,
    isSearchActive: Boolean,
    searchQuery: String,
    onSearchClick: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onDownloadClick: () -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    
    LaunchedEffect(isSearchActive) {
        if (isSearchActive) {
            focusRequester.requestFocus()
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        AccentPrimary.copy(alpha = 0.3f),
                        AccentPrimary.copy(alpha = 0.1f),
                        Color.Transparent
                    )
                )
            )
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!isSearchActive) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "My Music",
                            style = MaterialTheme.typography.headlineLarge,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "$songCount songs",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                        )
                    }
                } else {
                    // Search bar
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(DarkSurfaceVariant)
                            .padding(horizontal = 16.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = TextSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            BasicTextField(
                                value = searchQuery,
                                onValueChange = onSearchQueryChange,
                                modifier = Modifier
                                    .weight(1f)
                                    .focusRequester(focusRequester),
                                textStyle = MaterialTheme.typography.bodyLarge.copy(
                                    color = MaterialTheme.colorScheme.onBackground
                                ),
                                cursorBrush = SolidColor(AccentPrimary),
                                singleLine = true,
                                decorationBox = { innerTextField ->
                                    Box {
                                        if (searchQuery.isEmpty()) {
                                            Text(
                                                "Search songs...",
                                                style = MaterialTheme.typography.bodyLarge,
                                                color = TextSecondary
                                            )
                                        }
                                        innerTextField()
                                    }
                                }
                            )
                            if (searchQuery.isNotEmpty()) {
                                IconButton(
                                    onClick = { onSearchQueryChange("") },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Clear",
                                        tint = TextSecondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.width(8.dp))
                
                // Search toggle button
                IconButton(
                    onClick = onSearchClick,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(if (isSearchActive) AccentPrimary else DarkSurfaceVariant)
                ) {
                    Icon(
                        imageVector = if (isSearchActive) Icons.Default.Close else Icons.Default.Search,
                        contentDescription = "Search",
                        tint = if (isSearchActive) Color.White else TextSecondary
                    )
                }
                
                Spacer(modifier = Modifier.width(8.dp))
                
                // Download button
                IconButton(
                    onClick = onDownloadClick,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(DarkSurfaceVariant)
                ) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = "Download",
                        tint = TextSecondary
                    )
                }
            }
        }
    }
}

@Composable
fun TabRow(
    selectedTab: LibraryTab,
    onTabSelected: (LibraryTab) -> Unit,
    favoritesCount: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        TabChip(
            text = "All",
            selected = selectedTab == LibraryTab.ALL,
            onClick = { onTabSelected(LibraryTab.ALL) }
        )
        TabChip(
            text = "Favorites",
            badge = if (favoritesCount > 0) favoritesCount.toString() else null,
            selected = selectedTab == LibraryTab.FAVORITES,
            onClick = { onTabSelected(LibraryTab.FAVORITES) }
        )
    }
}

@Composable
fun TabChip(
    text: String,
    badge: String? = null,
    selected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = if (selected) AccentPrimary else DarkSurfaceVariant
    val textColor = if (selected) Color.White else TextSecondary
    
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                color = textColor
            )
            if (badge != null) {
                Spacer(modifier = Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(if (selected) Color.White.copy(alpha = 0.3f) else AccentPrimary.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = badge,
                        style = MaterialTheme.typography.labelSmall,
                        color = textColor
                    )
                }
            }
        }
    }
}

@Composable
fun SongListItem(
    song: AudioFile,
    isPlaying: Boolean,
    isFavorite: Boolean,
    index: Int,
    onFavoriteClick: () -> Unit,
    onClick: () -> Unit
) {
    val animatedAlpha by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(
            durationMillis = 300,
            delayMillis = index * 30,
            easing = FastOutSlowInEasing
        ),
        label = "itemAlpha"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { alpha = animatedAlpha }
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Album art
        Box {
            Card(
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.size(56.dp),
                elevation = CardDefaults.cardElevation(if (isPlaying) 8.dp else 2.dp)
            ) {
                Box {
                    if (song.albumArtUri != null) {
                        AsyncImage(
                            model = song.albumArtUri,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(DarkSurfaceVariant, DarkCard)
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.MusicNote,
                                contentDescription = null,
                                tint = TextSecondary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    if (isPlaying) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.4f)),
                            contentAlignment = Alignment.Center
                        ) {
                            PlayingIndicator()
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = if (isPlaying) FontWeight.SemiBold else FontWeight.Normal
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = if (isPlaying) AccentPrimary else MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = song.artist,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
            )
        }
        
        // Favorite button
        IconButton(
            onClick = onFavoriteClick,
            modifier = Modifier.size(40.dp)
        ) {
            Icon(
                imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                contentDescription = if (isFavorite) "Remove from favorites" else "Add to favorites",
                tint = if (isFavorite) AccentPrimary else TextSecondary,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
fun EmptyState(selectedTab: LibraryTab, searchQuery: String) {
    val (icon, title, subtitle) = when {
        searchQuery.isNotEmpty() -> Triple(
            Icons.Default.SearchOff,
            "No results found",
            "Try a different search term"
        )
        selectedTab == LibraryTab.FAVORITES -> Triple(
            Icons.Outlined.FavoriteBorder,
            "No favorites yet",
            "Tap the heart icon to add songs"
        )
        else -> Triple(
            Icons.Default.MusicNote,
            "No songs found",
            "Add music to your device"
        )
    }
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f)
            )
        }
    }
}

@Composable
fun PlayingIndicator() {
    val infiniteTransition = rememberInfiniteTransition(label = "playingBars")
    
    Row(
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.Bottom,
        modifier = Modifier.height(16.dp)
    ) {
        repeat(3) { index ->
            val animatedHeight by infiniteTransition.animateFloat(
                initialValue = 0.3f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(
                        durationMillis = 400 + (index * 100),
                        easing = FastOutSlowInEasing
                    ),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "bar$index"
            )
            
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .fillMaxHeight(animatedHeight)
                    .background(AccentPrimary, RoundedCornerShape(1.dp))
            )
        }
    }
}

@Composable
fun MiniPlayer(
    song: AudioFile,
    isPlaying: Boolean,
    progress: Float,
    accentColor: Color,
    onPlayPause: () -> Unit,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .navigationBarsPadding()
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp)
                .clickable(onClick = onClick),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = DarkSurfaceVariant.copy(alpha = 0.95f)
            ),
            elevation = CardDefaults.cardElevation(12.dp)
        ) {
            Box {
                if (song.albumArtUri != null) {
                    AsyncImage(
                        model = song.albumArtUri,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .blur(30.dp)
                            .graphicsLayer { alpha = 0.3f }
                    )
                }
                
                // Progress bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .align(Alignment.TopCenter)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.White.copy(alpha = 0.1f))
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progress.coerceIn(0f, 1f))
                            .fillMaxHeight()
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(accentColor, accentColor.copy(alpha = 0.7f))
                                )
                            )
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.size(52.dp),
                        elevation = CardDefaults.cardElevation(4.dp)
                    ) {
                        if (song.albumArtUri != null) {
                            AsyncImage(
                                model = song.albumArtUri,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(DarkCard),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MusicNote,
                                    contentDescription = null,
                                    tint = TextSecondary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = song.title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = song.artist,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(accentColor)
                            .clickable(onClick = onPlayPause),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }
        }
    }
}
