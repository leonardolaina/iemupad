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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.IosBlue
import com.example.ui.theme.IosMint
import com.example.ui.theme.SurfaceBorder

@Composable
fun IosStatusBar(
    modifier: Modifier = Modifier
) {
    Surface(
        color = Color.Black,
        modifier = modifier
            .fillMaxWidth()
            .height(26.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "iEmu 3G",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
                Icon(
                    imageVector = Icons.Default.Wifi,
                    contentDescription = "Wi-Fi",
                    tint = Color.White,
                    modifier = Modifier.size(13.dp)
                )
            }

            Text(
                text = "9:41 AM",
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "100%",
                    color = Color.White,
                    fontSize = 11.sp
                )
                Box(
                    modifier = Modifier
                        .size(width = 18.dp, height = 9.dp)
                        .border(1.dp, Color.White, RoundedCornerShape(2.dp))
                        .padding(1.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.9f)
                            .height(7.dp)
                            .background(IosMint, RoundedCornerShape(1.dp))
                    )
                }
            }
        }
    }
}

@Composable
fun EmulatorToolBar(
    fps: Float,
    speedMultiplier: Float,
    isFpsOverlayVisible: Boolean,
    isVirtualGamepadVisible: Boolean,
    savedStatesCount: Int = 0,
    onSpeedCycle: () -> Unit,
    onSaveState: () -> Unit,
    onToggleFps: () -> Unit,
    onToggleGamepad: () -> Unit,
    onExitEmulator: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Color(0xFF0F172A).copy(alpha = 0.95f),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder),
        modifier = modifier.padding(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            IconButton(
                onClick = onExitEmulator,
                modifier = Modifier.size(36.dp).testTag("emulator_back_button")
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar", tint = Color.White)
            }

            // Speed multiplier
            Surface(
                color = if (speedMultiplier > 1f) IosBlue else Color(0xFF1E293B),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onSpeedCycle() }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(Icons.Default.FastForward, contentDescription = "Velocidade", tint = Color.White, modifier = Modifier.size(14.dp))
                    Text(
                        text = "${speedMultiplier.toInt()}x",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Save State with badge
            Box {
                IconButton(
                    onClick = onSaveState,
                    modifier = Modifier.size(36.dp).testTag("save_state_button")
                ) {
                    Icon(Icons.Default.Save, contentDescription = "Salvar Estado", tint = Color.White)
                }
                if (savedStatesCount > 0) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .size(16.dp)
                            .background(IosBlue, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "$savedStatesCount",
                            color = Color.White,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Virtual Gamepad toggle
            IconButton(
                onClick = onToggleGamepad,
                modifier = Modifier.size(36.dp).testTag("toggle_gamepad_button")
            ) {
                Icon(
                    Icons.Default.SportsEsports,
                    contentDescription = "Controles",
                    tint = if (isVirtualGamepadVisible) IosMint else Color(0xFF94A3B8)
                )
            }

            // FPS Overlay toggle
            IconButton(
                onClick = onToggleFps,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    Icons.Default.Info,
                    contentDescription = "Telemetry",
                    tint = if (isFpsOverlayVisible) IosBlue else Color(0xFF94A3B8)
                )
            }

            // FPS Counter badge
            Text(
                text = "${fps.toInt()} FPS",
                color = IosMint,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(horizontal = 6.dp)
            )
        }
    }
}

@Composable
fun ClassicHomeButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(58.dp)
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(Color(0xFF1C1C1E))
                .border(2.dp, Color(0xFF3A3A3C), CircleShape)
                .clickable { onClick() }
                .testTag("ios_home_button"),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .border(2.dp, Color(0xFF8E8E93), RoundedCornerShape(4.dp))
            )
        }
    }
}
