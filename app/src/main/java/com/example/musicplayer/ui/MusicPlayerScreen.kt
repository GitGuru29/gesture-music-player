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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
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

    var gestureFeedback by remember { mutableStateOf<String?>(null) }
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current

    LaunchedEffect(gestureFeedback) {
        if (gestureFeedback != null) {
            kotlinx.coroutines.delay(1000)
            gestureFeedback = null
        }
    }

    DisposableEffect(lifecycleOwner) {
        val cameraProviderFuture = androidx.camera.lifecycle.ProcessCameraProvider.getInstance(context)

        val analyzer = GestureAnalyzer(context) { gesture ->
            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)

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

            // Glass gesture feedback overlay
            AnimatedVisibility(
                visible = gestureFeedback != null,
                enter = fadeIn() + scaleIn(),
                exit = fadeOut() + scaleOut(),
                modifier = Modifier.align(Alignment.Center)
            ) {
                GlassSurface(
                    modifier = Modifier.size(130.dp),
                    shape = RoundedCornerShape(28.dp),
                    backgroundColor = GlassSurfaceStrong,
                    borderColor = GlassBorderBright,
                    glowColor = AccentPrimary.copy(alpha = 0.3f)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
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
            }

            // Glass camera permission hint
            AnimatedVisibility(
                visible = !permissionState.status.isGranted,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = 60.dp)
            ) {
                GlassSurface(
                    modifier = Modifier.clickable { permissionState.launchPermissionRequest() },
                    shape = RoundedCornerShape(14.dp),
                    backgroundColor = GlassSurfaceStrong,
                    borderColor = GlassBorderBright
                ) {
                    Text(
                        text = "✋ Tap to enable Air Gestures",
                        color = Color.White,
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp)
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
        // Blurred album art — more intense for glass depth
        state.currentSong?.albumArtUri?.let { uri ->
            AsyncImage(
                model = uri,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(100.dp)
                    .scale(1.4f)
                    .graphicsLayer { alpha = 0.5f }
            )
        }

        // Gradient overlay with glass depth
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            darkVibrant.copy(alpha = 0.3f),
                            DarkBackground.copy(alpha = 0.8f),
                            DarkBackground.copy(alpha = 0.95f)
                        ),
                        startY = 0f
                    )
                )
        )

        // Ambient colored glow orb behind disc
        Box(
            modifier = Modifier
                .size(350.dp)
                .align(Alignment.Center)
                .offset(y = (-80).dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            vibrant.copy(alpha = 0.12f),
                            Color.Transparent
                        )
                    )
                )
        )
        
        // Glass back button
        GlassIconButton(
            onClick = onBack,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(12.dp)
                .statusBarsPadding()
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = Color.White,
                modifier = Modifier.size(24.dp)
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
            
            // Vinyl Disc with glass glow ring
            Box(
                modifier = Modifier.size(300.dp),
                contentAlignment = Alignment.Center
            ) {
                // Pulsing glow ring
                GlassGlowRing(
                    modifier = Modifier.size(306.dp),
                    color = vibrant
                )
                
                // Vinyl disc body
                Box(
                    modifier = Modifier
                        .size(300.dp)
                        .rotate(currentRotation)
                        .clip(CircleShape)
                        .background(Color(0xFF0A0A0A))
                        .border(1.5.dp, GlassBorder, CircleShape)
                ) {
                    // Vinyl grooves
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(40.dp)
                            .border(1.dp, Color.White.copy(alpha = 0.06f), CircleShape)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(60.dp)
                            .border(1.dp, Color.White.copy(alpha = 0.04f), CircleShape)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(80.dp)
                            .border(1.dp, Color.White.copy(alpha = 0.06f), CircleShape)
                    )
                }
                
                // Album art center label with glass border
                Card(
                    shape = CircleShape,
                    modifier = Modifier
                        .size(140.dp)
                        .rotate(currentRotation)
                        .border(2.5.dp, GlassBorderBright, CircleShape),
                    elevation = CardDefaults.cardElevation(12.dp)
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
                        .border(1.5.dp, GlassBorder, CircleShape)
                )
            }

            Spacer(modifier = Modifier.height(40.dp))

            // Song Info in glass panel
            GlassSurface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                shape = RoundedCornerShape(18.dp),
                backgroundColor = GlassSurface,
                borderColor = GlassBorder,
                glowColor = vibrant.copy(alpha = 0.08f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = state.currentSong?.title ?: "No Song Playing",
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color.White,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = state.currentSong?.artist ?: "Unknown Artist",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White.copy(alpha = 0.5f),
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(28.dp))
            
            // Progress Bar
            ProgressSection(state, viewModel, vibrant, darkVibrant, lightVibrant)

            Spacer(modifier = Modifier.height(24.dp))

            // Controls
            ControlsSection(state, viewModel, vibrant)
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Gesture hint
            Text(
                text = "Swipe to change • Double tap to play/pause",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.2f),
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
            // Glass track background
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(GlassSurface)
                    .border(0.5.dp, GlassBorderDim, RoundedCornerShape(3.dp))
            )
            
            // Active track with gradient glow
            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedProgress)
                    .height(5.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(vibrant, lightVibrant)
                        )
                    )
                    .graphicsLayer {
                        shadowElevation = 12f
                        shape = RoundedCornerShape(3.dp)
                        ambientShadowColor = vibrant
                        spotShadowColor = vibrant
                    }
                    .align(Alignment.CenterStart)
            )
            
            // Glowing thumb with glass border
            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedProgress)
                    .align(Alignment.CenterStart)
            ) {
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .align(Alignment.CenterEnd)
                        .offset(x = 9.dp)
                        .graphicsLayer {
                            shadowElevation = 20f
                            shape = CircleShape
                            ambientShadowColor = vibrant
                            spotShadowColor = vibrant
                        }
                        .background(Color.White, CircleShape)
                        .border(2.dp, vibrant.copy(alpha = 0.6f), CircleShape)
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
                color = Color.White.copy(alpha = 0.4f)
            )
            Text(
                text = formatTime(state.duration),
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.4f)
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
        GlassIconButton(
            onClick = { viewModel.toggleShuffle() },
            isActive = state.isShuffleEnabled,
            activeColor = accentColor,
            size = 48.dp
        ) {
            Icon(
                imageVector = Icons.Filled.Shuffle,
                contentDescription = "Shuffle",
                tint = if (state.isShuffleEnabled) accentColor else Color.White.copy(alpha = 0.4f),
                modifier = Modifier.size(22.dp)
            )
        }
        
        // Previous
        GlassIconButton(
            onClick = { viewModel.previous() },
            size = 56.dp
        ) {
            Icon(
                imageVector = Icons.Filled.SkipPrevious,
                contentDescription = "Previous",
                tint = Color.White,
                modifier = Modifier.size(32.dp)
            )
        }

        // Play/Pause — large glass FAB with strong glow
        Box(
            modifier = Modifier
                .size(80.dp)
                .graphicsLayer {
                    shadowElevation = 32f
                    shape = CircleShape
                    clip = false
                    ambientShadowColor = accentColor
                    spotShadowColor = accentColor
                }
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            accentColor,
                            accentColor.copy(alpha = 0.7f)
                        )
                    )
                )
                .border(1.5.dp, Color.White.copy(alpha = 0.3f), CircleShape)
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
        GlassIconButton(
            onClick = { viewModel.next() },
            size = 56.dp
        ) {
            Icon(
                imageVector = Icons.Filled.SkipNext,
                contentDescription = "Next",
                tint = Color.White,
                modifier = Modifier.size(32.dp)
            )
        }
        
        // Repeat
        GlassIconButton(
            onClick = { viewModel.toggleRepeat() },
            isActive = state.repeatMode != androidx.media3.common.Player.REPEAT_MODE_OFF,
            activeColor = accentColor,
            size = 48.dp
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
                modifier = Modifier.size(22.dp)
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

fun Modifier.alpha(alpha: Float) = this.then(Modifier.graphicsLayer(alpha = alpha))
