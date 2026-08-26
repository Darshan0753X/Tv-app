package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CastConnected
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
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
import com.example.data.model.TvDevice
import com.example.ui.theme.TvAccentCyan
import com.example.ui.theme.TvAccentGreen
import com.example.ui.theme.TvBorderDark
import com.example.ui.theme.TvCrimsonPrimary
import com.example.ui.theme.TvSurfaceDark

@Composable
fun DeviceStatusBadge(
    connectedDevice: TvDevice?,
    wifiSsid: String,
    onSwitchDeviceClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, TvBorderDark, RoundedCornerShape(16.dp))
            .clickable(onClick = onSwitchDeviceClick)
            .testTag("device_status_badge"),
        color = TvSurfaceDark
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(if (connectedDevice != null) TvAccentGreen.copy(alpha = 0.15f) else TvCrimsonPrimary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (connectedDevice != null) Icons.Default.Tv else Icons.Default.CastConnected,
                        contentDescription = "Device Status",
                        tint = if (connectedDevice != null) TvAccentGreen else TvCrimsonPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column(modifier = Modifier.padding(start = 10.dp)) {
                    Text(
                        text = connectedDevice?.name ?: "No TV Connected",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(if (connectedDevice != null) TvAccentGreen else Color.Gray)
                        )
                        Text(
                            text = if (connectedDevice != null) " ${connectedDevice.ipAddress} • $wifiSsid" else " Tap to scan on $wifiSsid",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF94A3B8),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF1E2435))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Change",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = TvAccentCyan
                    )
                    Icon(
                        imageVector = Icons.Default.ExpandMore,
                        contentDescription = "Switch",
                        tint = TvAccentCyan,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
