package com.example.ui.screens.tv

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
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
import androidx.compose.material.icons.filled.FolderShared
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SmartDisplay
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.AppShortcut
import com.example.data.model.MediaType
import com.example.network.NetworkUtils
import com.example.ui.components.TransferProgressCard
import com.example.ui.theme.TvAccentAmber
import com.example.ui.theme.TvAccentCyan
import com.example.ui.theme.TvAccentGreen
import com.example.ui.theme.TvBorderDark
import com.example.ui.theme.TvCrimsonPrimary
import com.example.ui.theme.TvObsidianBackground
import com.example.ui.theme.TvSurfaceDark
import com.example.ui.theme.TvSurfaceHighlight
import com.example.ui.theme.TvSurfaceVariant
import com.example.ui.viewmodel.TvCompanionViewModel

@Composable
fun TvDashboardScreen(
    viewModel: TvCompanionViewModel,
    onNavigateToScreenMirror: () -> Unit,
    onNavigateToPlayer: () -> Unit,
    onNavigateToBrowser: () -> Unit,
    onNavigateToPdf: () -> Unit,
    onNavigateToFiles: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val playbackState by viewModel.playbackState.collectAsStateWithLifecycle()
    val mirrorState by viewModel.screenMirrorState.collectAsStateWithLifecycle()
    val activeTransfer by viewModel.activeFileTransfer.collectAsStateWithLifecycle()
    val isServerRunning by viewModel.httpServer.isRunning.collectAsStateWithLifecycle()
    val cursorPosition by viewModel.virtualCursorPosition.collectAsStateWithLifecycle()
    val isCursorVisible by viewModel.isCursorVisible.collectAsStateWithLifecycle()

    val localIp = NetworkUtils.getLocalIpAddress(context)
    val wifiSsid = NetworkUtils.getWifiSsid(context)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF1E0A10),
                        TvObsidianBackground,
                        Color(0xFF030712)
                    )
                )
            )
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // TV Header & Server Status Banner
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .border(1.dp, TvCrimsonPrimary.copy(alpha = 0.5f), RoundedCornerShape(24.dp))
                        .testTag("tv_header_status"),
                    color = TvSurfaceDark
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(CircleShape)
                                    .background(TvCrimsonPrimary.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Tv,
                                    contentDescription = "TV Receiver",
                                    tint = TvCrimsonPrimary,
                                    modifier = Modifier.size(32.dp)
                                )
                            }

                            Column(modifier = Modifier.padding(start = 16.dp)) {
                                Text(
                                    text = "Android TV Set-Top Box Receiver",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(top = 4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(if (isServerRunning) TvAccentGreen else Color.Gray)
                                    )
                                    Text(
                                        text = " Ready for Phone Remote & Cast • IP: http://$localIp:8080 • Wi-Fi: $wifiSsid",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color(0xFF94A3B8)
                                    )
                                }
                            }
                        }

                        // PIN Badge
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0xFF1E2435))
                                .border(1.dp, TvAccentCyan.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                                .padding(horizontal = 16.dp, vertical = 10.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "PAIRING PIN",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = TvAccentCyan
                                )
                                Text(
                                    text = "7824",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White,
                                    letterSpacing = 2.sp
                                )
                            }
                        }
                    }
                }
            }

            // Active Screen Mirroring Card (if screen casting is active)
            if (mirrorState.isMirroring) {
                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .border(1.5.dp, TvCrimsonPrimary, RoundedCornerShape(20.dp))
                            .clickable(onClick = onNavigateToScreenMirror)
                            .testTag("tv_active_screen_mirror_card"),
                        color = TvSurfaceDark
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(50.dp)
                                        .clip(CircleShape)
                                        .background(TvCrimsonPrimary),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CastConnected,
                                        contentDescription = "Screen Mirror Active",
                                        tint = Color.White,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }

                                Column(modifier = Modifier.padding(start = 14.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "LIVE PHONE SCREEN MIRRORING ACTIVE",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Black,
                                            color = Color.White
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(TvAccentGreen)
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = "${mirrorState.activeFps} FPS",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.Black,
                                                fontSize = 9.sp
                                            )
                                        }
                                    }
                                    Text(
                                        text = "Source: ${mirrorState.source.title} • Latency: ${mirrorState.latencyMs}ms • ${mirrorState.currentBitrateMbps} Mbps",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color(0xFF94A3B8)
                                    )
                                }
                            }

                            Button(
                                onClick = onNavigateToScreenMirror,
                                colors = ButtonDefaults.buttonColors(containerColor = TvCrimsonPrimary),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Open Mirrored Screen", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Active Cast Player Card (if media is playing)
            if (playbackState.isPlaying || playbackState.streamUrl.isNotBlank()) {
                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .border(1.dp, TvCrimsonPrimary, RoundedCornerShape(20.dp))
                            .clickable(onClick = onNavigateToPlayer)
                            .testTag("tv_active_player_card"),
                        color = TvSurfaceDark
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(50.dp)
                                        .clip(CircleShape)
                                        .background(TvCrimsonPrimary),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = "Play",
                                        tint = Color.White,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }

                                Column(modifier = Modifier.padding(start = 14.dp)) {
                                    Text(
                                        text = "NOW PLAYING: ${playbackState.title}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "${playbackState.subtitle} • Volume ${playbackState.volume}%",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color(0xFF94A3B8)
                                    )
                                }
                            }

                            Button(
                                onClick = onNavigateToPlayer,
                                colors = ButtonDefaults.buttonColors(containerColor = TvCrimsonPrimary),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Open Cinema Player", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Active File Transfer Card
            if (activeTransfer != null) {
                item {
                    TransferProgressCard(file = activeTransfer)
                }
            }

            // TV Main Applications Grid
            item {
                Text(
                    text = "SET-TOP BOX APPS & HUBS",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF94A3B8),
                    letterSpacing = 1.5.sp,
                    modifier = Modifier.padding(top = 10.dp)
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    TvAppFeatureCard(
                        title = "Screen Mirroring",
                        subtitle = "Live Phone Display Stream",
                        icon = Icons.Default.CastConnected,
                        accentColor = Color(0xFFE11D48),
                        onClick = onNavigateToScreenMirror,
                        modifier = Modifier.weight(1f),
                        tag = "tv_card_screen_mirror"
                    )

                    TvAppFeatureCard(
                        title = "Cinema Player",
                        subtitle = "Direct Ultra HD Stream",
                        icon = Icons.Default.Movie,
                        accentColor = TvCrimsonPrimary,
                        onClick = onNavigateToPlayer,
                        modifier = Modifier.weight(1f),
                        tag = "tv_card_player"
                    )

                    TvAppFeatureCard(
                        title = "TV Web Browser",
                        subtitle = "Full Web & Video",
                        icon = Icons.Default.Language,
                        accentColor = TvAccentCyan,
                        onClick = onNavigateToBrowser,
                        modifier = Modifier.weight(1f),
                        tag = "tv_card_browser"
                    )

                    TvAppFeatureCard(
                        title = "PDF Presenter",
                        subtitle = "Slide Document Viewer",
                        icon = Icons.Default.PictureAsPdf,
                        accentColor = Color(0xFFFF9800),
                        onClick = onNavigateToPdf,
                        modifier = Modifier.weight(1f),
                        tag = "tv_card_pdf"
                    )

                    TvAppFeatureCard(
                        title = "File Hub (>1GB)",
                        subtitle = "Movies & APK Transfers",
                        icon = Icons.Default.FolderShared,
                        accentColor = TvAccentGreen,
                        onClick = onNavigateToFiles,
                        modifier = Modifier.weight(1f),
                        tag = "tv_card_files"
                    )
                }
            }
        }

        // Virtual pointer cursor on TV screen when Phone moves trackpad
        if (isCursorVisible) {
            Box(
                modifier = Modifier
                    .offset { IntOffset(cursorPosition.first.toInt(), cursorPosition.second.toInt()) }
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.9f))
                    .border(2.dp, TvCrimsonPrimary, CircleShape)
                    .shadow(8.dp, CircleShape)
            )
        }
    }
}

@Composable
private fun TvAppFeatureCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tag: String
) {
    Surface(
        modifier = modifier
            .height(160.dp)
            .clip(RoundedCornerShape(20.dp))
            .border(1.dp, TvBorderDark, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .testTag(tag),
        color = TvSurfaceDark
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(18.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(accentColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = accentColor,
                    modifier = Modifier.size(26.dp)
                )
            }

            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF94A3B8),
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}
