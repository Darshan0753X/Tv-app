package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CommandType
import com.example.data.model.RemoteCommand
import com.example.ui.theme.TvAccentCyan
import com.example.ui.theme.TvBorderDark
import com.example.ui.theme.TvCrimsonPrimary
import com.example.ui.theme.TvSurfaceDark
import com.example.ui.theme.TvSurfaceVariant

@Composable
fun VolumeControlHud(
    volume: Int,
    isMuted: Boolean,
    onCommand: (RemoteCommand) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .border(1.dp, TvBorderDark, RoundedCornerShape(20.dp)),
        color = TvSurfaceDark
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Mute button
            IconButton(
                onClick = { onCommand(RemoteCommand(CommandType.MUTE)) },
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(if (isMuted) TvCrimsonPrimary.copy(alpha = 0.2f) else TvSurfaceVariant)
                    .testTag("btn_mute")
            ) {
                Icon(
                    imageVector = if (isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeMute,
                    contentDescription = "Mute Volume",
                    tint = if (isMuted) TvCrimsonPrimary else Color(0xFFCBD5E1)
                )
            }

            // Slider
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "TV VOLUME",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF94A3B8),
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = if (isMuted) "MUTED" else "$volume%",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isMuted) TvCrimsonPrimary else TvAccentCyan
                    )
                }

                Slider(
                    value = if (isMuted) 0f else volume.toFloat(),
                    onValueChange = { newVol ->
                        onCommand(RemoteCommand(CommandType.SET_VOLUME, intValue = newVol.toInt()))
                    },
                    valueRange = 0f..100f,
                    colors = SliderDefaults.colors(
                        thumbColor = TvCrimsonPrimary,
                        activeTrackColor = TvCrimsonPrimary,
                        inactiveTrackColor = TvSurfaceVariant
                    ),
                    modifier = Modifier
                        .height(32.dp)
                        .testTag("slider_volume")
                )
            }

            // Vol Up
            IconButton(
                onClick = { onCommand(RemoteCommand(CommandType.VOLUME_UP)) },
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(TvSurfaceVariant)
                    .testTag("btn_vol_up")
            ) {
                Icon(
                    imageVector = Icons.Default.VolumeUp,
                    contentDescription = "Volume Up",
                    tint = Color.White
                )
            }
        }
    }
}
