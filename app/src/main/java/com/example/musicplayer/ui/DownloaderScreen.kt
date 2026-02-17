package com.example.musicplayer.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.musicplayer.data.DownloadState
import com.example.musicplayer.data.YouTubeSearchResult
import com.example.musicplayer.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloaderScreen(
    viewModel: MusicViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.downloaderState.collectAsState()
    val keyboardController = LocalSoftwareKeyboardController.current
    var searchText by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0D0B1A),
                        DarkBackground,
                        Color(0xFF08081A),
                        DarkBackground
                    )
                )
            )
    ) {
        // Ambient teal glow orb
        Box(
            modifier = Modifier
                .size(280.dp)
                .align(Alignment.TopEnd)
                .offset(x = 80.dp, y = (-30).dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            AccentSecondary.copy(alpha = 0.08f),
                            Color.Transparent
                        )
                    )
                )
        )

        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Glass header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                AccentSecondary.copy(alpha = 0.1f),
                                AccentSecondary.copy(alpha = 0.03f),
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
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        GlassIconButton(
                            onClick = onBack,
                            size = 40.dp
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = "Download Music",
                                style = MaterialTheme.typography.headlineMedium,
                                color = Color.White
                            )
                            Text(
                                text = "Search and download songs",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Glass search bar + button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        GlassSearchBar(
                            value = searchText,
                            onValueChange = { searchText = it },
                            modifier = Modifier.weight(1f),
                            placeholder = "Search for songs...",
                            onSearch = {
                                if (searchText.isNotBlank()) {
                                    viewModel.searchYouTube(searchText)
                                    keyboardController?.hide()
                                }
                            },
                            trailingIcon = {
                                if (searchText.isNotEmpty()) {
                                    IconButton(
                                        onClick = {
                                            searchText = ""
                                            viewModel.clearYouTubeSearch()
                                        },
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
                        )
                        
                        Spacer(modifier = Modifier.width(12.dp))
                        
                        // Glass search button
                        GlassSurface(
                            modifier = Modifier
                                .height(52.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .clickable(enabled = searchText.isNotBlank() && !state.isSearching) {
                                    if (searchText.isNotBlank()) {
                                        viewModel.searchYouTube(searchText)
                                        keyboardController?.hide()
                                    }
                                },
                            shape = RoundedCornerShape(16.dp),
                            backgroundColor = if (searchText.isNotBlank()) AccentSecondary.copy(alpha = 0.3f) else GlassSurface,
                            borderColor = if (searchText.isNotBlank()) AccentSecondary.copy(alpha = 0.5f) else GlassBorder,
                            glowColor = if (searchText.isNotBlank()) AccentSecondary.copy(alpha = 0.15f) else null
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .padding(horizontal = 20.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                if (state.isSearching) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        color = AccentSecondary,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Text(
                                        "Search",
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (searchText.isNotBlank()) Color.White else TextSecondary
                                    )
                                }
                            }
                        }
                    }
                }
            }
            
            // Results
            if (state.searchResults.isEmpty() && !state.isSearching) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.CloudDownload,
                            contentDescription = null,
                            modifier = Modifier.size(80.dp),
                            tint = TextSecondary.copy(alpha = 0.25f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Search for music",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Download songs in high quality",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary.copy(alpha = 0.4f)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(state.searchResults) { result ->
                        YouTubeResultItem(
                            result = result,
                            downloadState = state.downloadState,
                            onDownload = { viewModel.downloadSong(result) }
                        )
                    }
                }
            }
        }
        
        // Glass download status card
        AnimatedVisibility(
            visible = state.downloadState !is DownloadState.Idle,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
                .navigationBarsPadding()
        ) {
            DownloadStatusCard(
                downloadState = state.downloadState,
                onDismiss = { viewModel.resetDownloadState() }
            )
        }
    }
}

@Composable
fun YouTubeResultItem(
    result: YouTubeSearchResult,
    downloadState: DownloadState,
    onDownload: () -> Unit
) {
    val isDownloading = downloadState is DownloadState.Downloading && 
                        (downloadState as? DownloadState.Downloading)?.title == result.title
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 5.dp)
    ) {
        GlassSurface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            backgroundColor = if (isDownloading) AccentSecondary.copy(alpha = 0.1f) else GlassSurfaceDim,
            borderColor = if (isDownloading) AccentSecondary.copy(alpha = 0.4f) else GlassBorderDim,
            enableTopHighlight = isDownloading,
            glowColor = if (isDownloading) GlowSecondary else null
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Thumbnail with glass border
                Card(
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .width(100.dp)
                        .height(56.dp)
                        .border(1.5.dp, GlassBorder, RoundedCornerShape(12.dp)),
                    elevation = CardDefaults.cardElevation(6.dp)
                ) {
                    AsyncImage(
                        model = result.thumbnailUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                
                Spacer(modifier = Modifier.width(14.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = result.title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = result.artist,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = TextSecondary,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Text(
                            text = " • ${result.duration}",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary.copy(alpha = 0.5f)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                // Glass download button
                GlassIconButton(
                    onClick = onDownload,
                    isActive = !isDownloading,
                    activeColor = AccentSecondary,
                    size = 44.dp
                ) {
                    if (isDownloading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = AccentSecondary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = "Download",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DownloadStatusCard(
    downloadState: DownloadState,
    onDismiss: () -> Unit
) {
    val (icon, text, glowColor) = when (downloadState) {
        is DownloadState.Downloading -> Triple(
            Icons.Default.CloudDownload,
            "Downloading: ${downloadState.title}",
            AccentSecondary
        )
        is DownloadState.Success -> Triple(
            Icons.Default.CheckCircle,
            "Downloaded: ${downloadState.title}",
            Color(0xFF4CAF50)
        )
        is DownloadState.Error -> Triple(
            Icons.Default.Error,
            "Error: ${downloadState.message}",
            Color(0xFFEF5350)
        )
        else -> Triple(Icons.Default.Info, "", AccentSecondary)
    }
    
    GlassSurface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        backgroundColor = glowColor.copy(alpha = 0.15f),
        borderColor = glowColor.copy(alpha = 0.4f),
        glowColor = glowColor.copy(alpha = 0.2f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (downloadState is DownloadState.Downloading) {
                CircularProgressIndicator(
                    progress = { downloadState.progress / 100f },
                    modifier = Modifier.size(24.dp),
                    color = AccentSecondary,
                    strokeWidth = 3.dp
                )
            } else {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            
            if (downloadState !is DownloadState.Downloading) {
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Dismiss",
                        tint = Color.White.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}
