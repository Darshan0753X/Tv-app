package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.FolderShared
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.ScreenShare
import androidx.compose.material.icons.filled.SettingsRemote
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.network.NetworkUtils
import com.example.ui.components.DeviceStatusBadge
import com.example.ui.components.TransferProgressCard
import com.example.ui.screens.phone.CastScreen
import com.example.ui.screens.phone.DeviceDiscoveryScreen
import com.example.ui.screens.phone.FileShareScreen
import com.example.ui.screens.phone.PdfViewerScreen
import com.example.ui.screens.phone.RemoteScreen
import com.example.ui.screens.phone.WebBrowserScreen
import com.example.ui.screens.tv.TvBrowserScreen
import com.example.ui.screens.tv.TvCinemaPlayerScreen
import com.example.ui.screens.tv.TvDashboardScreen
import com.example.ui.screens.tv.TvFileReceiverScreen
import com.example.ui.screens.tv.TvPdfPresenterScreen
import com.example.ui.screens.tv.TvScreenMirrorScreen
import com.example.ui.theme.TvAccentCyan
import com.example.ui.theme.TvAccentGreen
import com.example.ui.theme.TvBorderDark
import com.example.ui.theme.TvCrimsonPrimary
import com.example.ui.theme.TvObsidianBackground
import com.example.ui.theme.TvSurfaceDark
import com.example.ui.theme.TvSurfaceVariant
import com.example.ui.viewmodel.TvCompanionViewModel
import com.example.ui.viewmodel.TvScreen

enum class PhoneNavTab(val title: String, val icon: ImageVector, val tag: String) {
    REMOTE("Remote", Icons.Default.SettingsRemote, "nav_remote"),
    CAST("Screen Cast", Icons.Default.ScreenShare, "nav_cast"),
    FILES("Files", Icons.Default.FolderShared, "nav_files"),
    BROWSER("Browser", Icons.Default.Language, "nav_browser"),
    PDF("PDF", Icons.Default.PictureAsPdf, "nav_pdf"),
    DEVICES("Wi-Fi TV", Icons.Default.Devices, "nav_devices")
}

enum class TvNavScreen {
    DASHBOARD,
    SCREEN_MIRROR,
    CINEMA_PLAYER,
    BROWSER,
    PDF_PRESENTER,
    FILE_RECEIVER
}

sealed interface AppNavTarget {
    data class Phone(val tab: PhoneNavTab) : AppNavTarget
    data class Tv(val screen: TvNavScreen) : AppNavTarget
}

@Composable
fun MainControlScreen(
    viewModel: TvCompanionViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isTvReceiverMode by viewModel.isTvReceiverMode.collectAsStateWithLifecycle()
    val connectedDevice by viewModel.connectedDevice.collectAsStateWithLifecycle()
    val activeTransfer by viewModel.activeFileTransfer.collectAsStateWithLifecycle()
    val wifiSsid = remember { NetworkUtils.getWifiSsid(context) }

    var selectedPhoneTab by remember { mutableStateOf(PhoneNavTab.REMOTE) }
    var selectedTvScreen by remember { mutableStateOf(TvNavScreen.DASHBOARD) }

    val vmTvScreen by viewModel.selectedTvScreen.collectAsStateWithLifecycle()
    LaunchedEffect(vmTvScreen) {
        selectedTvScreen = when (vmTvScreen) {
            TvScreen.DASHBOARD -> TvNavScreen.DASHBOARD
            TvScreen.SCREEN_MIRROR -> TvNavScreen.SCREEN_MIRROR
            TvScreen.CINEMA_PLAYER -> TvNavScreen.CINEMA_PLAYER
            TvScreen.TV_BROWSER -> TvNavScreen.BROWSER
            TvScreen.PDF_PRESENTER -> TvNavScreen.PDF_PRESENTER
            TvScreen.FILE_HUB -> TvNavScreen.FILE_RECEIVER
        }
    }

    val currentTarget: AppNavTarget = if (isTvReceiverMode) {
        AppNavTarget.Tv(selectedTvScreen)
    } else {
        AppNavTarget.Phone(selectedPhoneTab)
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = TvObsidianBackground,
        topBar = {
            // Main Top Bar with Mode Switch (Phone App vs TV Receiver Mode)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .border(1.dp, TvBorderDark, RoundedCornerShape(18.dp)),
                color = TvSurfaceDark
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Title and connection info
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(TvCrimsonPrimary.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isTvReceiverMode) Icons.Default.Tv else Icons.Default.PhoneAndroid,
                                contentDescription = "App Mode",
                                tint = TvCrimsonPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Column(modifier = Modifier.padding(start = 10.dp)) {
                            Text(
                                text = if (isTvReceiverMode) "TV Receiver Hub" else "TV Remote & Cast",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = if (isTvReceiverMode) "Port 8080 • Standby" else (connectedDevice?.name ?: "Wi-Fi: $wifiSsid"),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (connectedDevice != null) TvAccentGreen else Color(0xFF94A3B8)
                            )
                        }
                    }

                    // Mode Switch Toggle (Phone Controller vs TV Set-Top Box Mode)
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(TvSurfaceVariant)
                            .padding(3.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (!isTvReceiverMode) TvCrimsonPrimary else Color.Transparent)
                                .clickable {
                                    if (isTvReceiverMode) viewModel.toggleAppMode(false)
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                                .testTag("btn_mode_phone"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Phone",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (!isTvReceiverMode) Color.White else Color(0xFF94A3B8)
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (isTvReceiverMode) TvAccentCyan else Color.Transparent)
                                .clickable {
                                    if (!isTvReceiverMode) viewModel.toggleAppMode(true)
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                                .testTag("btn_mode_tv"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Android TV",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isTvReceiverMode) Color(0xFF0F172A) else Color(0xFF94A3B8)
                            )
                        }
                    }
                }
            }
        },
        bottomBar = {
            if (!isTvReceiverMode) {
                NavigationBar(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .border(1.dp, TvBorderDark, RoundedCornerShape(24.dp)),
                    containerColor = TvSurfaceDark,
                    tonalElevation = 8.dp
                ) {
                    PhoneNavTab.values().forEach { tab ->
                        val isSelected = selectedPhoneTab == tab
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = { selectedPhoneTab = tab },
                            icon = {
                                Icon(
                                    imageVector = tab.icon,
                                    contentDescription = tab.title,
                                    tint = if (isSelected) TvCrimsonPrimary else Color(0xFF94A3B8),
                                    modifier = Modifier.size(22.dp)
                                )
                            },
                            label = {
                                Text(
                                    text = tab.title,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) Color.White else Color(0xFF94A3B8)
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = TvCrimsonPrimary.copy(alpha = 0.15f)
                            ),
                            modifier = Modifier.testTag(tab.tag)
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            AnimatedContent(
                targetState = currentTarget,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "screen_transition"
            ) { target ->
                when (target) {
                    is AppNavTarget.Tv -> {
                        // TV Set-Top Box Receiver Interface
                        when (target.screen) {
                            TvNavScreen.DASHBOARD -> TvDashboardScreen(
                                viewModel = viewModel,
                                onNavigateToScreenMirror = { selectedTvScreen = TvNavScreen.SCREEN_MIRROR },
                                onNavigateToPlayer = { selectedTvScreen = TvNavScreen.CINEMA_PLAYER },
                                onNavigateToBrowser = { selectedTvScreen = TvNavScreen.BROWSER },
                                onNavigateToPdf = { selectedTvScreen = TvNavScreen.PDF_PRESENTER },
                                onNavigateToFiles = { selectedTvScreen = TvNavScreen.FILE_RECEIVER }
                            )
                            TvNavScreen.SCREEN_MIRROR -> TvScreenMirrorScreen(
                                viewModel = viewModel,
                                onBack = { selectedTvScreen = TvNavScreen.DASHBOARD }
                            )
                            TvNavScreen.CINEMA_PLAYER -> TvCinemaPlayerScreen(
                                viewModel = viewModel,
                                onBack = { selectedTvScreen = TvNavScreen.DASHBOARD }
                            )
                            TvNavScreen.BROWSER -> TvBrowserScreen(
                                viewModel = viewModel,
                                onBack = { selectedTvScreen = TvNavScreen.DASHBOARD }
                            )
                            TvNavScreen.PDF_PRESENTER -> TvPdfPresenterScreen(
                                viewModel = viewModel,
                                onBack = { selectedTvScreen = TvNavScreen.DASHBOARD }
                            )
                            TvNavScreen.FILE_RECEIVER -> TvFileReceiverScreen(
                                viewModel = viewModel,
                                onPlayMedia = { selectedTvScreen = TvNavScreen.CINEMA_PLAYER },
                                onBack = { selectedTvScreen = TvNavScreen.DASHBOARD }
                            )
                        }
                    }
                    is AppNavTarget.Phone -> {
                        // Phone Companion Controller Interface
                        Column(modifier = Modifier.fillMaxSize()) {
                            // Connected Device quick status bar (if on Remote or Cast tab)
                            if (target.tab == PhoneNavTab.REMOTE || target.tab == PhoneNavTab.CAST) {
                                DeviceStatusBadge(
                                    connectedDevice = connectedDevice,
                                    wifiSsid = wifiSsid,
                                    onSwitchDeviceClick = { selectedPhoneTab = PhoneNavTab.DEVICES }
                                )
                            }

                            when (target.tab) {
                                PhoneNavTab.REMOTE -> RemoteScreen(viewModel = viewModel)
                                PhoneNavTab.CAST -> CastScreen(viewModel = viewModel)
                                PhoneNavTab.FILES -> FileShareScreen(viewModel = viewModel)
                                PhoneNavTab.BROWSER -> WebBrowserScreen(viewModel = viewModel)
                                PhoneNavTab.PDF -> PdfViewerScreen(viewModel = viewModel)
                                PhoneNavTab.DEVICES -> DeviceDiscoveryScreen(viewModel = viewModel)
                            }
                        }
                    }
                }
            }

            // Global file transfer progress (show over other tabs)
            if (!isTvReceiverMode && selectedPhoneTab != PhoneNavTab.FILES) {
                activeTransfer?.let { file ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .padding(16.dp)
                    ) {
                        TransferProgressCard(file = file)
                    }
                }
            }
        }
    }
}
