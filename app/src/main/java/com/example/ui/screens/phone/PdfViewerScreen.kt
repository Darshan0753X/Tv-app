package com.example.ui.screens.phone

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.CastConnected
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.FirstPage
import androidx.compose.material.icons.filled.LastPage
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.PresentToAll
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
fun PdfViewerScreen(
    viewModel: TvCompanionViewModel,
    modifier: Modifier = Modifier
) {
    val currentPage by viewModel.pdfCurrentPage.collectAsStateWithLifecycle()
    val totalPages by viewModel.pdfTotalPages.collectAsStateWithLifecycle()
    val docTitle by viewModel.pdfDocTitle.collectAsStateWithLifecycle()
    val connectedDevice by viewModel.connectedDevice.collectAsStateWithLifecycle()

    var zoomLevel by remember { mutableFloatStateOf(1.0f) }

    val sampleDocuments = remember {
        listOf(
            Pair("Android TV Architecture & Setup Guide.pdf", 12),
            Pair("Set-Top Box 4K Streaming Specifications.pdf", 8),
            Pair("Q3 Product Roadmap & Feature Presentation.pdf", 16),
            Pair("Smart Home IoT Interoperability Standard.pdf", 24)
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(TvObsidianBackground)
            .padding(bottom = 90.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Document Selector & TV Presenter Bar
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .clip(RoundedCornerShape(20.dp))
                .border(1.dp, Color(0xFFFF9800).copy(alpha = 0.5f), RoundedCornerShape(20.dp)),
            color = TvSurfaceDark
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFF9800).copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PictureAsPdf,
                                contentDescription = "PDF",
                                tint = Color(0xFFFF9800),
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Column(modifier = Modifier.padding(start = 10.dp)) {
                            Text(
                                text = docTitle,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Page $currentPage of $totalPages • Presenter Mode",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF94A3B8)
                            )
                        }
                    }

                    // Cast to TV button
                    Button(
                        onClick = { viewModel.pushPdfToTv(docTitle, totalPages) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("btn_cast_pdf_to_tv")
                    ) {
                        Icon(Icons.Default.Tv, "Cast", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Cast to TV", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Live PDF Page Slide Canvas (High Fidelity Slide Visualizer)
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFFF8FAFC))
                .border(2.dp, TvBorderDark, RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            PdfPageCanvas(
                currentPage = currentPage,
                totalPages = totalPages,
                docTitle = docTitle,
                zoom = zoomLevel
            )
        }

        // Presenter Slide Controller Bar
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
                .clip(RoundedCornerShape(20.dp))
                .border(1.dp, TvBorderDark, RoundedCornerShape(20.dp)),
            color = TvSurfaceDark
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // First Page
                IconButton(
                    onClick = { viewModel.setPdfPage(1) },
                    enabled = currentPage > 1,
                    modifier = Modifier.testTag("btn_pdf_first")
                ) {
                    Icon(Icons.Default.FirstPage, "First Page", tint = if (currentPage > 1) Color.White else Color.Gray)
                }

                // Prev Page
                IconButton(
                    onClick = { viewModel.setPdfPage(currentPage - 1) },
                    enabled = currentPage > 1,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(TvSurfaceVariant)
                        .testTag("btn_pdf_prev")
                ) {
                    Icon(Icons.Default.ChevronLeft, "Previous Page", tint = if (currentPage > 1) Color.White else Color.Gray)
                }

                // Current Page Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF1E2435))
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "Page $currentPage / $totalPages",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFF9800)
                    )
                }

                // Next Page
                IconButton(
                    onClick = { viewModel.setPdfPage(currentPage + 1) },
                    enabled = currentPage < totalPages,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(TvSurfaceVariant)
                        .testTag("btn_pdf_next")
                ) {
                    Icon(Icons.Default.ChevronRight, "Next Page", tint = if (currentPage < totalPages) Color.White else Color.Gray)
                }

                // Last Page
                IconButton(
                    onClick = { viewModel.setPdfPage(totalPages) },
                    enabled = currentPage < totalPages,
                    modifier = Modifier.testTag("btn_pdf_last")
                ) {
                    Icon(Icons.Default.LastPage, "Last Page", tint = if (currentPage < totalPages) Color.White else Color.Gray)
                }
            }
        }
    }
}

@Composable
private fun PdfPageCanvas(
    currentPage: Int,
    totalPages: Int,
    docTitle: String,
    zoom: Float
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Slide Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = docTitle.substringBefore(".pdf"),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFFFF9800))
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "SLIDE $currentPage",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

        // Slide Content Simulation / Graphic Chart
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(vertical = 12.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
            ) {
                // Draw mock slide architecture diagrams & graphs
                val w = size.width
                val h = size.height

                // Draw slide layout blocks
                drawRoundRect(
                    color = Color(0xFFE2E8F0),
                    topLeft = Offset(20f, 10f),
                    size = Size(w - 40f, h - 20f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(16f, 16f)
                )

                // Draw slide chart bars or diagram nodes
                val barWidth = (w - 100f) / 6f
                for (i in 0 until 5) {
                    val barHeight = ((i + currentPage) % 5 + 1) * 18f
                    drawRect(
                        color = if (i == (currentPage % 5)) Color(0xFFE50914) else Color(0xFF0284C7),
                        topLeft = Offset(40f + i * (barWidth + 12f), h - 30f - barHeight),
                        size = Size(barWidth, barHeight)
                    )
                }
            }

            Text(
                text = "Section $currentPage: Overview & Architecture Architecture Specifications",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1E293B),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 10.dp)
            )

            Text(
                text = "• Seamless real-time Wi-Fi communication protocol\n• 4K Video streaming buffer synchronization\n• Large file transfers (>1GB) with socket streaming",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF475569),
                modifier = Modifier.padding(top = 6.dp)
            )
        }

        // Slide Footer
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Confidential • Android TV Hub",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF94A3B8)
            )
            Text(
                text = "$currentPage / $totalPages",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF64748B)
            )
        }
    }
}
