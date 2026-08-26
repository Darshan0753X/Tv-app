package com.example.ui.screens.phone

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Mouse
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Undo
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.AppShortcut
import com.example.data.model.CommandType
import com.example.data.model.RemoteCommand
import com.example.ui.components.DpadController
import com.example.ui.components.NumericKeypad
import com.example.ui.components.QuickAppLauncherRow
import com.example.ui.components.TrackpadController
import com.example.ui.components.VolumeControlHud
import com.example.ui.theme.TvAccentCyan
import com.example.ui.theme.TvBorderDark
import com.example.ui.theme.TvCrimsonPrimary
import com.example.ui.theme.TvObsidianBackground
import com.example.ui.theme.TvSurfaceDark
import com.example.ui.theme.TvSurfaceHighlight
import com.example.ui.theme.TvSurfaceVariant
import com.example.ui.viewmodel.TvCompanionViewModel

@Composable
fun RemoteScreen(
    viewModel: TvCompanionViewModel,
    modifier: Modifier = Modifier
) {
    val playbackState by viewModel.playbackState.collectAsStateWithLifecycle()
    val isTouchpadMode by viewModel.isTouchpadMode.collectAsStateWithLifecycle()
    val isNumpadOpen by viewModel.isNumpadOpen.collectAsStateWithLifecycle()
    val keyboardInputText by viewModel.keyboardInputText.collectAsStateWithLifecycle()
    var isKeyboardOpen by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(TvObsidianBackground)
            .verticalScroll(scrollState)
            .padding(bottom = 90.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Quick App Launcher
        QuickAppLauncherRow(
            apps = viewModel.tvAppShortcuts,
            onLaunchApp = { app ->
                viewModel.sendRemoteCommand(
                    RemoteCommand(CommandType.APP_LAUNCH, textValue = app.id)
                )
            },
            modifier = Modifier.padding(top = 4.dp)
        )

        // Top Control Row: Power, Mode Switch (D-Pad / Trackpad), Numpad, Keyboard
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
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
                // Power
                IconButton(
                    onClick = { viewModel.sendRemoteCommand(RemoteCommand(CommandType.POWER)) },
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(TvCrimsonPrimary.copy(alpha = 0.2f))
                        .testTag("btn_power")
                ) {
                    Icon(
                        imageVector = Icons.Default.PowerSettingsNew,
                        contentDescription = "Power",
                        tint = TvCrimsonPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // D-Pad / Trackpad Toggle
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(24.dp))
                        .background(TvSurfaceVariant)
                        .padding(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (!isTouchpadMode) TvCrimsonPrimary else Color.Transparent)
                            .clickable { if (isTouchpadMode) viewModel.toggleTouchpadMode() }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                            .testTag("toggle_dpad"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "D-Pad",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (!isTouchpadMode) Color.White else Color(0xFF94A3B8)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isTouchpadMode) TvAccentCyan else Color.Transparent)
                            .clickable { if (!isTouchpadMode) viewModel.toggleTouchpadMode() }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                            .testTag("toggle_trackpad"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Trackpad",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isTouchpadMode) Color(0xFF0F172A) else Color(0xFF94A3B8)
                        )
                    }
                }

                // Numpad Toggle
                IconButton(
                    onClick = { viewModel.toggleNumpad() },
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(if (isNumpadOpen) TvAccentCyan.copy(alpha = 0.2f) else TvSurfaceVariant)
                        .testTag("btn_numpad_toggle")
                ) {
                    Icon(
                        imageVector = Icons.Default.Dialpad,
                        contentDescription = "Numeric Keypad",
                        tint = if (isNumpadOpen) TvAccentCyan else Color(0xFFCBD5E1)
                    )
                }

                // Keyboard Text Input Toggle
                IconButton(
                    onClick = { isKeyboardOpen = !isKeyboardOpen },
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(if (isKeyboardOpen) TvAccentCyan.copy(alpha = 0.2f) else TvSurfaceVariant)
                        .testTag("btn_keyboard_toggle")
                ) {
                    Icon(
                        imageVector = Icons.Default.Keyboard,
                        contentDescription = "Keyboard Input",
                        tint = if (isKeyboardOpen) TvAccentCyan else Color(0xFFCBD5E1)
                    )
                }
            }
        }

        // Send Text Input Bar (Send text directly to TV search/input)
        AnimatedVisibility(visible = isKeyboardOpen) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, TvAccentCyan.copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
                color = TvSurfaceDark
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = keyboardInputText,
                        onValueChange = { viewModel.setKeyboardInputText(it) },
                        placeholder = { Text("Type text to TV...", color = Color(0xFF64748B)) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("tv_text_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TvAccentCyan,
                            unfocusedBorderColor = TvBorderDark,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        singleLine = true
                    )
                    IconButton(
                        onClick = {
                            if (keyboardInputText.isNotBlank()) {
                                viewModel.sendRemoteCommand(
                                    RemoteCommand(CommandType.TEXT_INPUT, textValue = keyboardInputText)
                                )
                                viewModel.setKeyboardInputText("")
                            }
                        },
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(TvAccentCyan)
                            .testTag("btn_send_text")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = "Send Text",
                            tint = Color(0xFF0F172A)
                        )
                    }
                }
            }
        }

        // Numeric Keypad
        AnimatedVisibility(visible = isNumpadOpen) {
            NumericKeypad(
                onCommand = { cmd -> viewModel.sendRemoteCommand(cmd) },
                modifier = Modifier.padding(vertical = 6.dp)
            )
        }

        // Volume HUD
        VolumeControlHud(
            volume = playbackState.volume,
            isMuted = playbackState.isMuted,
            onCommand = { cmd -> viewModel.sendRemoteCommand(cmd) },
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
        )

        // Main Navigation Section (D-Pad or Touchpad)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            if (isTouchpadMode) {
                TrackpadController(
                    onCommand = { cmd -> viewModel.sendRemoteCommand(cmd) }
                )
            } else {
                DpadController(
                    onCommand = { cmd -> viewModel.sendRemoteCommand(cmd) }
                )
            }
        }

        // Core TV Action Buttons: Back, Home, Menu
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            NavActionButton(
                icon = Icons.Default.Undo,
                label = "BACK",
                onClick = { viewModel.sendRemoteCommand(RemoteCommand(CommandType.BACK)) },
                tag = "btn_nav_back"
            )

            NavActionButton(
                icon = Icons.Default.Home,
                label = "HOME",
                isPrimary = true,
                onClick = { viewModel.sendRemoteCommand(RemoteCommand(CommandType.HOME)) },
                tag = "btn_nav_home"
            )

            NavActionButton(
                icon = Icons.Default.Menu,
                label = "MENU",
                onClick = { viewModel.sendRemoteCommand(RemoteCommand(CommandType.MENU)) },
                tag = "btn_nav_menu"
            )
        }

        // Media Playback Bar
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .clip(RoundedCornerShape(20.dp))
                .border(1.dp, TvBorderDark, RoundedCornerShape(20.dp)),
            color = TvSurfaceDark
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { viewModel.sendRemoteCommand(RemoteCommand(CommandType.PREVIOUS)) },
                    modifier = Modifier.testTag("btn_media_prev")
                ) {
                    Icon(Icons.Default.SkipPrevious, "Previous", tint = Color(0xFFCBD5E1))
                }

                IconButton(
                    onClick = { viewModel.sendRemoteCommand(RemoteCommand(CommandType.REWIND)) },
                    modifier = Modifier.testTag("btn_media_rewind")
                ) {
                    Icon(Icons.Default.FastRewind, "Rewind", tint = Color(0xFFCBD5E1))
                }

                // Play / Pause Master Button
                IconButton(
                    onClick = { viewModel.sendRemoteCommand(RemoteCommand(CommandType.PLAY_PAUSE)) },
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(TvCrimsonPrimary)
                        .testTag("btn_media_play_pause")
                ) {
                    Icon(
                        imageVector = if (playbackState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Play/Pause",
                        tint = Color.White,
                        modifier = Modifier.size(30.dp)
                    )
                }

                IconButton(
                    onClick = { viewModel.sendRemoteCommand(RemoteCommand(CommandType.FAST_FORWARD)) },
                    modifier = Modifier.testTag("btn_media_ff")
                ) {
                    Icon(Icons.Default.FastForward, "Fast Forward", tint = Color(0xFFCBD5E1))
                }

                IconButton(
                    onClick = { viewModel.sendRemoteCommand(RemoteCommand(CommandType.NEXT)) },
                    modifier = Modifier.testTag("btn_media_next")
                ) {
                    Icon(Icons.Default.SkipNext, "Next", tint = Color(0xFFCBD5E1))
                }
            }
        }
    }
}

@Composable
private fun NavActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isPrimary: Boolean = false,
    onClick: () -> Unit,
    tag: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (isPrimary) TvCrimsonPrimary.copy(alpha = 0.15f) else TvSurfaceVariant)
            .border(1.dp, if (isPrimary) TvCrimsonPrimary else TvBorderDark, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 10.dp)
            .testTag(tag)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isPrimary) TvCrimsonPrimary else Color.White,
            modifier = Modifier.size(24.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = if (isPrimary) TvCrimsonPrimary else Color(0xFF94A3B8),
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}
