package com.example.musicplayer.ui
import androidx.compose.ui.draw.shadow

import android.Manifest
import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.MusicNote
import com.google.accompanist.permissions.*

import androidx.compose.ui.graphics.graphicsLayer
import androidx.camera.core.ImageAnalysis
import androidx.compose.ui.platform.LocalContext
import com.example.musicplayer.ui.theme.*

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun MusicPlayerScreen(
    viewModel: MusicViewModel = viewModel(),
    onBack: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current


    val permissionState = rememberPermissionState(
        permission = Manifest.permission.CAMERA
    )
    
     // Audio permission handling logic
     val audioPermissionState = rememberPermissionState(
        permission = if (Build.VERSION.SDK_INT >= 33) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
    )

    LaunchedEffect(Unit) {
        audioPermissionState.launchPermissionRequest()
        permissionState.launchPermissionRequest()
    }

    // Feedback State
    var gestureFeedback by remember { mutableStateOf<String?>(null) }
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    
    // Auto-hide feedback
    LaunchedEffect(gestureFeedback) {
        if (gestureFeedback != null) {
            kotlinx.coroutines.delay(1000)
            gestureFeedback = null
        }
    }

    // Camera Logic
    DisposableEffect(lifecycleOwner) {
        val cameraProviderFuture = androidx.camera.lifecycle.ProcessCameraProvider.getInstance(context)
        
        val analyzer = GestureAnalyzer(context) { gesture ->
            // Trigger Feedback
            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
            
            // Map internal gesture to user-friendly label/icon
            val feedbackText = when(gesture) {
                "PAUSE_PLAY" -> "Play/Pause"
                "NEXT" -> "Next"
                "PREV" -> "Previous"
                "VOL_UP" -> "Vol +"
                "VOL_DOWN" -> "Vol -"
                else -> null
            }
            if (feedbackText != null) {
                gestureFeedback = feedbackText
            }

            when(gesture) {
                "PAUSE_PLAY" -> viewModel.playPause()
                "NEXT" -> viewModel.next()
                "PREV" -> viewModel.previous()
                "VOL_UP" -> viewModel.volumeUp()
                "VOL_DOWN" -> viewModel.volumeDown()
            }
        }

        val executor = androidx.core.content.ContextCompat.getMainExecutor(context)

        try {
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                val analysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                
                analysis.setAnalyzer(java.util.concurrent.Executors.newSingleThreadExecutor(), analyzer)

                val cameraSelector = androidx.camera.core.CameraSelector.DEFAULT_FRONT_CAMERA

                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        analysis
                    )
                } catch(e: Exception) {
                    e.printStackTrace()
                }

            }, executor)
        } catch(e: Exception) {
            e.printStackTrace()
        }

        onDispose {
            analyzer.close()
        }
    }

    if (audioPermissionState.status.isGranted) {
        Box(modifier = Modifier.fillMaxSize()) {
            MusicPlayerContent(state, viewModel, onBack)
            
            // Gesture Feedback Overlay
            AnimatedVisibility(
                visible = gestureFeedback != null,
                enter = fadeIn() + scaleIn(),
                exit = fadeOut() + scaleOut(),
                modifier = Modifier.align(Alignment.Center)
            ) {
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color.Black.copy(alpha = 0.85f))
                        .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(24.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = gestureFeedback ?: "",
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Camera permission hint
            AnimatedVisibility(
                visible = !permissionState.status.isGranted,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = 60.dp)
            ) {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.8f)),
                    modifier = Modifier.clickable { permissionState.launchPermissionRequest() }
                ) {
                    Text(
                        text = "✋ Tap to enable Air Gestures",
                        color = Color.White,
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                    )
                }
            }
        }
    } else {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(DarkBackground),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = AccentPrimary
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "Grant permissions to continue",
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}

@Composable
fun MusicPlayerContent(state: MusicState, viewModel: MusicViewModel, onBack: () -> Unit) {
    val defaultAccent = AccentPrimary
    val vibrant = state.paletteColors["vibrant"] ?: defaultAccent
    val darkVibrant = state.paletteColors["darkVibrant"] ?: GradientStart
    val lightVibrant = state.paletteColors["lightVibrant"] ?: Color.White

    // Vinyl rotation animation
    val infiniteTransition = rememberInfiniteTransition(label = "vinyl")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "discRotation"
    )
    val currentRotation = if (state.isPlaying) rotation else 0f

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = { viewModel.playPause() }
                )
            }
            .pointerInput(Unit) {
                var dragAmount = 0f
                detectHorizontalDragGestures(
                    onDragEnd = {
                        if (dragAmount < -80) {
                            viewModel.next()
                        } else if (dragAmount > 80) {
                            viewModel.previous()
                        }
                        dragAmount = 0f
                    },
                    onHorizontalDrag = { change, dragAmountDelta ->
                        change.consume()
                        dragAmount += dragAmountDelta
                    }
                )
            }
    ) {
        // Blurred Background with album art
        state.currentSong?.albumArtUri?.let { uri ->
            AsyncImage(
                model = uri,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(80.dp)
                    .scale(1.2f)
                    .graphicsLayer { alpha = 0.4f }
            )
        }

        // Gradient overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            darkVibrant.copy(alpha = 0.6f),
                            DarkBackground.copy(alpha = 0.95f),
                            DarkBackground
                        ),
                        startY = 0f
                    )
                )
        )
        
        // Back Button
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(8.dp)
                .statusBarsPadding()
        ) {
             Icon(
                 imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                 contentDescription = "Back",
                 tint = Color.White,
                 modifier = Modifier.size(28.dp)
             )
        }

        // Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 32.dp)
                .padding(top = 60.dp, bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.weight(0.5f))
            
            // Vinyl Disc with Album Art
            Box(
                modifier = Modifier.size(300.dp),
                contentAlignment = Alignment.Center
            ) {
                // Vinyl disc background (black)
                Box(
                    modifier = Modifier
                        .size(300.dp)
                        .rotate(currentRotation)
                        .clip(CircleShape)
                        .background(Color(0xFF1A1A1A))
                        .border(2.dp, Color(0xFF333333), CircleShape)
                ) {
                    // Grooves effect
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(40.dp)
                            .border(1.dp, Color(0xFF2A2A2A), CircleShape)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(60.dp)
                            .border(1.dp, Color(0xFF252525), CircleShape)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(80.dp)
                            .border(1.dp, Color(0xFF2A2A2A), CircleShape)
                    )
                }
                
                // Album art center (label)
                Card(
                    shape = CircleShape,
                    modifier = Modifier
                        .size(140.dp)
                        .rotate(currentRotation),
                    elevation = CardDefaults.cardElevation(8.dp)
                ) {
                    if (state.currentSong?.albumArtUri != null) {
                        AsyncImage(
                            model = state.currentSong.albumArtUri,
                            contentDescription = "Album Art",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.radialGradient(
                                        colors = listOf(vibrant, darkVibrant)
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.MusicNote,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(48.dp)
                            )
                        }
                    }
                }
                
                // Center hole
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(DarkBackground)
                        .border(2.dp, Color(0xFF333333), CircleShape)
                )
            }

            Spacer(modifier = Modifier.height(48.dp))

            // Song Info
            Text(
                text = state.currentSong?.title ?: "No Song Playing",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = Color.White,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = state.currentSong?.artist ?: "Unknown Artist",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.6f),
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(40.dp))
            
            // Progress Bar
            ProgressSection(state, viewModel, vibrant, darkVibrant, lightVibrant)

            Spacer(modifier = Modifier.height(32.dp))

            // Controls
            ControlsSection(state, viewModel, vibrant)
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Gesture hint
            Text(
                text = "Swipe to change • Double tap to play/pause",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.3f),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun ProgressSection(
    state: MusicState,
    viewModel: MusicViewModel,
    vibrant: Color,
    darkVibrant: Color,
    lightVibrant: Color
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        var isDragging by remember { mutableStateOf(false) }
        var dragPosition by remember { mutableFloatStateOf(0f) }
        
        val currentProgress = if (isDragging) dragPosition else state.currentPosition.toFloat()
        val totalDuration = state.duration.coerceAtLeast(1L).toFloat()
        val progressFraction = (currentProgress / totalDuration).coerceIn(0f, 1f)
        
        val animatedProgress by animateFloatAsState(
            targetValue = progressFraction,
            animationSpec = tween(durationMillis = if (isDragging) 0 else 100),
            label = "progress"
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragStart = { offset ->
                            isDragging = true
                            dragPosition = (offset.x / size.width) * totalDuration
                        },
                        onDragEnd = {
                            viewModel.seekTo(dragPosition.toLong())
                            isDragging = false
                        },
                        onHorizontalDrag = { change, _ ->
                            change.consume()
                            val newProgress = (change.position.x / size.width) * totalDuration
                            dragPosition = newProgress.coerceIn(0f, totalDuration)
                        }
                    )
                }
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        val newProgress = (offset.x / size.width) * totalDuration
                        viewModel.seekTo(newProgress.toLong())
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            // Track Background
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.White.copy(alpha = 0.15f))
            )
            
            // Active Track with Gradient
            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedProgress)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(vibrant, lightVibrant)
                        )
                    )
                    .align(Alignment.CenterStart)
            )
            
            // Thumb
            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedProgress)
                    .align(Alignment.CenterStart)
            ) {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .align(Alignment.CenterEnd)
                        .offset(x = 8.dp)
                        .shadow(8.dp, CircleShape, spotColor = vibrant)
                        .background(Color.White, CircleShape)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = formatTime(state.currentPosition),
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.5f)
            )
            Text(
                text = formatTime(state.duration),
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.5f)
            )
        }
    }
}

@Composable
fun ControlsSection(state: MusicState, viewModel: MusicViewModel, accentColor: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Shuffle
        IconButton(
            onClick = { viewModel.toggleShuffle() },
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Shuffle,
                contentDescription = "Shuffle",
                tint = if (state.isShuffleEnabled) accentColor else Color.White.copy(alpha = 0.4f),
                modifier = Modifier.size(24.dp)
            )
        }
        
        // Previous
        IconButton(
            onClick = { viewModel.previous() },
            modifier = Modifier.size(56.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.SkipPrevious,
                contentDescription = "Previous",
                tint = Color.White,
                modifier = Modifier.size(36.dp)
            )
        }

        // Play/Pause (Large FAB)
        Box(
            modifier = Modifier
                .size(80.dp)
                .shadow(16.dp, CircleShape, spotColor = accentColor)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        colors = listOf(accentColor, accentColor.copy(alpha = 0.8f))
                    )
                )
                .clickable { viewModel.playPause() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (state.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                contentDescription = "Play/Pause",
                tint = Color.White,
                modifier = Modifier.size(40.dp)
            )
        }

        // Next
        IconButton(
            onClick = { viewModel.next() },
            modifier = Modifier.size(56.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.SkipNext,
                contentDescription = "Next",
                tint = Color.White,
                modifier = Modifier.size(36.dp)
            )
        }
        
        // Repeat
        IconButton(
            onClick = { viewModel.toggleRepeat() },
            modifier = Modifier.size(48.dp)
        ) {
            val icon = when(state.repeatMode) {
                androidx.media3.common.Player.REPEAT_MODE_ONE -> Icons.Filled.RepeatOne
                else -> Icons.Filled.Repeat
            }
            val tint = if (state.repeatMode != androidx.media3.common.Player.REPEAT_MODE_OFF) 
                accentColor else Color.White.copy(alpha = 0.4f)
            
            Icon(
                imageVector = icon,
                contentDescription = "Repeat",
                tint = tint,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

fun formatTime(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}

// Extension to set alpha on modifier
fun Modifier.alpha(alpha: Float) = this.then(Modifier.graphicsLayer(alpha = alpha))
