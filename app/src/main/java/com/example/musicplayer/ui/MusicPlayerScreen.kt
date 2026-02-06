package com.example.musicplayer.ui
import androidx.compose.ui.draw.shadow

import android.Manifest
import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
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
import com.google.accompanist.permissions.*

import androidx.compose.ui.graphics.graphicsLayer
import androidx.camera.core.ImageAnalysis
import androidx.compose.ui.platform.LocalContext

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
    
     // Audio permission handling logic (simplified for brevity, keeping existing flow is better but merging for clarity)
     // We need both Audio and Camera.
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
                     // Bind Analysis only. We don't need Preview for analysis to work on most devices.
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
            androidx.compose.animation.AnimatedVisibility(
                visible = gestureFeedback != null,
                enter = androidx.compose.animation.fadeIn() + androidx.compose.animation.scaleIn(),
                exit = androidx.compose.animation.fadeOut() + androidx.compose.animation.scaleOut(),
                modifier = Modifier.align(Alignment.Center)
            ) {
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = gestureFeedback ?: "",
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (!permissionState.status.isGranted) {
                Text(
                    text = "⚠ Camera needed for Air Gestures",
                    color = Color.Red,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 40.dp)
                        .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(4.dp))
                        .padding(8.dp)
                        .clickable { permissionState.launchPermissionRequest() }
                )
            }
        }
    } else {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
             Text("Please grant permissions")
        }
    }
}

@Composable
fun MusicPlayerContent(state: MusicState, viewModel: MusicViewModel, onBack: () -> Unit) {
    val defaultColor = MaterialTheme.colorScheme.primaryContainer
    val vibrant = state.paletteColors["vibrant"] ?: defaultColor
    val darkVibrant = state.paletteColors["darkVibrant"] ?: Color.Black
    val lightVibrant = state.paletteColors["lightVibrant"] ?: Color.White

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = { viewModel.playPause() }
                )
            }
            .pointerInput(Unit) {
                var dragAmount = 0f
                detectHorizontalDragGestures(
                    onDragEnd = {
                        if (dragAmount < -50) { // Swipe Left -> Next
                            viewModel.next()
                        } else if (dragAmount > 50) { // Swipe Right -> Prev
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
        // Blurred Background
        state.currentSong?.albumArtUri?.let { uri ->
            AsyncImage(
                model = uri,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(60.dp)
                    .alpha(0.5f)
            )
        }

        // Overlay Gradient with Dynamic Color
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            darkVibrant.copy(alpha = 0.3f),
                            Color.Black.copy(alpha = 0.9f)
                        )
                    )
                )
        )
        
        // Back Button
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
                .statusBarsPadding()
        ) {
             Icon(
                 imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                 contentDescription = "Back",
                 tint = Color.White
             )
        }

        // Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Album Art Card
            Card(
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(16.dp),
                modifier = Modifier.size(320.dp)
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
                            .background(Color.DarkGray),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("♪", fontSize = 64.sp, color = Color.White)
                    }
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            // Text Info
            Text(
                text = state.currentSong?.title ?: "No Song Playing",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = lightVibrant,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = state.currentSong?.artist ?: "Unknown Artist",
                style = MaterialTheme.typography.titleMedium,
                color = lightVibrant.copy(alpha = 0.7f),
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Progress Bar
            Column(modifier = Modifier.fillMaxWidth()) {
                // Modern Custom Slider
                var isDragging by remember { mutableStateOf(false) }
                var dragPosition by remember { mutableFloatStateOf(0f) }
                
                val currentProgress = if (isDragging) dragPosition else state.currentPosition.toFloat()
                val totalDuration = state.duration.coerceAtLeast(1L).toFloat()
                val progressFraction = (currentProgress / totalDuration).coerceIn(0f, 1f)
                
                // Animated Progress Value for smooth visual connection
                val animatedProgress by androidx.compose.animation.core.animateFloatAsState(
                    targetValue = progressFraction,
                    animationSpec = androidx.compose.animation.core.tween(durationMillis = if (isDragging) 0 else 100)
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp) // Touch target height
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
                    contentAlignment = Alignment.CenterStart
                ) {
                    // Track Background
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(Color.White.copy(alpha = 0.2f))
                            .align(Alignment.Center)
                    )
                    
                    // Active Gradient Track
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(animatedProgress)
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        darkVibrant,
                                        vibrant,
                                        lightVibrant
                                    )
                                )
                            )
                            .align(Alignment.CenterStart)
                    )
                    
                    // Glowing Thumb Container
                     Box(
                        modifier = Modifier
                            .fillMaxWidth(animatedProgress)
                            .fillMaxHeight()
                            .align(Alignment.CenterStart)
                    ) {
                        // Thumb
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .align(Alignment.CenterEnd)
                                .offset(x = 10.dp) // Center on the end edge
                                .shadow(8.dp, androidx.compose.foundation.shape.CircleShape, spotColor = vibrant)
                                .background(Color.White, androidx.compose.foundation.shape.CircleShape)
                        )
                    }
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = formatTime(state.currentPosition),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                    Text(
                        text = formatTime(state.duration),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Shuffle
                 IconButton(onClick = { viewModel.toggleShuffle() }) {
                    Icon(
                        imageVector = Icons.Filled.Shuffle,
                        contentDescription = "Shuffle",
                        tint = if (state.isShuffleEnabled) vibrant else Color.White.copy(alpha = 0.5f),
                        modifier = Modifier.size(28.dp)
                    )
                }
                
                // Prev
                IconButton(onClick = { viewModel.previous() }) {
                    Icon(
                        imageVector = Icons.Filled.SkipPrevious,
                        contentDescription = "Previous",
                        tint = Color.White,
                        modifier = Modifier.size(48.dp)
                    )
                }

                // Play/Pause (FAB style)
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(androidx.compose.foundation.shape.CircleShape)
                        .background(vibrant)
                        .clickable { viewModel.playPause() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (state.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = "Play/Pause",
                        tint = Color.Black, // Contrast against vibrant
                        modifier = Modifier.size(40.dp)
                    )
                }

                // Next
                IconButton(onClick = { viewModel.next() }) {
                    Icon(
                        imageVector = Icons.Filled.SkipNext,
                        contentDescription = "Next",
                        tint = Color.White,
                        modifier = Modifier.size(48.dp)
                    )
                }
                
                // Repeat
                 IconButton(onClick = { viewModel.toggleRepeat() }) {
                     val icon = when(state.repeatMode) {
                         androidx.media3.common.Player.REPEAT_MODE_ONE -> Icons.Filled.RepeatOne
                         else -> Icons.Filled.Repeat
                     }
                     val tint = if (state.repeatMode != androidx.media3.common.Player.REPEAT_MODE_OFF) vibrant else Color.White.copy(alpha = 0.5f)
                     
                    Icon(
                        imageVector = icon,
                        contentDescription = "Repeat",
                        tint = tint,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))

             // Controls Hint - kept minimal
             Text(
                text = "✋ Pause | 👍 Next | 👎 Prev | 👌 Volume",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.5f)
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
