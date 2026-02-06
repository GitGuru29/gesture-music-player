package com.example.musicplayer.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
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
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                AccentSecondary.copy(alpha = 0.3f),
                                AccentSecondary.copy(alpha = 0.1f),
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
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(DarkSurfaceVariant)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Back",
                                tint = TextPrimary
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = "Download Music",
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = "Search and download from YouTube",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Search bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(DarkSurfaceVariant)
                                .padding(horizontal = 16.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = null,
                                    tint = TextSecondary,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                BasicTextField(
                                    value = searchText,
                                    onValueChange = { searchText = it },
                                    modifier = Modifier.weight(1f),
                                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                                        color = MaterialTheme.colorScheme.onBackground
                                    ),
                                    cursorBrush = SolidColor(AccentSecondary),
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                    keyboardActions = KeyboardActions(
                                        onSearch = {
                                            if (searchText.isNotBlank()) {
                                                viewModel.searchYouTube(searchText)
                                                keyboardController?.hide()
                                            }
                                        }
                                    ),
                                    decorationBox = { innerTextField ->
                                        Box {
                                            if (searchText.isEmpty()) {
                                                Text(
                                                    "Search for songs...",
                                                    style = MaterialTheme.typography.bodyLarge,
                                                    color = TextSecondary
                                                )
                                            }
                                            innerTextField()
                                        }
                                    }
                                )
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
                        }
                        
                        Spacer(modifier = Modifier.width(12.dp))
                        
                        Button(
                            onClick = {
                                if (searchText.isNotBlank()) {
                                    viewModel.searchYouTube(searchText)
                                    keyboardController?.hide()
                                }
                            },
                            enabled = searchText.isNotBlank() && !state.isSearching,
                            modifier = Modifier.height(52.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AccentSecondary
                            )
                        ) {
                            if (state.isSearching) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text("Search", fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
            
            // Results
            if (state.searchResults.isEmpty() && !state.isSearching) {
                // Empty state
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
                            tint = TextSecondary.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Search for music",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Download songs from YouTube",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary.copy(alpha = 0.6f)
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
        
        // Download status snackbar
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
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Thumbnail
        Card(
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .width(100.dp)
                .height(56.dp),
            elevation = CardDefaults.cardElevation(4.dp)
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
                color = TextPrimary
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
                    color = TextSecondary.copy(alpha = 0.7f)
                )
            }
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        // Download button
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(
                    if (isDownloading) AccentSecondary.copy(alpha = 0.2f) 
                    else AccentSecondary
                )
                .clickable(enabled = !isDownloading, onClick = onDownload),
            contentAlignment = Alignment.Center
        ) {
            if (isDownloading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
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

@Composable
fun DownloadStatusCard(
    downloadState: DownloadState,
    onDismiss: () -> Unit
) {
    val (icon, text, containerColor) = when (downloadState) {
        is DownloadState.Downloading -> Triple(
            Icons.Default.CloudDownload,
            "Downloading: ${downloadState.title}",
            DarkSurfaceVariant
        )
        is DownloadState.Success -> Triple(
            Icons.Default.CheckCircle,
            "Downloaded: ${downloadState.title}",
            Color(0xFF2E7D32)
        )
        is DownloadState.Error -> Triple(
            Icons.Default.Error,
            "Error: ${downloadState.message}",
            Color(0xFFC62828)
        )
        else -> Triple(Icons.Default.Info, "", DarkSurfaceVariant)
    }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor)
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
                        tint = Color.White.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}
