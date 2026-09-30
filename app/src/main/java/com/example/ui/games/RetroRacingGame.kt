package com.example.ui.games

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.SoundEffects
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.random.Random

data class TrafficCar(
    val id: Int,
    var lane: Float, // -1.0 (left) to 1.0 (right)
    var z: Float, // 0.0 (near player) to 1.0 (horizon)
    val color: Color,
    val speedFactor: Float = 0.65f
)

@Composable
fun RetroRacingGame(
    gameTitle: String,
    tiltX: Float,
    soundEffects: SoundEffects,
    fastForwardMultiplier: Float = 1f,
    onScoreUpdated: (Int) -> Unit = {}
) {
    var speedKmh by remember { mutableFloatStateOf(140f) }
    var nitroFuel by remember { mutableFloatStateOf(100f) }
    var isNitroActive by remember { mutableStateOf(false) }
    var playerTrackPos by remember { mutableFloatStateOf(0f) } // -1f to 1f
    var manualSteer by remember { mutableFloatStateOf(0f) }

    var distanceMeters by remember { mutableFloatStateOf(0f) }
    var positionRank by remember { mutableIntStateOf(6) }
    var carsOvertaken by remember { mutableIntStateOf(0) }
    var gameOver by remember { mutableStateOf(false) }

    var roadCurve by remember { mutableFloatStateOf(0f) }
    var curveTarget by remember { mutableFloatStateOf(0f) }
    var roadSegmentOffset by remember { mutableFloatStateOf(0f) }

    val opponentCars = remember { mutableStateListOf<TrafficCar>() }
    var carIdCounter by remember { mutableIntStateOf(1) }

    var canvasWidth by remember { mutableFloatStateOf(800f) }
    var canvasHeight by remember { mutableFloatStateOf(1200f) }

    // Init opponent racers
    LaunchedEffect(Unit) {
        if (opponentCars.isEmpty()) {
            opponentCars.add(TrafficCar(carIdCounter++, -0.5f, 0.4f, Color(0xFFFFCC00)))
            opponentCars.add(TrafficCar(carIdCounter++, 0.4f, 0.6f, Color(0xFFFF3B30)))
            opponentCars.add(TrafficCar(carIdCounter++, -0.2f, 0.8f, Color(0xFF34C759)))
            opponentCars.add(TrafficCar(carIdCounter++, 0.6f, 0.95f, Color(0xFFAF52DE)))
        }
    }

    // Racing Physics Loop
    LaunchedEffect(gameOver, fastForwardMultiplier) {
        if (gameOver) return@LaunchedEffect
        var tick = 0
        while (!gameOver) {
            val step = (25 / fastForwardMultiplier).toLong().coerceAtLeast(10)
            delay(step)
            tick++

            // Dynamic curve generation
            if (tick % 120 == 0) {
                curveTarget = (Random.nextFloat() - 0.5f) * 1.8f
            }
            roadCurve += (curveTarget - roadCurve) * 0.03f

            // Steering handling (real sensor tilt + manual buttons)
            val combinedSteer = (tiltX * 1.8f + manualSteer).coerceIn(-1.5f, 1.5f)
            playerTrackPos = (playerTrackPos + combinedSteer * 0.045f * (speedKmh / 150f)).coerceIn(-1.1f, 1.1f)

            // Speed calculation
            val targetBaseSpeed = if (isNitroActive && nitroFuel > 0f) 255f else 170f
            if (isNitroActive && nitroFuel > 0f) {
                nitroFuel = (nitroFuel - 1.2f).coerceAtLeast(0f)
                if (nitroFuel <= 0f) isNitroActive = false
            } else if (!isNitroActive && nitroFuel < 100f) {
                nitroFuel = (nitroFuel + 0.35f).coerceAtMost(100f)
            }

            speedKmh += (targetBaseSpeed - speedKmh) * 0.05f
            val deltaDist = (speedKmh / 3.6f) * 0.025f * fastForwardMultiplier
            distanceMeters += deltaDist
            onScoreUpdated(distanceMeters.toInt())

            roadSegmentOffset = (roadSegmentOffset + (speedKmh * 0.003f)) % 1f

            // Opponent traffic physics
            val toRespawn = mutableListOf<TrafficCar>()
            for (car in opponentCars) {
                // Closer to player as player moves faster
                val relSpeed = (speedKmh - 110f) * 0.00015f * fastForwardMultiplier
                car.z -= relSpeed

                // Passed player
                if (car.z < 0.02f) {
                    toRespawn.add(car)
                    carsOvertaken++
                    if (positionRank > 1) positionRank--
                    soundEffects.playCoin()
                    soundEffects.vibrate(25)
                }

                // Collision detection with player
                if (car.z in 0.05f..0.22f) {
                    val laneDiff = abs(car.lane - playerTrackPos)
                    if (laneDiff < 0.32f) {
                        // Crash impact!
                        speedKmh = 60f
                        isNitroActive = false
                        soundEffects.playBoom()
                        soundEffects.vibrate(100)
                    }
                }
            }

            // Respawn passed cars at horizon
            for (oldCar in toRespawn) {
                opponentCars.remove(oldCar)
                val newLane = (Random.nextFloat() - 0.5f) * 1.4f
                val newColor = listOf(Color(0xFFFF3B30), Color(0xFFFFCC00), Color(0xFF30D158), Color(0xFFBF5AF2), Color(0xFF007AFF)).random()
                opponentCars.add(TrafficCar(carIdCounter++, newLane, 1.0f, newColor))
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            canvasWidth = size.width
            canvasHeight = size.height

            val horizonY = size.height * 0.42f

            // Sky gradient (Sunset / Racing Twilight)
            drawRect(
                brush = Brush.verticalGradient(
                    listOf(Color(0xFF1E1B4B), Color(0xFF4C1D95), Color(0xFFEA580C)),
                    startY = 0f,
                    endY = horizonY
                ),
                topLeft = Offset(0f, 0f),
                size = Size(size.width, horizonY)
            )

            // Sun & Horizon Mountain silhouettes
            drawCircle(Color(0xFFFDE047), radius = 45f, center = Offset(size.width * 0.75f, horizonY - 35f))
            val mountainPath = Path().apply {
                moveTo(0f, horizonY)
                lineTo(size.width * 0.2f, horizonY - 45f)
                lineTo(size.width * 0.45f, horizonY - 20f)
                lineTo(size.width * 0.7f, horizonY - 60f)
                lineTo(size.width * 0.9f, horizonY - 30f)
                lineTo(size.width, horizonY)
                close()
            }
            drawPath(mountainPath, color = Color(0xFF2E1065))

            // Ground / Grass
            drawRect(
                color = Color(0xFF14532D),
                topLeft = Offset(0f, horizonY),
                size = Size(size.width, size.height - horizonY)
            )

            // 3D Perspective Road
            val bottomRoadWidth = size.width * 0.88f
            val topRoadWidth = size.width * 0.12f
            val bottomCenter = size.width * 0.5f
            val topCenter = size.width * 0.5f + (roadCurve * size.width * 0.25f)

            val roadPath = Path().apply {
                moveTo(topCenter - topRoadWidth / 2f, horizonY)
                lineTo(topCenter + topRoadWidth / 2f, horizonY)
                lineTo(bottomCenter + bottomRoadWidth / 2f, size.height)
                lineTo(bottomCenter - bottomRoadWidth / 2f, size.height)
                close()
            }
            drawPath(roadPath, color = Color(0xFF262626))

            // Road Edge Rumble Strips (Kerbs - Red/White stripes)
            val stripCount = 14
            for (i in 0 until stripCount) {
                val z1 = i.toFloat() / stripCount
                val z2 = (i + 1).toFloat() / stripCount
                val shiftedZ1 = (z1 + roadSegmentOffset) % 1f
                val isRed = (i % 2 == 0)
                val kerbColor = if (isRed) Color(0xFFDC2626) else Color(0xFFFAFAFA)

                val y1 = horizonY + (size.height - horizonY) * shiftedZ1
                val rW1 = topRoadWidth + (bottomRoadWidth - topRoadWidth) * shiftedZ1
                val cX1 = topCenter + (bottomCenter - topCenter) * shiftedZ1

                drawCircle(kerbColor, radius = 5f * (1f + shiftedZ1 * 2f), center = Offset(cX1 - rW1 / 2f - 4f, y1))
                drawCircle(kerbColor, radius = 5f * (1f + shiftedZ1 * 2f), center = Offset(cX1 + rW1 / 2f + 4f, y1))
            }

            // Road Center Lane Dashes
            for (i in 0 until 9) {
                val zPos = (i.toFloat() / 9f + roadSegmentOffset) % 1f
                val yDash = horizonY + (size.height - horizonY) * zPos
                val dashW = 4f + zPos * 8f
                val dashH = 10f + zPos * 30f
                val cX = topCenter + (bottomCenter - topCenter) * zPos
                drawRect(
                    color = Color(0xFFFACC15),
                    topLeft = Offset(cX - dashW / 2f, yDash),
                    size = Size(dashW, dashH)
                )
            }

            // Draw Opponent Cars (sorted by Z distance)
            for (car in opponentCars.sortedByDescending { it.z }) {
                val scale = 1f - car.z // 0.0 (far) to 1.0 (close)
                if (scale <= 0.05f) continue

                val carY = horizonY + (size.height - horizonY) * (1f - car.z)
                val rW = topRoadWidth + (bottomRoadWidth - topRoadWidth) * (1f - car.z)
                val cX = topCenter + (bottomCenter - topCenter) * (1f - car.z)
                val carX = cX + (car.lane * (rW / 2f) * 0.75f)

                val w = 55f * scale
                val h = 32f * scale
                // Shadow
                drawOval(Color.Black.copy(alpha = 0.5f), Offset(carX - w * 0.6f, carY + h * 0.4f), Size(w * 1.2f, h * 0.4f))
                // Body
                drawRoundRect(car.color, Offset(carX - w / 2f, carY - h / 2f), Size(w, h), CornerRadius(6f * scale, 6f * scale))
                // Roof / Window
                drawRoundRect(Color(0xFF1E293B), Offset(carX - w * 0.35f, carY - h * 0.4f), Size(w * 0.7f, h * 0.5f), CornerRadius(4f * scale, 4f * scale))
                // Tail lights
                drawCircle(Color.Red, radius = 3.5f * scale, center = Offset(carX - w * 0.38f, carY + h * 0.35f))
                drawCircle(Color.Red, radius = 3.5f * scale, center = Offset(carX + w * 0.38f, carY + h * 0.35f))
            }

            // Draw Player Supercar at bottom
            val playerY = size.height * 0.82f
            val playerX = bottomCenter + (playerTrackPos * (bottomRoadWidth / 2f) * 0.75f)
            val pW = 120f
            val pH = 68f

            // Player car shadow
            drawOval(Color.Black.copy(alpha = 0.6f), Offset(playerX - pW * 0.55f, playerY + pH * 0.4f), Size(pW * 1.1f, pH * 0.35f))

            // Nitro Boost Flame effect
            if (isNitroActive && nitroFuel > 0f) {
                val flameLength = 55f + Random.nextFloat() * 25f
                val flamePathLeft = Path().apply {
                    moveTo(playerX - 28f, playerY + pH * 0.45f)
                    lineTo(playerX - 22f, playerY + pH * 0.45f + flameLength)
                    lineTo(playerX - 16f, playerY + pH * 0.45f)
                    close()
                }
                val flamePathRight = Path().apply {
                    moveTo(playerX + 16f, playerY + pH * 0.45f)
                    lineTo(playerX + 22f, playerY + pH * 0.45f + flameLength)
                    lineTo(playerX + 28f, playerY + pH * 0.45f)
                    close()
                }
                drawPath(flamePathLeft, brush = Brush.verticalGradient(listOf(Color(0xFF38BDF8), Color(0xFF6366F1), Color.Transparent)))
                drawPath(flamePathRight, brush = Brush.verticalGradient(listOf(Color(0xFF38BDF8), Color(0xFF6366F1), Color.Transparent)))
            }

            // Chassis (Electric Racing Cyan / Metal)
            drawRoundRect(Color(0xFF0284C7), Offset(playerX - pW / 2f, playerY - pH / 2f), Size(pW, pH), CornerRadius(14f, 14f))
            drawRoundRect(Color(0xFF0369A1), Offset(playerX - pW * 0.42f, playerY - pH * 0.3f), Size(pW * 0.84f, pH * 0.7f), CornerRadius(10f, 10f))

            // Cockpit Windshield & Roof
            drawRoundRect(Color(0xFF0F172A), Offset(playerX - pW * 0.32f, playerY - pH * 0.4f), Size(pW * 0.64f, pH * 0.52f), CornerRadius(8f, 8f))

            // Glowing Tail Lights
            drawRoundRect(Color(0xFFFF2222), Offset(playerX - pW * 0.44f, playerY + pH * 0.32f), Size(24f, 10f), CornerRadius(3f, 3f))
            drawRoundRect(Color(0xFFFF2222), Offset(playerX + pW * 0.44f - 24f, playerY + pH * 0.32f), Size(24f, 10f), CornerRadius(3f, 3f))

            // Dual Exhausts
            drawCircle(Color(0xFF475569), radius = 6f, center = Offset(playerX - 22f, playerY + pH * 0.45f))
            drawCircle(Color(0xFF475569), radius = 6f, center = Offset(playerX + 22f, playerY + pH * 0.45f))
        }

        // Top Telemetry Dashboard HUD
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Speedometer & Position
            Surface(
                color = Color.Black.copy(alpha = 0.8f),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155))
            ) {
                Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) {
                    Text(gameTitle, color = Color(0xFF38BDF8), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = "${speedKmh.toInt()}",
                            color = if (isNitroActive) Color(0xFF38BDF8) else Color.White,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = " km/h",
                            color = Color(0xFF94A3B8),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }
                }
            }

            // Rank & Distance
            Surface(
                color = Color.Black.copy(alpha = 0.8f),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155))
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "POSIÇÃO ${positionRank}º / 8",
                        color = Color(0xFFFDE047),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = "Ultrapassagens: $carsOvertaken",
                        color = Color(0xFFA1A1AA),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // Steering Guidance Indicator
        Text(
            text = "Incline o celular (Giroscópio) para fazer as curvas!",
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 12.sp,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 90.dp)
        )

        // Bottom Controls: Nitro Button + Manual Steer
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = 20.dp, start = 16.dp, end = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Manual Arrow Left / Right
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                IconButton(
                    onClick = {},
                    modifier = Modifier
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onPress = {
                                    manualSteer = -1.0f
                                    soundEffects.vibrate(10)
                                    tryAwaitRelease()
                                    manualSteer = 0f
                                }
                            )
                        }
                        .background(Color.Black.copy(alpha = 0.6f), shape = RoundedCornerShape(16.dp))
                        .padding(8.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Esquerda", tint = Color.White)
                }

                IconButton(
                    onClick = {},
                    modifier = Modifier
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onPress = {
                                    manualSteer = 1.0f
                                    soundEffects.vibrate(10)
                                    tryAwaitRelease()
                                    manualSteer = 0f
                                }
                            )
                        }
                        .background(Color.Black.copy(alpha = 0.6f), shape = RoundedCornerShape(16.dp))
                        .padding(8.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Direita", tint = Color.White)
                }
            }

            // Nitro Boost Pad Button
            Box(
                modifier = Modifier
                    .size(width = 110.dp, height = 58.dp)
                    .background(
                        if (nitroFuel > 10f) Color(0xFF0284C7) else Color(0xFF334155),
                        shape = RoundedCornerShape(18.dp)
                    )
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onPress = {
                                if (nitroFuel > 5f) {
                                    isNitroActive = true
                                    soundEffects.playNitro()
                                }
                                tryAwaitRelease()
                                isNitroActive = false
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(Icons.Default.Bolt, contentDescription = "Nitro", tint = Color(0xFFFDE047), modifier = Modifier.size(24.dp))
                    Column {
                        Text("NITRO", color = Color.White, fontWeight = FontWeight.Black, fontSize = 13.sp)
                        Text("${nitroFuel.toInt()}%", color = Color(0xFF93C5FD), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
