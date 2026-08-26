package com.example.ui.screens.tv

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.CommandType
import com.example.data.model.RemoteCommand
import com.example.network.NetworkUtils
import com.example.ui.theme.TvAccentCyan
import com.example.ui.theme.TvAccentGreen
import com.example.ui.theme.TvCrimsonPrimary
import com.example.ui.theme.TvObsidianBackground
import com.example.ui.theme.TvSurfaceDark
import com.example.ui.theme.TvSurfaceVariant
import com.example.ui.viewmodel.TvCompanionViewModel

@Composable
fun TvCinemaPlayerScreen(
    viewModel: TvCompanionViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val playbackState by viewModel.playbackState.collectAsStateWithLifecycle()
    var showOsd by remember { mutableStateOf(true) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable { showOsd = !showOsd }
            .testTag("tv_cinema_player_canvas")
    ) {
        // Simulated Fullscreen Video Canvas with dynamic gradient lighting
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF2B0A12),
                            Color(0xFF100305),
                            Color.Black
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(90.dp)
                        .clip(CircleShape)
                        .background(TvCrimsonPrimary.copy(alpha = 0.25f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (playbackState.isPlaying) Icons.Default.Movie else Icons.Default.Pause,
                        contentDescription = "Video Stream",
                        tint = TvCrimsonPrimary,
                        modifier = Modifier.size(48.dp)
                    )
                }

                Text(
                    text = playbackState.title,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(top = 16.dp)
                )

                Text(
                    text = "4K HDR • 60 FPS • Dolby Atmos • Cast Stream",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TvAccentCyan,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        // On-Screen Display (OSD) Overlay
        AnimatedVisibility(
            visible = showOsd,
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color(0xCC05070B), Color(0xF505070B))
                        )
                    )
                    .padding(32.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = playbackState.title,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = playbackState.subtitle,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF94A3B8)
                            )
                        }

                        // Volume Indicator
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(TvSurfaceDark)
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.VolumeUp, "Volume", tint = TvAccentCyan, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "${playbackState.volume}%",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }

                    // Progress Bar
                    val maxDur = if (playbackState.durationMs > 0) playbackState.durationMs.toFloat() else 600000f
                    val curPos = playbackState.currentPositionMs.toFloat().coerceIn(0f, maxDur)

                    Slider(
                        value = curPos,
                        onValueChange = { newPos ->
                            viewModel.sendRemoteCommand(RemoteCommand(CommandType.SEEK_TO, intValue = newPos.toInt()))
                        },
                        valueRange = 0f..maxDur,
                        colors = SliderDefaults.colors(
                            thumbColor = TvCrimsonPrimary,
                            activeTrackColor = TvCrimsonPrimary,
                            inactiveTrackColor = Color(0xFF334155)
                        ),
                        modifier = Modifier.padding(top = 10.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = NetworkUtils.formatDuration(playbackState.currentPositionMs),
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White
                        )
                        Text(
                            text = NetworkUtils.formatDuration(playbackState.durationMs),
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF94A3B8)
                        )
                    }

                    // Bottom Control Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                val newPos = (playbackState.currentPositionMs - 10000L).coerceAtLeast(0L)
                                viewModel.sendRemoteCommand(RemoteCommand(CommandType.SEEK_TO, intValue = newPos.toInt()))
                            }
                        ) {
                            Icon(Icons.Default.FastRewind, "Rewind 10s", tint = Color.White, modifier = Modifier.size(32.dp))
                        }

                        Spacer(modifier = Modifier.width(20.dp))

                        IconButton(
                            onClick = { viewModel.sendRemoteCommand(RemoteCommand(CommandType.PLAY_PAUSE)) },
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(TvCrimsonPrimary)
                        ) {
                            Icon(
                                imageVector = if (playbackState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = "Play/Pause",
                                tint = Color.White,
                                modifier = Modifier.size(36.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(20.dp))

                        IconButton(
                            onClick = {
                                val newPos = playbackState.currentPositionMs + 10000L
                                viewModel.sendRemoteCommand(RemoteCommand(CommandType.SEEK_TO, intValue = newPos.toInt()))
                            }
                        ) {
                            Icon(Icons.Default.FastForward, "Forward 10s", tint = Color.White, modifier = Modifier.size(32.dp))
                        }
                    }
                }
            }
        }

        // Back Button Top Left
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .padding(24.dp)
                .size(48.dp)
                .clip(CircleShape)
                .background(Color(0x88000000))
                .align(Alignment.TopStart)
                .testTag("btn_tv_player_back")
        ) {
            Icon(Icons.Default.ArrowBack, "Back", tint = Color.White)
        }
    }
}
