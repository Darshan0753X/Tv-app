package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FolderShared
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.OndemandVideo
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.SmartDisplay
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppShortcut
import com.example.data.model.CommandType
import com.example.data.model.RemoteCommand
import com.example.ui.theme.TvAccentAmber
import com.example.ui.theme.TvAccentCyan
import com.example.ui.theme.TvAccentGreen
import com.example.ui.theme.TvAccentPurple
import com.example.ui.theme.TvBorderDark
import com.example.ui.theme.TvCrimsonPrimary
import com.example.ui.theme.TvSurfaceDark
import com.example.ui.theme.TvSurfaceVariant

@Composable
fun QuickAppLauncherRow(
    apps: List<AppShortcut>,
    onLaunchApp: (AppShortcut) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "QUICK LAUNCH ON TV",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF94A3B8),
            letterSpacing = 1.2.sp,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
        )

        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(apps, key = { it.id }) { app ->
                AppShortcutCard(app = app, onClick = { onLaunchApp(app) })
            }
        }
    }
}

@Composable
fun AppShortcutCard(
    app: AppShortcut,
    onClick: () -> Unit
) {
    val (icon, badgeColor) = when (app.iconName) {
        "youtube" -> Pair(Icons.Default.SmartDisplay, TvCrimsonPrimary)
        "netflix" -> Pair(Icons.Default.Movie, TvCrimsonPrimary)
        "prime" -> Pair(Icons.Default.OndemandVideo, TvAccentCyan)
        "browser" -> Pair(Icons.Default.Language, TvAccentCyan)
        "pdf" -> Pair(Icons.Default.PictureAsPdf, TvAccentAmber)
        "files" -> Pair(Icons.Default.FolderShared, TvAccentGreen)
        "music" -> Pair(Icons.Default.MusicNote, TvAccentPurple)
        else -> Pair(Icons.Default.PhotoLibrary, Color(0xFF38BDF8))
    }

    Column(
        modifier = Modifier
            .width(74.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(TvSurfaceDark)
            .border(1.dp, TvBorderDark, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 4.dp)
            .testTag("quick_app_${app.id}"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(badgeColor.copy(alpha = 0.15f))
                .border(1.dp, badgeColor.copy(alpha = 0.5f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = app.name,
                tint = badgeColor,
                modifier = Modifier.size(22.dp)
            )
        }

        Text(
            text = app.name,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = Color(0xFFE2E8F0),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 6.dp)
        )
    }
}
