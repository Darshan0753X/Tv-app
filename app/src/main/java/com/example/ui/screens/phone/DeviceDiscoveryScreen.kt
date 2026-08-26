package com.example.ui.screens.phone

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.filled.CastConnected
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PermDeviceInformation
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.TvDevice
import com.example.network.NetworkUtils
import com.example.ui.theme.TvAccentCyan
import com.example.ui.theme.TvAccentGreen
import com.example.ui.theme.TvBorderDark
import com.example.ui.theme.TvCrimsonPrimary
import com.example.ui.theme.TvObsidianBackground
import com.example.ui.theme.TvSurfaceDark
import com.example.ui.theme.TvSurfaceVariant
import com.example.ui.viewmodel.TvCompanionViewModel

@Composable
fun DeviceDiscoveryScreen(
    viewModel: TvCompanionViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val discoveredDevices by viewModel.discoveryService.discoveredDevices.collectAsStateWithLifecycle()
    val isScanning by viewModel.discoveryService.isScanning.collectAsStateWithLifecycle()
    val connectedDevice by viewModel.connectedDevice.collectAsStateWithLifecycle()

    var manualIpInput by remember { mutableStateOf(NetworkUtils.getLocalIpAddress(context)) }
    var manualNameInput by remember { mutableStateOf("My Set-Top Box") }
    var showManualDialog by remember { mutableStateOf(false) }

    val wifiSsid = remember { NetworkUtils.getWifiSsid(context) }
    val phoneIp = remember { NetworkUtils.getLocalIpAddress(context) }

    val infiniteTransition = rememberInfiniteTransition(label = "radar")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(TvObsidianBackground)
            .padding(bottom = 90.dp),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Wi-Fi Subnet Status Banner
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .border(1.dp, TvBorderDark, RoundedCornerShape(22.dp)),
                color = TvSurfaceDark
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
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
                                    .background(TvAccentCyan.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Wifi,
                                    contentDescription = "Wi-Fi",
                                    tint = TvAccentCyan,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Column(modifier = Modifier.padding(start = 12.dp)) {
                                Text(
                                    text = "Wi-Fi: $wifiSsid",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "Phone IP: $phoneIp",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF94A3B8)
                                )
                            }
                        }

                        // Scan Button
                        Button(
                            onClick = { viewModel.triggerSubnetScan() },
                            enabled = !isScanning,
                            colors = ButtonDefaults.buttonColors(containerColor = TvCrimsonPrimary),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("btn_scan_wifi")
                        ) {
                            if (isScanning) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(Icons.Default.Refresh, "Scan", modifier = Modifier.size(16.dp))
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (isScanning) "Scanning..." else "Scan Wi-Fi", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Radar Visualizer during scan
        if (isScanning) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(TvSurfaceDark)
                        .border(1.dp, TvAccentCyan.copy(alpha = 0.3f), RoundedCornerShape(20.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(90.dp)
                            .scale(pulseScale)
                            .clip(CircleShape)
                            .background(TvAccentCyan.copy(alpha = 0.15f))
                            .border(1.dp, TvAccentCyan, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tv,
                            contentDescription = "Searching",
                            tint = TvAccentCyan,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }
            }
        }

        // Discovered Devices Section
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "DISCOVERED ANDROID TVS (${discoveredDevices.size})",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF94A3B8),
                    letterSpacing = 1.2.sp
                )

                Text(
                    text = "+ Manual IP",
                    style = MaterialTheme.typography.labelSmall,
                    color = TvAccentCyan,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clickable { showManualDialog = !showManualDialog }
                        .padding(4.dp)
                        .testTag("btn_toggle_manual_ip")
                )
            }
        }

        // Manual IP Connect Form
        if (showManualDialog) {
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .border(1.dp, TvAccentCyan, RoundedCornerShape(18.dp)),
                    color = TvSurfaceDark
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Connect by Set-Top Box IP",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        OutlinedTextField(
                            value = manualNameInput,
                            onValueChange = { manualNameInput = it },
                            label = { Text("Device Name") },
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = manualIpInput,
                            onValueChange = { manualIpInput = it },
                            label = { Text("IP Address (e.g. 192.168.1.105)") },
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            singleLine = true
                        )
                        Button(
                            onClick = {
                                if (manualIpInput.isNotBlank()) {
                                    viewModel.addManualTvDevice(manualIpInput.trim(), 8080, manualNameInput.trim())
                                    showManualDialog = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = TvAccentCyan),
                            modifier = Modifier.fillMaxWidth().padding(top = 10.dp).testTag("btn_manual_connect"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Connect & Pair", color = Color(0xFF0F172A), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Devices List
        if (discoveredDevices.isEmpty() && !isScanning) {
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp)),
                    color = TvSurfaceDark
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Info",
                            tint = Color(0xFF64748B),
                            modifier = Modifier.size(36.dp)
                        )
                        Text(
                            text = "No TV devices detected yet",
                            style = MaterialTheme.typography.titleSmall,
                            color = Color.White,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                        Text(
                            text = "Make sure your Android TV / Set-Top Box is connected to the same Wi-Fi network ($wifiSsid)",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF94A3B8),
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        }

        items(discoveredDevices, key = { it.id }) { device ->
            val isConnected = connectedDevice?.id == device.id

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .border(
                        1.dp,
                        if (isConnected) TvAccentGreen else TvBorderDark,
                        RoundedCornerShape(18.dp)
                    )
                    .clickable { viewModel.setConnectedDevice(device) }
                    .testTag("device_item_${device.id}"),
                color = TvSurfaceDark
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isConnected) TvAccentGreen.copy(alpha = 0.2f) else TvSurfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tv,
                                contentDescription = device.name,
                                tint = if (isConnected) TvAccentGreen else Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Column(modifier = Modifier.padding(start = 12.dp)) {
                            Text(
                                text = device.name,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "${device.ipAddress}:${device.port} • ${device.model}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF94A3B8)
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isConnected) TvAccentGreen else TvCrimsonPrimary)
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = if (isConnected) "CONNECTED" else "CONNECT",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isConnected) Color(0xFF0F172A) else Color.White
                        )
                    }
                }
            }
        }
    }
}
