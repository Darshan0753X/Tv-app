package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdsClick
import androidx.compose.material.icons.filled.Mouse
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
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
fun TrackpadController(
    onCommand: (RemoteCommand) -> Unit,
    modifier: Modifier = Modifier
) {
    var isDragging by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(230.dp)
                .shadow(12.dp, RoundedCornerShape(24.dp))
                .clip(RoundedCornerShape(24.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            TvSurfaceVariant,
                            TvSurfaceDark
                        )
                    )
                )
                .border(
                    width = if (isDragging) 2.dp else 1.dp,
                    color = if (isDragging) TvAccentCyan else TvBorderDark,
                    shape = RoundedCornerShape(24.dp)
                )
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = {
                            onCommand(RemoteCommand(CommandType.MOUSE_CLICK))
                        },
                        onDoubleTap = {
                            onCommand(RemoteCommand(CommandType.DPAD_CENTER))
                        }
                    )
                }
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { isDragging = true },
                        onDragEnd = { isDragging = false },
                        onDragCancel = { isDragging = false },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            onCommand(
                                RemoteCommand(
                                    commandType = CommandType.MOUSE_MOVE,
                                    floatX = dragAmount.x,
                                    floatY = dragAmount.y
                                )
                            )
                        }
                    )
                }
                .testTag("trackpad_surface"),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.TouchApp,
                    contentDescription = "Swipe Trackpad",
                    tint = if (isDragging) TvAccentCyan else Color(0xFF64748B),
                    modifier = Modifier.size(42.dp)
                )
                Text(
                    text = "Swipe to Move Cursor • Tap to Click",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isDragging) TvAccentCyan else Color(0xFF94A3B8),
                    modifier = Modifier.padding(top = 8.dp)
                )
                Text(
                    text = "Double-tap for OK / Enter",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF64748B)
                )
            }
        }
    }
}
