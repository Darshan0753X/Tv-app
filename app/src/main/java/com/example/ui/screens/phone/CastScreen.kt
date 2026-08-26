package com.example.ui.screens.phone

import android.app.Activity
import android.content.Context
import android.media.projection.MediaProjectionManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CastConnected
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.NavigateBefore
import androidx.compose.material.icons.filled.NavigateNext
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.ScreenShare
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.StopScreenShare
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.MirrorOrientation
import com.example.data.model.MirrorQuality
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
fun CastScreen(
    viewModel: TvCompanionViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val mirrorState by viewModel.screenMirrorState.collectAsStateWithLifecycle()
    val connectedDevice by viewModel.connectedDevice.collectAsStateWithLifecycle()

    var showQualityDialog by remember { mutableStateOf(false) }

    val mediaProjectionManager = remember {
        context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as? MediaProjectionManager
    }

    val captureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            viewModel.startMediaProjectionCapture(
                resultCode = result.resultCode,
                resultData = result.data!!,
                source = mirrorState.source,
                quality = mirrorState.quality
            )
        } else {
            // User declined permission or cancelled, fallback to standard stream
            viewModel.startScreenMirroring(mirrorState.source, mirrorState.quality)
        }
    }

    val onToggleCast: () -> Unit = {
        if (mirrorState.isMirroring) {
            viewModel.stopMediaProjectionCapture()
        } else {
            if (mediaProjectionManager != null) {
                try {
                    captureLauncher.launch(mediaProjectionManager.createScreenCaptureIntent())
                } catch (e: Exception) {
                    viewModel.startScreenMirroring(mirrorState.source, mirrorState.quality)
                }
            } else {
                viewModel.startScreenMirroring(mirrorState.source, mirrorState.quality)
            }
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(TvObsidianBackground)
            .padding(bottom = 90.dp),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Live Screen Mirroring Action Card
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .border(
                        1.5.dp,
                        if (mirrorState.isMirroring) TvCrimsonPrimary else TvBorderDark,
                        RoundedCornerShape(24.dp)
                    )
                    .testTag("hero_screen_cast_card"),
                color = TvSurfaceDark
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Status Badge
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(if (mirrorState.isMirroring) TvAccentGreen else Color.Gray)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (mirrorState.isMirroring) {
                                    if (mirrorState.isMediaProjectionActive) "MEDIA PROJECTION ACTIVE" else "SCREEN CASTING LIVE"
                                } else "READY TO CAST SCREEN",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Black,
                                color = if (mirrorState.isMirroring) TvAccentGreen else Color(0xFF94A3B8),
                                letterSpacing = 1.sp
                            )
                        }

                        Text(
                            text = connectedDevice?.name ?: "Android TV 4K",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = TvAccentCyan
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Real-Time Screen Capture Live Preview (If streaming and bitmap available)
                    if (mirrorState.isMirroring && mirrorState.liveBitmap != null) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .border(1.5.dp, TvCrimsonPrimary.copy(alpha = 0.6f), RoundedCornerShape(16.dp)),
                            color = Color.Black
                        ) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Image(
                                    bitmap = mirrorState.liveBitmap!!.asImageBitmap(),
                                    contentDescription = "Live Captured Screen",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Fit
                                )
                                Surface(
                                    modifier = Modifier
                                        .align(Alignment.TopStart)
                                        .padding(8.dp)
                                        .clip(RoundedCornerShape(6.dp)),
                                    color = Color.Black.copy(alpha = 0.7f)
                                ) {
                                    Text(
                                        text = "LIVE FEED • ${mirrorState.capturedFramesCount} frames",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TvAccentGreen,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    } else {
                        // Center Cast Pulse Button
                        Box(
                            modifier = Modifier
                                .size(100.dp)
                                .scale(if (mirrorState.isMirroring) pulseScale else 1f)
                                .clip(CircleShape)
                                .background(
                                    if (mirrorState.isMirroring) Brush.radialGradient(
                                        listOf(TvCrimsonPrimary, Color(0xFF881337))
                                    ) else Brush.radialGradient(
                                        listOf(TvSurfaceVariant, Color(0xFF1E293B))
                                    )
                                )
                                .border(
                                    2.dp,
                                    if (mirrorState.isMirroring) TvCrimsonPrimary.copy(alpha = 0.8f) else TvBorderDark,
                                    CircleShape
                                )
                                .clickable { onToggleCast() }
                                .testTag("btn_toggle_screen_mirror"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (mirrorState.isMirroring) Icons.Default.StopScreenShare else Icons.Default.ScreenShare,
                                contentDescription = "Toggle Screen Mirror",
                                tint = Color.White,
                                modifier = Modifier.size(44.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    Text(
                        text = if (mirrorState.isMirroring) "Mirroring to ${connectedDevice?.name ?: "TV"}" else "Cast Phone Screen to TV",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = if (mirrorState.isMirroring) "Live Display: ${mirrorState.source.title} (${mirrorState.quality.resolution} @ ${mirrorState.activeFps}fps)" else "Broadcast your phone's live display, camera, photos, or games directly to your TV screen over local Wi-Fi",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF94A3B8),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )

                    // Big Toggle Action Button
                    Button(
                        onClick = { onToggleCast() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp)
                            .testTag("btn_screen_cast_action"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (mirrorState.isMirroring) Color(0xFFDC2626) else TvCrimsonPrimary
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(
                            imageVector = if (mirrorState.isMirroring) Icons.Default.StopScreenShare else Icons.Default.ScreenShare,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (mirrorState.isMirroring) "STOP SCREEN CAST" else "START SCREEN MIRRORING",
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.8.sp
                        )
                    }
                }
            }
        }

        // Live Telemetry Bar (When active)
        if (mirrorState.isMirroring) {
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .border(1.dp, TvAccentGreen.copy(alpha = 0.4f), RoundedCornerShape(16.dp)),
                    color = TvSurfaceDark
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LiveStatItem("FPS", "${mirrorState.activeFps} FPS", TvAccentGreen)
                        LiveStatItem("BITRATE", "${mirrorState.currentBitrateMbps} Mbps", TvAccentAmber)
                        LiveStatItem("LATENCY", "${mirrorState.latencyMs} ms", TvAccentCyan)
                        if (mirrorState.isMediaProjectionActive && mirrorState.capturedFramesCount > 0) {
                            LiveStatItem("FRAMES", "${mirrorState.capturedFramesCount}", Color.White)
                        } else {
                            LiveStatItem("STREAM", "Direct Wi-Fi", Color.White)
                        }
                    }
                }
            }
        }

        // Screen Mirror Source Selection
        item {
            Text(
                text = "SELECT SCREEN MIRROR SOURCE",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF94A3B8),
                letterSpacing = 1.2.sp
            )
        }

        items(MirrorSource.values().toList(), key = { it.name }) { source ->
            val isSelected = mirrorState.source == source
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .border(
                        1.dp,
                        if (isSelected) TvCrimsonPrimary else TvBorderDark,
                        RoundedCornerShape(18.dp)
                    )
                    .clickable {
                        viewModel.setMirrorSource(source)
                    }
                    .testTag("source_card_${source.name.lowercase()}"),
                color = if (isSelected) TvCrimsonPrimary.copy(alpha = 0.12f) else TvSurfaceDark
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isSelected) TvCrimsonPrimary else TvSurfaceVariant
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = getSourceIcon(source),
                                contentDescription = source.title,
                                tint = if (isSelected) Color.White else Color(0xFF94A3B8),
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Column(modifier = Modifier.padding(start = 14.dp)) {
                            Text(
                                text = source.title,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.White else Color(0xFFE2E8F0)
                            )
                            Text(
                                text = source.subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF94A3B8)
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .border(2.dp, if (isSelected) TvCrimsonPrimary else Color(0xFF64748B), CircleShape)
                            .background(if (isSelected) TvCrimsonPrimary else Color.Transparent)
                    )
                }
            }
        }

        // Interactive Source Controls (If Mirroring active)
        if (mirrorState.isMirroring) {
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .border(1.dp, TvBorderDark, RoundedCornerShape(18.dp)),
                    color = TvSurfaceDark
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "ACTIVE MIRROR CONTROLS",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = TvAccentCyan,
                            letterSpacing = 1.2.sp
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        when (mirrorState.source) {
                            MirrorSource.PHOTOS_GALLERY -> {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Button(
                                        onClick = {
                                            viewModel.setMirrorPhotoIndex(mirrorState.activePhotoIndex - 1)
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = TvSurfaceVariant)
                                    ) {
                                        Icon(Icons.Default.NavigateBefore, "Prev")
                                        Text("Previous Photo")
                                    }

                                    Text(
                                        text = "Photo ${(mirrorState.activePhotoIndex % 4) + 1} / 4",
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )

                                    Button(
                                        onClick = {
                                            viewModel.setMirrorPhotoIndex(mirrorState.activePhotoIndex + 1)
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = TvSurfaceVariant)
                                    ) {
                                        Text("Next Photo")
                                        Icon(Icons.Default.NavigateNext, "Next")
                                    }
                                }
                            }
                            MirrorSource.CAMERA_LIVE -> {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceAround
                                ) {
                                    QuickControlPill(label = "Switch Lens", icon = Icons.Default.PhotoCamera) {
                                        viewModel.performHapticFeedback()
                                    }
                                    QuickControlPill(label = "Flash Light", icon = Icons.Default.Tune) {
                                        viewModel.performHapticFeedback()
                                    }
                                }
                            }
                            else -> {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceAround
                                ) {
                                    QuickControlPill(
                                        label = if (mirrorState.orientation == MirrorOrientation.PORTRAIT) "Set Landscape" else "Set Portrait",
                                        icon = Icons.Default.ScreenRotation
                                    ) {
                                        viewModel.toggleMirrorOrientation()
                                    }

                                    QuickControlPill(
                                        label = if (mirrorState.isAudioEnabled) "Mute Audio" else "Unmute Audio",
                                        icon = if (mirrorState.isAudioEnabled) Icons.Default.VolumeUp else Icons.Default.VolumeMute
                                    ) {
                                        viewModel.toggleMirrorAudio()
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Screen Mirroring Preferences & Settings
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .border(1.dp, TvBorderDark, RoundedCornerShape(20.dp)),
                color = TvSurfaceDark
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "STREAM QUALITY & SETTINGS",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF94A3B8),
                        letterSpacing = 1.2.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Quality Selector Pills
                    Text(
                        text = "Stream Resolution & FPS",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MirrorQuality.values().forEach { q ->
                            val isQSelected = mirrorState.quality == q
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isQSelected) TvCrimsonPrimary else TvSurfaceVariant)
                                    .clickable { viewModel.setMirrorQuality(q) }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = q.label.split(" ").first(),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isQSelected) Color.White else Color(0xFF94A3B8)
                                    )
                                    Text(
                                        text = "${q.fps}fps",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isQSelected) Color.White.copy(alpha = 0.8f) else Color(0xFF64748B),
                                        fontSize = 9.sp
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Toggles
                    SettingToggleRow(
                        title = "Transmit Internal Audio",
                        subtitle = "Stream phone apps sound directly to TV speakers",
                        icon = if (mirrorState.isAudioEnabled) Icons.Default.VolumeUp else Icons.Default.VolumeMute,
                        checked = mirrorState.isAudioEnabled,
                        onCheckedChange = { viewModel.toggleMirrorAudio() }
                    )

                    SettingToggleRow(
                        title = "Privacy Shield",
                        subtitle = "Automatically hide incoming notifications on TV",
                        icon = Icons.Default.Security,
                        checked = mirrorState.isPrivacyMode,
                        onCheckedChange = { viewModel.togglePrivacyMode() }
                    )

                    SettingToggleRow(
                        title = "Microphone Narration",
                        subtitle = "Stream your voice through phone mic during mirror",
                        icon = if (mirrorState.isMicEnabled) Icons.Default.Mic else Icons.Default.MicOff,
                        checked = mirrorState.isMicEnabled,
                        onCheckedChange = { viewModel.toggleMirrorMic() }
                    )
                }
            }
        }
    }
}

@Composable
private fun LiveStatItem(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF94A3B8),
            fontSize = 9.sp
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Black,
            color = color,
            fontSize = 12.sp
        )
    }
}

@Composable
private fun QuickControlPill(label: String, icon: ImageVector, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, TvBorderDark, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        color = TvSurfaceVariant
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(imageVector = icon, contentDescription = label, tint = Color.White, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = Color.White)
        }
    }
}

@Composable
private fun SettingToggleRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = if (checked) TvCrimsonPrimary else Color(0xFF64748B),
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF94A3B8)
                )
            }
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = TvCrimsonPrimary,
                uncheckedTrackColor = TvSurfaceVariant
            )
        )
    }
}

private fun getSourceIcon(source: MirrorSource): ImageVector {
    return when (source) {
        MirrorSource.FULL_SCREEN -> Icons.Default.PhoneAndroid
        MirrorSource.PHOTOS_GALLERY -> Icons.Default.PhotoLibrary
        MirrorSource.CAMERA_LIVE -> Icons.Default.PhotoCamera
        MirrorSource.GAME_MODE -> Icons.Default.SportsEsports
        MirrorSource.DOCUMENT_SLIDES -> Icons.Default.Description
    }
}
