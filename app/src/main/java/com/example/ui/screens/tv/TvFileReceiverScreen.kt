package com.example.ui.screens.tv

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderShared
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
fun TvFileReceiverScreen(
    viewModel: TvCompanionViewModel,
    onPlayMedia: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sharedFiles by viewModel.sharedFilesList.collectAsStateWithLifecycle()
    val activeTransfer by viewModel.activeFileTransfer.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(TvObsidianBackground)
            .padding(horizontal = 32.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(TvSurfaceVariant)
                        .testTag("btn_tv_files_back")
                ) {
                    Icon(Icons.Default.ArrowBack, "Back", tint = Color.White)
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Text(
                        text = "Received Files Hub (Android TV Storage)",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Movies (>1GB), Documents, APKs, and Audio sent from connected phones",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF94A3B8)
                    )
                }
            }
        }

        if (activeTransfer != null) {
            item {
                TransferProgressCard(file = activeTransfer)
            }
        }

        if (sharedFiles.isEmpty()) {
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp)),
                    color = TvSurfaceDark
                ) {
                    Column(
                        modifier = Modifier.padding(36.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Folder,
                            contentDescription = "Empty",
                            tint = Color(0xFF64748B),
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = "No files received on TV yet",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            modifier = Modifier.padding(top = 12.dp)
                        )
                        Text(
                            text = "Use the Phone app on the same Wi-Fi network to send movies, APKs, or documents directly to this TV.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF94A3B8),
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        }

        items(sharedFiles, key = { it.id }) { file ->
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
                    .clip(RoundedCornerShape(18.dp))
                    .border(1.dp, TvBorderDark, RoundedCornerShape(18.dp)),
                color = TvSurfaceDark
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
                                .size(48.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(badgeColor.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = file.fileName,
                                tint = badgeColor,
                                modifier = Modifier.size(26.dp)
                            )
                        }

                        Column(modifier = Modifier.padding(start = 14.dp)) {
                            Text(
                                text = file.fileName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${file.formattedSize} • ${file.category.name} File • High-Speed Transferred",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF94A3B8)
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Button(
                            onClick = {
                                viewModel.castMediaToTv(
                                    CastPayload(
                                        title = file.fileName,
                                        subtitle = "Local File Storage (${file.formattedSize})",
                                        mediaType = if (file.category == FileCategory.PDF) MediaType.PDF_DOC else MediaType.VIDEO,
                                        streamUrl = file.localUriOrPath
                                    )
                                )
                                onPlayMedia()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = TvCrimsonPrimary),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.PlayArrow, "Open", modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (file.category == FileCategory.PDF) "Open PDF" else "Play on TV", fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        IconButton(
                            onClick = { viewModel.deleteSharedFile(file.id) }
                        ) {
                            Icon(Icons.Default.Delete, "Delete", tint = Color(0xFF64748B))
                        }
                    }
                }
            }
        }
    }
}
