package com.example.ui.screens.tv

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CastConnected
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PointMode
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.MirrorOrientation
import com.example.data.model.MirrorSource
import com.example.ui.theme.TvAccentAmber
import com.example.ui.theme.TvAccentCyan
import com.example.ui.theme.TvAccentGreen
import com.example.ui.theme.TvBorderDark
import com.example.ui.theme.TvCrimsonPrimary
import com.example.ui.theme.TvObsidianBackground
import com.example.ui.theme.TvSurfaceDark
import com.example.ui.theme.TvSurfaceVariant
import com.example.ui.viewmodel.TvCompanionViewModel

@Composable
fun TvScreenMirrorScreen(
    viewModel: TvCompanionViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val mirrorState by viewModel.screenMirrorState.collectAsStateWithLifecycle()
    val cursorPosition by viewModel.virtualCursorPosition.collectAsStateWithLifecycle()
    val isCursorVisible by viewModel.isCursorVisible.collectAsStateWithLifecycle()

    var isBorderlessFullscreen by remember { mutableStateOf(false) }
    var isDrawingMode by remember { mutableStateOf(false) }

    val infiniteTransition = rememberInfiniteTransition(label = "audio_wave")
    val pulseAnim by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(TvObsidianBackground)
            .testTag("tv_screen_mirror_root")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // TV Header with live telemetry
            AnimatedVisibility(
                visible = !isBorderlessFullscreen,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .border(1.dp, TvBorderDark, RoundedCornerShape(18.dp)),
                    color = TvSurfaceDark
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = onBack,
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(TvSurfaceVariant)
                                    .testTag("btn_tv_mirror_back")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ArrowBack,
                                    contentDescription = "Back",
                                    tint = Color.White
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(TvCrimsonPrimary.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CastConnected,
                                    contentDescription = "Screen Mirror",
                                    tint = TvCrimsonPrimary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "LIVE SCREEN MIRROR",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Black,
                                        color = Color.White,
                                        letterSpacing = 1.sp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(TvAccentGreen.copy(alpha = 0.2f))
                                            .border(1.dp, TvAccentGreen, RoundedCornerShape(6.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "STREAMING",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = TvAccentGreen,
                                            fontSize = 9.sp
                                        )
                                    }
                                }

                                Text(
                                    text = "Source: ${mirrorState.source.title} • Connected: ${mirrorState.connectedDeviceName}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF94A3B8)
                                )
                            }
                        }

                        // Stream Telemetry Badges
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            TelemetryBadge(
                                label = "RESOLUTION",
                                value = mirrorState.quality.resolution,
                                color = TvAccentCyan
                            )
                            TelemetryBadge(
                                label = "FPS",
                                value = "${mirrorState.activeFps} FPS",
                                color = TvAccentGreen
                            )
                            TelemetryBadge(
                                label = "BITRATE",
                                value = "${mirrorState.currentBitrateMbps} Mbps",
                                color = TvAccentAmber
                            )
                            TelemetryBadge(
                                label = "LATENCY",
                                value = "${mirrorState.latencyMs} ms",
                                color = Color(0xFF38BDF8)
                            )

                            // Quick Screen Controls
                            IconButton(
                                onClick = { viewModel.toggleMirrorOrientation() },
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(TvSurfaceVariant)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ScreenRotation,
                                    contentDescription = "Rotate",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            IconButton(
                                onClick = { isBorderlessFullscreen = !isBorderlessFullscreen },
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(TvSurfaceVariant)
                            ) {
                                Icon(
                                    imageVector = if (isBorderlessFullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                                    contentDescription = "Fullscreen",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            IconButton(
                                onClick = { viewModel.stopScreenMirroring() },
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(TvCrimsonPrimary)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Stop,
                                    contentDescription = "Stop Mirroring",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Main Mirrored Display Viewport
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(if (isBorderlessFullscreen) 0.dp else 16.dp),
                contentAlignment = Alignment.Center
            ) {
                MirroredScreenCanvas(
                    mirrorState = mirrorState,
                    isBorderless = isBorderlessFullscreen,
                    onDraw = { x, y -> viewModel.addMirrorAnnotation(x, y) },
                    modifier = Modifier.fillMaxSize()
                )

                // Virtual Remote Cursor (Synced with phone trackpad/touch)
                if (isCursorVisible) {
                    Box(
                        modifier = Modifier
                            .offset(cursorPosition.first, cursorPosition.second)
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.85f))
                            .border(2.dp, TvCrimsonPrimary, CircleShape)
                    )
                }

                // Fullscreen restore button if collapsed
                if (isBorderlessFullscreen) {
                    IconButton(
                        onClick = { isBorderlessFullscreen = false },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(20.dp)
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.6f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.FullscreenExit,
                            contentDescription = "Exit Fullscreen",
                            tint = Color.White
                        )
                    }
                }
            }

            // Audio Stream & Status Footer
            AnimatedVisibility(
                visible = !isBorderlessFullscreen,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .border(1.dp, TvBorderDark, RoundedCornerShape(16.dp)),
                    color = TvSurfaceDark
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (mirrorState.isAudioEnabled) Icons.Default.VolumeUp else Icons.Default.VolumeMute,
                                contentDescription = "Audio Stream",
                                tint = if (mirrorState.isAudioEnabled) TvAccentGreen else Color.Gray,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (mirrorState.isAudioEnabled) "Ultra-HD Stereo PCM Stream (48kHz)" else "Audio Muted",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (mirrorState.isAudioEnabled) Color.White else Color.Gray,
                                fontWeight = FontWeight.Bold
                            )

                            // Audio visualizer bars
                            if (mirrorState.isAudioEnabled) {
                                Spacer(modifier = Modifier.width(12.dp))
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val heights = listOf(14.dp, 22.dp, 10.dp, 28.dp, 18.dp, 24.dp, 12.dp, 20.dp)
                                    heights.forEachIndexed { i, h ->
                                        Box(
                                            modifier = Modifier
                                                .width(3.dp)
                                                .height(h * if (i % 2 == 0) pulseAnim else (1.4f - pulseAnim * 0.4f))
                                                .clip(RoundedCornerShape(2.dp))
                                                .background(TvAccentGreen)
                                        )
                                    }
                                }
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                text = "Orientation: ${mirrorState.orientation.name}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF94A3B8)
                            )
                            Text(
                                text = "Privacy Shield: ${if (mirrorState.isPrivacyMode) "PROTECTED" else "OFF"}",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (mirrorState.isPrivacyMode) TvAccentAmber else Color(0xFF94A3B8),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MirroredScreenCanvas(
    mirrorState: com.example.data.model.ScreenMirrorState,
    isBorderless: Boolean,
    onDraw: (Float, Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val isLandscape = mirrorState.orientation == MirrorOrientation.LANDSCAPE
    val aspectRatio = if (isLandscape) 16f / 9f else 9f / 19.5f

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .fillMaxHeight(if (isLandscape) 0.95f else 0.98f)
                .aspectRatio(aspectRatio)
                .clip(RoundedCornerShape(if (isBorderless) 0.dp else 28.dp))
                .border(
                    if (isBorderless) 0.dp else 4.dp,
                    if (isBorderless) Color.Transparent else Color(0xFF334155),
                    RoundedCornerShape(if (isBorderless) 0.dp else 28.dp)
                )
                .shadow(24.dp, RoundedCornerShape(28.dp)),
            color = Color(0xFF090D16)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectDragGestures { change, _ ->
                            onDraw(change.position.x, change.position.y)
                        }
                    }
            ) {
                if (mirrorState.liveBitmap != null) {
                    Image(
                        bitmap = mirrorState.liveBitmap.asImageBitmap(),
                        contentDescription = "Live Mirrored Screen",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    when (mirrorState.source) {
                        MirrorSource.FULL_SCREEN -> {
                            PhoneHomeScreenMirrorView(mirrorState = mirrorState)
                        }
                        MirrorSource.PHOTOS_GALLERY -> {
                            PhotoGalleryMirrorView(mirrorState = mirrorState)
                        }
                        MirrorSource.CAMERA_LIVE -> {
                            LiveCameraMirrorView(mirrorState = mirrorState)
                        }
                        MirrorSource.GAME_MODE -> {
                            GamingMirrorView(mirrorState = mirrorState)
                        }
                        MirrorSource.DOCUMENT_SLIDES -> {
                            PresentationMirrorView(mirrorState = mirrorState)
                        }
                    }
                }

                // Annotation Layer (Drawings / Laser Lines from Phone/TV)
                Canvas(modifier = Modifier.fillMaxSize()) {
                    if (mirrorState.annotationLines.isNotEmpty()) {
                        val points = mirrorState.annotationLines.map { Offset(it.x, it.y) }
                        drawPoints(
                            points = points,
                            pointMode = PointMode.Polygon,
                            color = Color(0xFFFF0055),
                            strokeWidth = 6f,
                            cap = StrokeCap.Round
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PhoneHomeScreenMirrorView(mirrorState: com.example.data.model.ScreenMirrorState) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF0F172A),
                        Color(0xFF1E1B4B),
                        Color(0xFF090D16)
                    )
                )
            )
            .padding(12.dp)
    ) {
        // Mirrored Phone Status Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "08:42",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                fontSize = 11.sp
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(Icons.Default.Wifi, "Wi-Fi", tint = Color.White, modifier = Modifier.size(12.dp))
                Icon(Icons.Default.CastConnected, "Cast", tint = TvAccentGreen, modifier = Modifier.size(12.dp))
                Text(
                    text = "98%",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 11.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Mirrored Clock Widget
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Tuesday, August 25",
                style = MaterialTheme.typography.labelMedium,
                color = Color(0xFF94A3B8)
            )
            Text(
                text = "08:42",
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Black,
                color = Color.White
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Mirrored App Grid
        val apps = listOf(
            Pair("Camera", Color(0xFFE11D48)),
            Pair("Gallery", Color(0xFF3B82F6)),
            Pair("YouTube", Color(0xFFDC2626)),
            Pair("Chrome", Color(0xFFF59E0B)),
            Pair("Spotify", Color(0xFF10B981)),
            Pair("Maps", Color(0xFF6366F1)),
            Pair("Files", Color(0xFF8B5CF6)),
            Pair("Settings", Color(0xFF64748B))
        )

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            apps.chunked(4).forEach { rowApps ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    rowApps.forEach { app ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(app.second),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = app.first.take(1),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = app.first,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Mirrored Bottom Dock
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp)),
            color = Color.White.copy(alpha = 0.1f)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                listOf(
                    Pair("Phone", Color(0xFF10B981)),
                    Pair("Messages", Color(0xFF3B82F6)),
                    Pair("Browser", Color(0xFFF59E0B)),
                    Pair("Camera", Color(0xFFE11D48))
                ).forEach { dockApp ->
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(dockApp.second),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = dockApp.first.take(1),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PhotoGalleryMirrorView(mirrorState: com.example.data.model.ScreenMirrorState) {
    val photos = listOf(
        Pair("Alpine Mountains 4K", Color(0xFF1E3A8A)),
        Pair("Golden Sunset Coast", Color(0xFF9A3412)),
        Pair("Cyberpunk Tokyo Neon", Color(0xFF4C1D95)),
        Pair("Tropical Emerald Bay", Color(0xFF065F46))
    )
    val currentPhoto = photos.getOrElse(mirrorState.activePhotoIndex % photos.size) { photos.first() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(currentPhoto.second),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(20.dp)
        ) {
            Icon(
                imageVector = Icons.Default.PhotoLibrary,
                contentDescription = "Photo",
                tint = Color.White.copy(alpha = 0.8f),
                modifier = Modifier.size(54.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = currentPhoto.first,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                color = Color.White
            )
            Text(
                text = "3840 x 2160 • Shot with HDR10+ • 12-bit RAW",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.7f)
            )
        }

        // Photo Index Indicator
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            photos.forEachIndexed { idx, _ ->
                Box(
                    modifier = Modifier
                        .size(if (idx == mirrorState.activePhotoIndex % photos.size) 10.dp else 6.dp)
                        .clip(CircleShape)
                        .background(if (idx == mirrorState.activePhotoIndex % photos.size) Color.White else Color.White.copy(alpha = 0.4f))
                )
            }
        }
    }
}

@Composable
private fun LiveCameraMirrorView(mirrorState: com.example.data.model.ScreenMirrorState) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
    ) {
        // Camera Viewfinder lines
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            // Grid 3x3
            drawLine(Color.White.copy(alpha = 0.2f), Offset(w / 3, 0f), Offset(w / 3, h), strokeWidth = 1.5f)
            drawLine(Color.White.copy(alpha = 0.2f), Offset(w * 2 / 3, 0f), Offset(w * 2 / 3, h), strokeWidth = 1.5f)
            drawLine(Color.White.copy(alpha = 0.2f), Offset(0f, h / 3), Offset(w, h / 3), strokeWidth = 1.5f)
            drawLine(Color.White.copy(alpha = 0.2f), Offset(0f, h * 2 / 3), Offset(w, h * 2 / 3), strokeWidth = 1.5f)

            // Center Focus Box
            drawCircle(TvCrimsonPrimary, radius = 24f, center = center, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3f))
        }

        // Camera live overlay
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(TvCrimsonPrimary)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "LIVE 4K 60FPS",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
            Text(
                text = "f/1.8 • 1/120s • ISO 100",
                style = MaterialTheme.typography.labelSmall,
                color = TvAccentCyan
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Phone Camera Live Cast",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = "Streaming real-time 4K optical sensor stream directly to TV",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF94A3B8)
            )
        }
    }
}

@Composable
private fun GamingMirrorView(mirrorState: com.example.data.model.ScreenMirrorState) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    listOf(
                        Color(0xFF312E81),
                        Color(0xFF0F172A)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(TvAccentGreen.copy(alpha = 0.2f))
                    .border(1.dp, TvAccentGreen, RoundedCornerShape(12.dp))
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "LOW-LATENCY GAME MODE (6ms)",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Black,
                    color = TvAccentGreen
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Asphalt Legends 9 (Mirroring)",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black,
                color = Color.White
            )
            Text(
                text = "Hardware Accelerated H.265 Direct Wi-Fi Display Stream",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF94A3B8)
            )
        }
    }
}

@Composable
private fun PresentationMirrorView(mirrorState: com.example.data.model.ScreenMirrorState) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Executive Project Review & Keynote",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black,
                color = Color(0xFF0F172A)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Quarterly Strategic Milestone & Expansion Plan",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF475569)
            )
            Spacer(modifier = Modifier.height(20.dp))
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp)),
                color = Color(0xFFF1F5F9)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("• Local Wi-Fi Direct Stream: 0 packet loss", color = Color(0xFF1E293B), fontWeight = FontWeight.Bold)
                    Text("• Real-time slide synchronization with low latency", color = Color(0xFF1E293B))
                    Text("• Interactive Pen markup on TV screen", color = Color(0xFF1E293B))
                }
            }
        }
    }
}

@Composable
private fun TelemetryBadge(label: String, value: String, color: Color) {
    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .border(1.dp, color.copy(alpha = 0.4f), RoundedCornerShape(10.dp)),
        color = color.copy(alpha = 0.12f)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = color,
                fontSize = 8.sp
            )
            Text(
                text = value,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Black,
                color = Color.White,
                fontSize = 11.sp
            )
        }
    }
}

private fun Modifier.offset(x: Float, y: Float): Modifier = this.then(
    Modifier.padding(start = (x / 1920f * 300f).dp, top = (y / 1080f * 200f).dp)
)
