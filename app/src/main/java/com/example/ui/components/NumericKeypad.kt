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
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.KeyboardReturn
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
fun NumericKeypad(
    onCommand: (RemoteCommand) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .clip(RoundedCornerShape(20.dp))
            .border(1.dp, TvBorderDark, RoundedCornerShape(20.dp)),
        color = TvSurfaceDark
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val rows = listOf(
                listOf("1", "2", "3"),
                listOf("4", "5", "6"),
                listOf("7", "8", "9"),
                listOf("CH-", "0", "CH+")
            )

            rows.forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    row.forEach { key ->
                        KeypadButton(
                            text = key,
                            onClick = {
                                when (key) {
                                    "0" -> onCommand(RemoteCommand(CommandType.NUM_0, intValue = 0))
                                    "1" -> onCommand(RemoteCommand(CommandType.NUM_1, intValue = 1))
                                    "2" -> onCommand(RemoteCommand(CommandType.NUM_2, intValue = 2))
                                    "3" -> onCommand(RemoteCommand(CommandType.NUM_3, intValue = 3))
                                    "4" -> onCommand(RemoteCommand(CommandType.NUM_4, intValue = 4))
                                    "5" -> onCommand(RemoteCommand(CommandType.NUM_5, intValue = 5))
                                    "6" -> onCommand(RemoteCommand(CommandType.NUM_6, intValue = 6))
                                    "7" -> onCommand(RemoteCommand(CommandType.NUM_7, intValue = 7))
                                    "8" -> onCommand(RemoteCommand(CommandType.NUM_8, intValue = 8))
                                    "9" -> onCommand(RemoteCommand(CommandType.NUM_9, intValue = 9))
                                    "CH+" -> onCommand(RemoteCommand(CommandType.CHANNEL_UP))
                                    "CH-" -> onCommand(RemoteCommand(CommandType.CHANNEL_DOWN))
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun KeypadButton(
    text: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(62.dp)
            .clip(CircleShape)
            .background(TvSurfaceVariant)
            .border(1.dp, TvBorderDark, CircleShape)
            .clickable(onClick = onClick)
            .testTag("keypad_$text"),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = if (text.startsWith("CH")) TvAccentCyan else Color.White
        )
    }
}
