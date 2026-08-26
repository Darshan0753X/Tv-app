package com.example.ui.screens.phone

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.FolderShared
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import com.example.data.model.CastPayload
import com.example.data.model.FileCategory
import com.example.data.model.MediaType
import com.example.data.model.SharedFile
import com.example.data.model.TransferStatus
import com.example.network.NetworkUtils
import com.example.ui.components.TransferProgressCard
import com.example.ui.theme.TvAccentCyan
import com.example.ui.theme.TvAccentGreen
import com.example.ui.theme.TvBorderDark
import com.example.ui.theme.TvCrimsonPrimary
import com.example.ui.theme.TvObsidianBackground
import com.example.ui.theme.TvSurfaceDark
import com.example.ui.theme.TvSurfaceVariant
import com.example.ui.viewmodel.TvCompanionViewModel

@Composable
fun FileShareScreen(
    viewModel: TvCompanionViewModel,
    modifier: Modifier = Modifier
) {
    val activeTransfer by viewModel.activeFileTransfer.collectAsStateWithLifecycle()
    val sharedFiles by viewModel.sharedFilesList.collectAsStateWithLifecycle()
    val connectedDevice by viewModel.connectedDevice.collectAsStateWithLifecycle()

    var selectedFilter by remember { mutableStateOf("ALL") }

    val filteredFiles = remember(sharedFiles, selectedFilter) {
        if (selectedFilter == "ALL") sharedFiles
        else sharedFiles.filter { it.category.name == selectedFilter }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(TvObsidianBackground)
            .padding(bottom = 90.dp),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Hero Send Files Banner
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .border(1.dp, TvAccentGreen.copy(alpha = 0.4f), RoundedCornerShape(22.dp)),
                color = TvSurfaceDark
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(TvAccentGreen.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FolderShared,
                                    contentDescription = "Share",
                                    tint = TvAccentGreen,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Column(modifier = Modifier.padding(start = 12.dp)) {
                                Text(
                                    text = "Wi-Fi High-Speed File Share",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "Direct transfer to TV (Supports >1GB Movies)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF94A3B8)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Preset Send Quick Buttons
                    Text(
                        text = "SELECT FILE TO SEND TO TV",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF64748B),
                        letterSpacing = 1.sp
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Send 4K Movie (>1GB)
                        QuickSendChip(
                            label = "4K Movie (1.8GB)",
                            icon = Icons.Default.Movie,
                            color = TvCrimsonPrimary,
                            onClick = {
                                viewModel.shareFileToTv(
                                    fileName = "Dune_Part2_4K_IMAX_Rip.mkv",
                                    fileSize = 1850000000L, // 1.85 GB
                                    category = FileCategory.VIDEO
                                )
                            },
                            modifier = Modifier.weight(1f),
                            tag = "btn_send_movie"
                        )

                        // Send APK
                        QuickSendChip(
                            label = "TV App (APK)",
                            icon = Icons.Default.FolderZip,
                            color = TvAccentGreen,
                            onClick = {
                                viewModel.shareFileToTv(
                                    fileName = "VLC_Player_AndroidTV.apk",
                                    fileSize = 48200000L,
                                    category = FileCategory.APK
                                )
                            },
                            modifier = Modifier.weight(1f),
                            tag = "btn_send_apk"
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Send PDF Presentation
                        QuickSendChip(
                            label = "PDF Slide (24MB)",
                            icon = Icons.Default.PictureAsPdf,
                            color = Color(0xFFFF9800),
                            onClick = {
                                viewModel.shareFileToTv(
                                    fileName = "Q3_Company_Presentation_Master.pdf",
                                    fileSize = 24800000L,
                                    category = FileCategory.PDF
                                )
                            },
                            modifier = Modifier.weight(1f),
                            tag = "btn_send_pdf"
                        )

                        // Send Music FLAC
                        QuickSendChip(
                            label = "Album (FLAC)",
                            icon = Icons.Default.MusicNote,
                            color = Color(0xFFAB47BC),
                            onClick = {
                                viewModel.shareFileToTv(
                                    fileName = "Daft_Punk_Random_Access_Memories.flac",
                                    fileSize = 85400000L,
                                    category = FileCategory.AUDIO
                                )
                            },
                            modifier = Modifier.weight(1f),
                            tag = "btn_send_music"
                        )
                    }
                }
            }
        }

        // Active Transfer Card
        if (activeTransfer != null) {
            item {
                TransferProgressCard(file = activeTransfer)
            }
        }

        // Category Filters
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf("ALL", "VIDEO", "PDF", "APK", "AUDIO").forEach { filter ->
                    FilterPill(
                        label = filter,
                        selected = selectedFilter == filter,
                        onClick = { selectedFilter = filter }
                    )
                }
            }
        }

        // Section Title & Clear
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "TRANSFERRED FILES HISTORY (${filteredFiles.size})",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF94A3B8),
                    letterSpacing = 1.2.sp
                )

                if (sharedFiles.isNotEmpty()) {
                    Text(
                        text = "Clear All",
                        style = MaterialTheme.typography.labelSmall,
                        color = TvCrimsonPrimary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clickable { viewModel.clearSharedFiles() }
                            .padding(4.dp)
                            .testTag("btn_clear_files")
                    )
                }
            }
        }

        // File List
        items(filteredFiles, key = { it.id }) { file ->
            SharedFileRow(
                file = file,
                onPlayOnTv = {
                    viewModel.castMediaToTv(
                        CastPayload(
                            title = file.fileName,
                            subtitle = "Transferred File (${file.formattedSize})",
                            mediaType = if (file.category == FileCategory.PDF) MediaType.PDF_DOC else MediaType.VIDEO,
                            streamUrl = file.localUriOrPath
                        )
                    )
                },
                onDelete = { viewModel.deleteSharedFile(file.id) }
            )
        }
    }
}

@Composable
private fun QuickSendChip(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tag: String
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(TvSurfaceVariant)
            .border(1.dp, TvBorderDark, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .testTag(tag),
        color = TvSurfaceVariant
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = color,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun FilterPill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(18.dp))
            .background(if (selected) TvAccentGreen else TvSurfaceVariant)
            .border(1.dp, if (selected) TvAccentGreen else TvBorderDark, RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .testTag("filter_$label")
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = if (selected) Color(0xFF0F172A) else Color(0xFF94A3B8)
        )
    }
}

@Composable
private fun SharedFileRow(
    file: SharedFile,
    onPlayOnTv: () -> Unit,
    onDelete: () -> Unit
) {
    val (icon, badgeColor) = when (file.category) {
        FileCategory.VIDEO -> Pair(Icons.Default.Movie, TvCrimsonPrimary)
        FileCategory.PDF -> Pair(Icons.Default.PictureAsPdf, Color(0xFFFF9800))
        FileCategory.APK -> Pair(Icons.Default.FolderZip, TvAccentGreen)
        FileCategory.AUDIO -> Pair(Icons.Default.MusicNote, Color(0xFFAB47BC))
        else -> Pair(Icons.Default.FolderShared, TvAccentCyan)
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, TvBorderDark, RoundedCornerShape(16.dp))
            .testTag("file_row_${file.id}"),
        color = TvSurfaceDark
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(badgeColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = file.fileName,
                        tint = badgeColor,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Column(modifier = Modifier.padding(start = 12.dp)) {
                    Text(
                        text = file.fileName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${file.formattedSize} • ${if (file.isIncoming) "Received on TV" else "Sent from Phone"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF94A3B8)
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onPlayOnTv,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(TvCrimsonPrimary.copy(alpha = 0.2f))
                        .testTag("btn_play_file_${file.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Tv,
                        contentDescription = "Play on TV",
                        tint = TvCrimsonPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.testTag("btn_delete_file_${file.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Delete",
                        tint = Color(0xFF64748B),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
