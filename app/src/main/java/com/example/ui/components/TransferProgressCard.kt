package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.FileCategory
import com.example.data.model.SharedFile
import com.example.data.model.TransferStatus
import com.example.ui.theme.TvAccentCyan
import com.example.ui.theme.TvAccentGreen
import com.example.ui.theme.TvBorderDark
import com.example.ui.theme.TvCrimsonPrimary
import com.example.ui.theme.TvSurfaceDark
import com.example.ui.theme.TvSurfaceVariant

@Composable
fun TransferProgressCard(
    file: SharedFile?,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = file != null && (file.status == TransferStatus.TRANSFERRING || file.status == TransferStatus.COMPLETED),
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
    ) {
        if (file == null) return@AnimatedVisibility

        val (catIcon, catColor) = when (file.category) {
            FileCategory.VIDEO -> Pair(Icons.Default.Movie, TvCrimsonPrimary)
            FileCategory.PDF -> Pair(Icons.Default.PictureAsPdf, Color(0xFFFF9800))
            FileCategory.AUDIO -> Pair(Icons.Default.MusicNote, Color(0xFFAB47BC))
            FileCategory.APK -> Pair(Icons.Default.FolderZip, TvAccentGreen)
            else -> Pair(Icons.Default.FileUpload, TvAccentCyan)
        }

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
                .clip(RoundedCornerShape(18.dp))
                .border(1.dp, catColor.copy(alpha = 0.5f), RoundedCornerShape(18.dp)),
            color = TvSurfaceDark
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(catColor.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (file.status == TransferStatus.COMPLETED) Icons.Default.CheckCircle else catIcon,
                                contentDescription = file.fileName,
                                tint = catColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Column(modifier = Modifier.padding(start = 10.dp)) {
                            Text(
                                text = file.fileName,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${file.formattedSize} • ${if (file.isIncoming) "Receiving from Phone" else "Sending over Wi-Fi"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF94A3B8)
                            )
                        }
                    }

                    Text(
                        text = if (file.status == TransferStatus.COMPLETED) "COMPLETE" else "${(file.progress * 100).toInt()}%",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (file.status == TransferStatus.COMPLETED) TvAccentGreen else TvAccentCyan
                    )
                }

                if (file.status == TransferStatus.TRANSFERRING) {
                    LinearProgressIndicator(
                        progress = { file.progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp)
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = catColor,
                        trackColor = TvSurfaceVariant
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Speed,
                                contentDescription = "Transfer Speed",
                                tint = TvAccentCyan,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = " %.1f Mbps".format(file.speedMbps),
                                style = MaterialTheme.typography.labelSmall,
                                color = TvAccentCyan,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Text(
                            text = "Wi-Fi High-Speed Stream",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF64748B)
                        )
                    }
                }
            }
        }
    }
}
