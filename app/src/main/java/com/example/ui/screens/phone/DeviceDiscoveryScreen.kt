package com.example.ui.screens.phone

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.DiscoveryProtocol
import com.example.data.model.TvDevice
import com.example.network.NetworkUtils
import com.example.network.discovery.NsdLogEntry
import com.example.network.discovery.NsdServiceFilter
import com.example.ui.theme.TvAccentCyan
import com.example.ui.theme.TvAccentGreen
import com.example.ui.theme.TvBorderDark
import com.example.ui.theme.TvCrimsonPrimary
import com.example.ui.theme.TvObsidianBackground
import com.example.ui.theme.TvSurfaceDark
import com.example.ui.theme.TvSurfaceVariant
import com.example.ui.viewmodel.TvCompanionViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DeviceDiscoveryScreen(
    viewModel: TvCompanionViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val discoveredDevices by viewModel.discoveryService.discoveredDevices.collectAsStateWithLifecycle()
    val isScanning by viewModel.discoveryService.isScanning.collectAsStateWithLifecycle()
    val isNsdScanning by viewModel.nsdService.isScanning.collectAsStateWithLifecycle()
    val connectedDevice by viewModel.connectedDevice.collectAsStateWithLifecycle()
    val activeFilter by viewModel.nsdService.activeFilter.collectAsStateWithLifecycle()
    val nsdLogs by viewModel.nsdService.eventLogs.collectAsStateWithLifecycle()
    val isBroadcasting by viewModel.nsdService.isBroadcasting.collectAsStateWithLifecycle()
    val broadcastName by viewModel.nsdService.broadcastServiceName.collectAsStateWithLifecycle()

    var manualIpInput by remember { mutableStateOf(NetworkUtils.getLocalIpAddress(context)) }
    var manualNameInput by remember { mutableStateOf("My Set-Top Box") }
    var showManualDialog by remember { mutableStateOf(false) }
    var pairingDevice by remember { mutableStateOf<TvDevice?>(null) }
    var enteredPin by remember { mutableStateOf("") }
    var showLogsTray by remember { mutableStateOf(false) }
    var expandedDeviceId by remember { mutableStateOf<String?>(null) }

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

    // Filter devices list based on selected filter
    val filteredDevices = remember(discoveredDevices, activeFilter) {
        val list = when (activeFilter) {
            NsdServiceFilter.ALL -> discoveredDevices
            NsdServiceFilter.ANDROID_TV -> discoveredDevices.filter { it.serviceType.contains("androidtv") }
            NsdServiceFilter.GOOGLE_CAST -> discoveredDevices.filter { it.serviceType.contains("googlecast") }
            NsdServiceFilter.TV_HUB -> discoveredDevices.filter { it.serviceType.contains("tvhub") }
            NsdServiceFilter.AIRPLAY -> discoveredDevices.filter { it.serviceType.contains("airplay") }
        }
        list.distinctBy { it.id }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(TvObsidianBackground)
            .padding(bottom = 90.dp),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Wi-Fi Network & NSD Discovery Status Banner
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
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(TvAccentCyan.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Sensors,
                                    contentDescription = "mDNS Discovery",
                                    tint = TvAccentCyan,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Column(modifier = Modifier.padding(start = 12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "Wi-Fi: $wifiSsid",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(TvAccentCyan.copy(alpha = 0.2f))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "NsdManager",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = TvAccentCyan,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.sp
                                        )
                                    }
                                }
                                Text(
                                    text = "IP: $phoneIp • mDNS / ZeroConf active",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF94A3B8)
                                )
                            }
                        }

                        // Primary Scan / Stop Scan Button
                        val activeScan = isScanning || isNsdScanning
                        Button(
                            onClick = {
                                if (activeScan) {
                                    viewModel.stopNsdScan()
                                } else {
                                    viewModel.triggerSubnetScan()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (activeScan) Color(0xFFB91C1C) else TvCrimsonPrimary
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("btn_scan_wifi")
                        ) {
                            if (activeScan) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Stop Scan", fontWeight = FontWeight.Bold)
                            } else {
                                Icon(Icons.Default.Refresh, "Scan", modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Scan Wi-Fi", fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // TV Broadcasting Pill (if TV receiver mode active)
                    if (isBroadcasting) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(TvAccentGreen.copy(alpha = 0.15f))
                                .border(1.dp, TvAccentGreen.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(TvAccentGreen)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Broadcasting TV Receiver as '${broadcastName ?: "TV"}' via _tvhub._tcp. (Port 8080)",
                                style = MaterialTheme.typography.bodySmall,
                                color = TvAccentGreen,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }

        // Radar Visualizer during scan
        if (isScanning || isNsdScanning) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(TvSurfaceDark)
                        .border(1.dp, TvAccentCyan.copy(alpha = 0.3f), RoundedCornerShape(20.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .scale(pulseScale)
                            .clip(CircleShape)
                            .background(TvAccentCyan.copy(alpha = 0.15f))
                            .border(1.dp, TvAccentCyan, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tv,
                            contentDescription = "Searching for Android TVs",
                            tint = TvAccentCyan,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }
        }

        // NSD Service Filters Row
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "SERVICE PROTOCOL FILTER",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF94A3B8),
                    letterSpacing = 1.2.sp
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    NsdServiceFilter.values().forEach { filter ->
                        val isSelected = activeFilter == filter
                        val count = when (filter) {
                            NsdServiceFilter.ALL -> discoveredDevices.size
                            NsdServiceFilter.ANDROID_TV -> discoveredDevices.count { it.serviceType.contains("androidtv") }
                            NsdServiceFilter.GOOGLE_CAST -> discoveredDevices.count { it.serviceType.contains("googlecast") }
                            NsdServiceFilter.TV_HUB -> discoveredDevices.count { it.serviceType.contains("tvhub") }
                            NsdServiceFilter.AIRPLAY -> discoveredDevices.count { it.serviceType.contains("airplay") }
                        }

                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.setNsdFilter(filter) },
                            label = {
                                Text(
                                    text = "${filter.label} ($count)",
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 12.sp
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = TvAccentCyan,
                                selectedLabelColor = Color(0xFF0F172A),
                                containerColor = TvSurfaceDark,
                                labelColor = Color(0xFFCBD5E1)
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                borderColor = TvBorderDark,
                                selectedBorderColor = TvAccentCyan
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("filter_chip_${filter.name.lowercase()}")
                        )
                    }
                }
            }
        }

        // Action Toolbar (Demo Sample / Manual IP)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "DISCOVERED ANDROID TVS (${filteredDevices.size})",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF94A3B8),
                    letterSpacing = 1.2.sp
                )

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Demo TV injector
                    Text(
                        text = "+ Demo TV (NSD)",
                        style = MaterialTheme.typography.labelSmall,
                        color = TvAccentGreen,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clickable { viewModel.simulateNsdDevice(activeFilter) }
                            .padding(4.dp)
                            .testTag("btn_demo_tv")
                    )

                    // Manual IP toggle
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

        // Empty state
        if (filteredDevices.isEmpty() && !isScanning && !isNsdScanning) {
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
                            text = "No Android TV devices detected",
                            style = MaterialTheme.typography.titleSmall,
                            color = Color.White,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                        Text(
                            text = "Ensure your Android TV / Google TV is powered on and connected to $wifiSsid. Tap 'Scan Wi-Fi' or '+ Demo TV (NSD)' to test.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF94A3B8),
                            modifier = Modifier.padding(top = 4.dp),
                            lineHeight = 18.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = { viewModel.simulateNsdDevice(activeFilter) },
                            colors = ButtonDefaults.buttonColors(containerColor = TvSurfaceVariant),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Add Demo Android TV (NSD)", color = TvAccentCyan, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // Devices List
        items(filteredDevices, key = { it.id }) { device ->
            val isConnected = connectedDevice?.id == device.id
            val isExpanded = expandedDeviceId == device.id

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .border(
                        1.dp,
                        if (isConnected) TvAccentGreen else TvBorderDark,
                        RoundedCornerShape(18.dp)
                    )
                    .testTag("device_item_${device.id}"),
                color = TvSurfaceDark
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Device Icon & Info
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(
                                        when {
                                            isConnected -> TvAccentGreen.copy(alpha = 0.2f)
                                            device.serviceType.contains("googlecast") -> Color(0xFF8B5CF6).copy(alpha = 0.2f)
                                            device.serviceType.contains("androidtv") -> TvAccentCyan.copy(alpha = 0.2f)
                                            else -> TvSurfaceVariant
                                        }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (device.serviceType.contains("googlecast")) Icons.Default.Cast else Icons.Default.Tv,
                                    contentDescription = device.name,
                                    tint = when {
                                        isConnected -> TvAccentGreen
                                        device.serviceType.contains("googlecast") -> Color(0xFFC4B5FD)
                                        device.serviceType.contains("androidtv") -> TvAccentCyan
                                        else -> Color.White
                                    },
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            Column(modifier = Modifier.padding(start = 12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = device.name,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(top = 2.dp)
                                ) {
                                    Text(
                                        text = "${device.ipAddress}:${device.port}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF94A3B8)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))

                                    // Protocol Pill
                                    val (badgeText, badgeBg, badgeTextColor) = when {
                                        device.discoveryProtocol == DiscoveryProtocol.NSD && device.serviceType.contains("googlecast") ->
                                            Triple("NSD · Cast", Color(0xFF8B5CF6).copy(alpha = 0.25f), Color(0xFFDDD6FE))
                                        device.discoveryProtocol == DiscoveryProtocol.NSD && device.serviceType.contains("androidtv") ->
                                            Triple("NSD · Remote", TvAccentCyan.copy(alpha = 0.2f), TvAccentCyan)
                                        device.discoveryProtocol == DiscoveryProtocol.NSD && device.serviceType.contains("tvhub") ->
                                            Triple("NSD · TV Hub", TvAccentGreen.copy(alpha = 0.2f), TvAccentGreen)
                                        device.discoveryProtocol == DiscoveryProtocol.NSD ->
                                            Triple("NSD mDNS", TvAccentCyan.copy(alpha = 0.2f), TvAccentCyan)
                                        device.discoveryProtocol == DiscoveryProtocol.UDP_BEACON ->
                                            Triple("UDP Beacon", Color(0xFFF59E0B).copy(alpha = 0.2f), Color(0xFFFDE68A))
                                        device.discoveryProtocol == DiscoveryProtocol.SUBNET_PROBE ->
                                            Triple("Subnet Probe", Color(0xFF64748B).copy(alpha = 0.2f), Color(0xFFCBD5E1))
                                        else ->
                                            Triple("Manual", TvSurfaceVariant, Color.White)
                                    }

                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(badgeBg)
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = badgeText,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = badgeTextColor,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 9.sp
                                        )
                                    }
                                }

                                Text(
                                    text = device.model,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF64748B),
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        // Connect / Connected Action
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isConnected) TvAccentGreen else TvCrimsonPrimary)
                                    .clickable {
                                        if (isConnected) {
                                            viewModel.setConnectedDevice(null)
                                        } else {
                                            pairingDevice = device
                                            enteredPin = device.pin.ifBlank { "" }
                                        }
                                    }
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                                    .testTag("btn_connect_${device.id}")
                            ) {
                                Text(
                                    text = if (isConnected) "CONNECTED" else "PAIR & CONNECT",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isConnected) Color(0xFF0F172A) else Color.White
                                )
                            }

                            // Details expander
                            IconButton(
                                onClick = {
                                    expandedDeviceId = if (isExpanded) null else device.id
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = "Toggle Details",
                                    tint = Color(0xFF94A3B8)
                                )
                            }
                        }
                    }

                    // Expandable Service Details & TXT Records
                    AnimatedVisibility(
                        visible = isExpanded,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(TvSurfaceVariant.copy(alpha = 0.5f))
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "NsdManager Service Discovery Metadata",
                                style = MaterialTheme.typography.labelSmall,
                                color = TvAccentCyan,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Service Name: ${device.serviceName.ifEmpty { device.name }}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFCBD5E1),
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp
                            )
                            if (device.serviceType.isNotEmpty()) {
                                Text(
                                    text = "Service Type: ${device.serviceType}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFFCBD5E1),
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp
                                )
                            }
                            Text(
                                text = "Host: ${device.ipAddress} • Port: ${device.port}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFCBD5E1),
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp
                            )

                            if (device.attributes.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "DNS-SD TXT Records (${device.attributes.size}):",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF94A3B8),
                                    fontWeight = FontWeight.Bold
                                )
                                device.attributes.forEach { (key, value) ->
                                    Text(
                                        text = "• $key = $value",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF94A3B8),
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Live NsdManager Diagnostic & Event Logs Tray
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .border(1.dp, TvBorderDark, RoundedCornerShape(18.dp)),
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
                            modifier = Modifier
                                .clickable { showLogsTray = !showLogsTray }
                                .padding(vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Router,
                                contentDescription = "Log",
                                tint = TvAccentCyan,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "NsdManager Event Log",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(TvSurfaceVariant)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "${nsdLogs.size}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
                                    fontSize = 10.sp
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (nsdLogs.isNotEmpty()) {
                                IconButton(
                                    onClick = { viewModel.clearNsdLogs() },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DeleteSweep,
                                        contentDescription = "Clear Logs",
                                        tint = Color(0xFF94A3B8),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            IconButton(
                                onClick = { showLogsTray = !showLogsTray },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = if (showLogsTray) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = "Expand Log Tray",
                                    tint = Color(0xFF94A3B8)
                                )
                            }
                        }
                    }

                    AnimatedVisibility(
                        visible = showLogsTray,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (nsdLogs.isEmpty()) {
                                Text(
                                    text = "No mDNS discovery events recorded yet. Click 'Scan Wi-Fi' to start.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF64748B),
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            } else {
                                val timeFormat = remember { SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()) }
                                nsdLogs.take(15).forEach { log ->
                                    val (badgeBg, badgeColor) = when (log.eventType) {
                                        "SERVICE_RESOLVED" -> Pair(TvAccentGreen.copy(alpha = 0.2f), TvAccentGreen)
                                        "SERVICE_FOUND" -> Pair(TvAccentCyan.copy(alpha = 0.2f), TvAccentCyan)
                                        "RESOLVING" -> Pair(Color(0xFFF59E0B).copy(alpha = 0.2f), Color(0xFFFDE68A))
                                        "SERVICE_LOST" -> Pair(Color(0xFFEF4444).copy(alpha = 0.2f), Color(0xFFFCA5A5))
                                        "ERROR" -> Pair(Color(0xFFDC2626).copy(alpha = 0.2f), Color(0xFFFCA5A5))
                                        else -> Pair(TvSurfaceVariant, Color(0xFF94A3B8))
                                    }

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(TvSurfaceVariant.copy(alpha = 0.4f))
                                            .padding(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = timeFormat.format(Date(log.timestamp)),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontFamily = FontFamily.Monospace,
                                            color = Color(0xFF64748B),
                                            fontSize = 9.sp
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(badgeBg)
                                                .padding(horizontal = 4.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = log.eventType,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = badgeColor,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "${log.serviceName} • ${log.details}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color(0xFFCBD5E1),
                                            fontSize = 11.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // PIN Authentication Dialog
    if (pairingDevice != null) {
        val target = pairingDevice!!
        AlertDialog(
            onDismissRequest = { pairingDevice = null },
            title = {
                Text("Enter TV Pairing PIN", color = Color.White, fontWeight = FontWeight.Bold)
            },
            text = {
                Column {
                    Text(
                        text = "Enter the 4-digit PIN code displayed on ${target.name}:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFFCBD5E1)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = enteredPin,
                        onValueChange = { if (it.length <= 8) enteredPin = it },
                        label = { Text("4-Digit PIN (e.g. 1234)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("input_pairing_pin"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TvAccentCyan,
                            unfocusedBorderColor = TvBorderDark,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.pairDeviceWithPin(target, enteredPin.trim())
                        pairingDevice = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TvAccentCyan),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("btn_confirm_pair")
                ) {
                    Text("Pair & Connect", color = Color(0xFF0F172A), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { pairingDevice = null }) {
                    Text("Cancel", color = Color(0xFF94A3B8))
                }
            },
            containerColor = TvSurfaceDark,
            shape = RoundedCornerShape(20.dp)
        )
    }
}
