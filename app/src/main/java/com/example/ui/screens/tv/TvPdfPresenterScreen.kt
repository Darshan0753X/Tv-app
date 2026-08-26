package com.example.ui.screens.tv

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.PresentToAll
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.theme.TvAccentAmber
import com.example.ui.theme.TvAccentCyan
import com.example.ui.theme.TvBorderDark
import com.example.ui.theme.TvCrimsonPrimary
import com.example.ui.theme.TvObsidianBackground
import com.example.ui.theme.TvSurfaceDark
import com.example.ui.viewmodel.TvCompanionViewModel

@Composable
fun TvPdfPresenterScreen(
    viewModel: TvCompanionViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentPage by viewModel.pdfCurrentPage.collectAsStateWithLifecycle()
    val totalPages by viewModel.pdfTotalPages.collectAsStateWithLifecycle()
    val docTitle by viewModel.pdfDocTitle.collectAsStateWithLifecycle()
    val cursorPosition by viewModel.virtualCursorPosition.collectAsStateWithLifecycle()
    val isCursorVisible by viewModel.isCursorVisible.collectAsStateWithLifecycle()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(TvObsidianBackground)
            .testTag("tv_pdf_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
        ) {
            // TV Header
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, Color(0xFFFF9800).copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
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
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(Icons.Default.ArrowBack, "Back", tint = Color.White)
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Icon(
                            imageVector = Icons.Default.PictureAsPdf,
                            contentDescription = "PDF",
                            tint = Color(0xFFFF9800),
                            modifier = Modifier.size(24.dp)
                        )

                        Spacer(modifier = Modifier.width(10.dp))

                        Text(
                            text = docTitle,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFFFF9800))
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "SLIDE $currentPage / $totalPages",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Main High-Res Slide Projector Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFFF8FAFC))
                    .border(2.dp, TvBorderDark, RoundedCornerShape(20.dp)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(40.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Slide Top Banner
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = docTitle.substringBefore(".pdf"),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = "CONFIDENTIAL • EXECUTIVE BRIEFING",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF64748B)
                        )
                    }

                    // Slide High-Definition Chart & Body
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(vertical = 24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Canvas(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                        ) {
                            val w = size.width
                            val h = size.height

                            drawRoundRect(
                                color = Color(0xFFE2E8F0),
                                topLeft = Offset(40f, 10f),
                                size = Size(w - 80f, h - 20f),
                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(20f, 20f)
                            )

                            val barWidth = (w - 180f) / 7f
                            for (i in 0 until 6) {
                                val barHeight = ((i * 3 + currentPage * 2) % 6 + 1) * 26f
                                drawRect(
                                    color = if (i == (currentPage % 6)) Color(0xFFE50914) else Color(0xFF0284C7),
                                    topLeft = Offset(80f + i * (barWidth + 16f), h - 40f - barHeight),
                                    size = Size(barWidth, barHeight)
                                )
                            }
                        }

                        Text(
                            text = "Chapter $currentPage: System Interoperability & High Performance Wi-Fi Streaming",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E293B),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 18.dp)
                        )

                        Text(
                            text = "Live page synchronization streamed directly from connected mobile device\nZero-latency remote slide controller with real-time feedback",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color(0xFF475569),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }

                    // Slide Bottom Info
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Android TV Companion Suite • High-Speed Transfer Hub",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF94A3B8)
                        )
                        Text(
                            text = "Slide $currentPage of $totalPages",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF334155)
                        )
                    }
                }
            }
        }

        // Virtual Laser Pointer / Cursor on TV screen
        if (isCursorVisible) {
            Box(
                modifier = Modifier
                    .offset { IntOffset(cursorPosition.first.toInt(), cursorPosition.second.toInt()) }
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFF0055).copy(alpha = 0.9f))
                    .border(2.dp, Color.White, CircleShape)
            )
        }
    }
}
