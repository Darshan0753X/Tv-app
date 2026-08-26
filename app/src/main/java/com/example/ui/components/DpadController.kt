package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.example.ui.theme.TvSurfaceHighlight
import com.example.ui.theme.TvSurfaceVariant

@Composable
fun DpadController(
    onCommand: (RemoteCommand) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(240.dp)
            .shadow(16.dp, CircleShape)
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        TvSurfaceHighlight,
                        TvSurfaceDark,
                        Color(0xFF090B10)
                    )
                )
            )
            .border(2.dp, Brush.sweepGradient(listOf(TvBorderDark, TvCrimsonPrimary.copy(alpha = 0.5f), TvBorderDark)), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        // UP Button
        DpadDirectionalButton(
            icon = Icons.Default.KeyboardArrowUp,
            contentDescription = "DPad Up",
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = 12.dp)
                .testTag("dpad_up"),
            onClick = { onCommand(RemoteCommand(CommandType.DPAD_UP)) }
        )

        // DOWN Button
        DpadDirectionalButton(
            icon = Icons.Default.KeyboardArrowDown,
            contentDescription = "DPad Down",
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset(y = (-12).dp)
                .testTag("dpad_down"),
            onClick = { onCommand(RemoteCommand(CommandType.DPAD_DOWN)) }
        )

        // LEFT Button
        DpadDirectionalButton(
            icon = Icons.Default.KeyboardArrowLeft,
            contentDescription = "DPad Left",
            modifier = Modifier
                .align(Alignment.CenterStart)
                .offset(x = 12.dp)
                .testTag("dpad_left"),
            onClick = { onCommand(RemoteCommand(CommandType.DPAD_LEFT)) }
        )

        // RIGHT Button
        DpadDirectionalButton(
            icon = Icons.Default.KeyboardArrowRight,
            contentDescription = "DPad Right",
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .offset(x = (-12).dp)
                .testTag("dpad_right"),
            onClick = { onCommand(RemoteCommand(CommandType.DPAD_RIGHT)) }
        )

        // CENTER / OK Button
        val centerInteraction = remember { MutableInteractionSource() }
        val isPressed by centerInteraction.collectIsPressedAsState()
        val scale by animateFloatAsState(targetValue = if (isPressed) 0.92f else 1.0f, label = "center_scale")

        Box(
            modifier = Modifier
                .size(86.dp)
                .scale(scale)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = if (isPressed) listOf(TvCrimsonPrimary, Color(0xFF8B0000))
                        else listOf(Color(0xFF2E1015), TvCrimsonPrimary.copy(alpha = 0.85f), Color(0xFF1E0A0D))
                    )
                )
                .border(2.dp, TvCrimsonPrimary, CircleShape)
                .clickable(
                    interactionSource = centerInteraction,
                    indication = null,
                    onClick = { onCommand(RemoteCommand(CommandType.DPAD_CENTER)) }
                )
                .testTag("dpad_center_ok"),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "OK",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}

@Composable
private fun DpadDirectionalButton(
    icon: ImageVector,
    contentDescription: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(targetValue = if (isPressed) 0.85f else 1.0f, label = "dir_scale")

    Box(
        modifier = modifier
            .size(54.dp)
            .scale(scale)
            .clip(CircleShape)
            .background(if (isPressed) TvCrimsonPrimary.copy(alpha = 0.4f) else Color.Transparent)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (isPressed) TvCrimsonPrimary else Color(0xFFE2E8F0),
            modifier = Modifier.size(36.dp)
        )
    }
}
