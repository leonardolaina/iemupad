package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sensor.MotionSensorService
import com.example.sensor.MotionTiltState
import com.example.ui.theme.DeepSlate
import com.example.ui.theme.IosBlue
import com.example.ui.theme.IosMint
import com.example.ui.theme.SurfaceBorder
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun SensorCalibrationDialog(
    motionState: MotionTiltState,
    onDismiss: () -> Unit
) {
    var sensitivity by remember { mutableFloatStateOf(motionState.sensitivity) }
    var deadzone by remember { mutableFloatStateOf(motionState.deadzone) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DeepSlate,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Sensors, contentDescription = null, tint = IosMint)
                Text("Sensor de Movimento & Inclinação", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Mapeamento em tempo real dos eixos X (rolagem/curva) e Y (arfagem/aceleração) via MotionSensorService do Android.",
                    fontSize = 12.sp,
                    color = TextSecondary,
                    lineHeight = 16.sp
                )

                // Spirit Bubble Level Visualizer
                Surface(
                    color = Color.Black,
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Canvas(modifier = Modifier.size(110.dp)) {
                            // Target rings
                            drawCircle(Color(0xFF334155), radius = size.minDimension / 2f, style = Stroke(width = 2f))
                            drawCircle(Color(0xFF1E293B), radius = size.minDimension / 3f, style = Stroke(width = 1.5f))
                            drawCircle(Color(0xFF0F172A), radius = size.minDimension / 6f, style = Stroke(width = 1.5f))

                            // Crosshair lines
                            drawLine(Color(0xFF334155), Offset(0f, size.height / 2f), Offset(size.width, size.height / 2f), strokeWidth = 1f)
                            drawLine(Color(0xFF334155), Offset(size.width / 2f, 0f), Offset(size.width / 2f, size.height), strokeWidth = 1f)

                            // Gyro Bubble (offset by tiltX and tiltY)
                            val maxRadius = size.minDimension / 2.3f
                            val bubbleX = size.width / 2f + (motionState.tiltX * maxRadius).coerceIn(-maxRadius, maxRadius)
                            val bubbleY = size.height / 2f + (-motionState.tiltY * maxRadius).coerceIn(-maxRadius, maxRadius)

                            // Bubble core
                            drawCircle(IosMint.copy(alpha = 0.85f), radius = 14f, center = Offset(bubbleX, bubbleY))
                            drawCircle(Color.White, radius = 5f, center = Offset(bubbleX - 3f, bubbleY - 3f))
                        }

                        // Status Badge
                        Surface(
                            color = Color(0x99000000),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 6.dp)
                        ) {
                            Text(
                                text = "X: %+.2f | Y: %+.2f (Roll: %.0f° Pitch: %.0f°)".format(
                                    motionState.tiltX, motionState.tiltY, motionState.rollDeg, motionState.pitchDeg
                                ),
                                color = IosMint,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                // Calibrate Zero Button
                Button(
                    onClick = {
                        MotionSensorService.calibrateRestingPosition()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = IosBlue),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                    Text(
                        text = if (motionState.isCalibrated) "Recalibrar Posição de Repouso" else "Calibrar Posição Atual como Zero (0°)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 6.dp)
                    )
                }

                // Sensitivity Slider
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Sensibilidade:", color = TextSecondary, fontSize = 12.sp)
                        Text("%.1fx".format(sensitivity), color = IosBlue, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = sensitivity,
                        onValueChange = {
                            sensitivity = it
                            MotionSensorService.currentSensitivity = it
                        },
                        valueRange = 0.5f..2.5f,
                        colors = SliderDefaults.colors(thumbColor = IosBlue, activeTrackColor = IosBlue, inactiveTrackColor = Color(0xFF334155))
                    )
                }

                // Deadzone Slider
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Zona Morta (Filtro de Tremor):", color = TextSecondary, fontSize = 12.sp)
                        Text("${(deadzone * 100).toInt()}%", color = Color(0xFF64D2FF), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = deadzone,
                        onValueChange = {
                            deadzone = it
                            MotionSensorService.currentDeadzone = it
                        },
                        valueRange = 0.01f..0.15f,
                        colors = SliderDefaults.colors(thumbColor = Color(0xFF64D2FF), activeTrackColor = Color(0xFF64D2FF), inactiveTrackColor = Color(0xFF334155))
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = IosMint),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Concluído", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }
    )
}
