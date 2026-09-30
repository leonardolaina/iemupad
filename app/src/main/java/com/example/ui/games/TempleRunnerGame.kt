package com.example.ui.games

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.SoundEffects
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.random.Random

data class RunnerObstacle(
    val id: Int,
    val lane: Int, // 0: left, 1: center, 2: right
    var z: Float, // 1.0 (horizon) down to 0.0 (near player)
    val isJumpOver: Boolean // true = jump over, false = slide under
)

data class RunnerCoin(
    val id: Int,
    val lane: Int,
    var z: Float,
    var isCollected: Boolean = false
)

@Composable
fun TempleRunnerGame(
    gameTitle: String,
    tiltX: Float,
    soundEffects: SoundEffects,
    fastForwardMultiplier: Float = 1f,
    onScoreUpdated: (Int) -> Unit = {}
) {
    var metersRan by remember { mutableIntStateOf(0) }
    var coinsCollected by remember { mutableIntStateOf(0) }
    var currentLane by remember { mutableIntStateOf(1) } // 0: left, 1: center, 2: right
    var isJumping by remember { mutableStateOf(false) }
    var isSliding by remember { mutableStateOf(false) }
    var gameOver by remember { mutableStateOf(false) }

    val obstacles = remember { mutableStateListOf<RunnerObstacle>() }
    val coins = remember { mutableStateListOf<RunnerCoin>() }
    var entityCounter by remember { mutableIntStateOf(1) }

    var canvasWidth by remember { mutableFloatStateOf(800f) }
    var canvasHeight by remember { mutableFloatStateOf(1200f) }

    fun resetRunner() {
        metersRan = 0
        coinsCollected = 0
        currentLane = 1
        isJumping = false
        isSliding = false
        gameOver = false
        obstacles.clear()
        coins.clear()
    }

    // Runner Game Loop
    LaunchedEffect(gameOver, fastForwardMultiplier) {
        if (gameOver) return@LaunchedEffect
        var spawnTimer = 0
        while (!gameOver) {
            val step = (24 / fastForwardMultiplier).toLong().coerceAtLeast(10)
            delay(step)

            metersRan += (1 * fastForwardMultiplier).toInt()
            val totalScore = metersRan + (coinsCollected * 10)
            onScoreUpdated(totalScore)

            // Tilt steering: if tilt is significant, change lane
            if (tiltX > 0.45f && currentLane < 2) {
                currentLane++
                soundEffects.vibrate(12)
            } else if (tiltX < -0.45f && currentLane > 0) {
                currentLane--
                soundEffects.vibrate(12)
            }

            // Spawn obstacles & coins
            spawnTimer++
            if (spawnTimer > 35) {
                spawnTimer = 0
                val obsLane = Random.nextInt(3)
                val isJump = Random.nextBoolean()
                obstacles.add(RunnerObstacle(entityCounter++, obsLane, 1.0f, isJump))

                // Spawn 3 coins in a different lane
                val coinLane = if (obsLane == 1) (if (Random.nextBoolean()) 0 else 2) else 1
                coins.add(RunnerCoin(entityCounter++, coinLane, 0.95f))
                coins.add(RunnerCoin(entityCounter++, coinLane, 0.85f))
                coins.add(RunnerCoin(entityCounter++, coinLane, 0.75f))
            }

            // Update obstacles
            val toRemoveObs = mutableListOf<RunnerObstacle>()
            for (obs in obstacles) {
                obs.z -= 0.022f * fastForwardMultiplier
                if (obs.z in 0.08f..0.22f && obs.lane == currentLane) {
                    val hitJump = obs.isJumpOver && !isJumping
                    val hitSlide = !obs.isJumpOver && !isSliding
                    if (hitJump || hitSlide) {
                        gameOver = true
                        soundEffects.playBoom()
                        soundEffects.vibrate(100)
                    }
                }
                if (obs.z <= 0.02f) {
                    toRemoveObs.add(obs)
                }
            }
            obstacles.removeAll(toRemoveObs)

            // Update coins
            val toRemoveCoins = mutableListOf<RunnerCoin>()
            for (c in coins) {
                c.z -= 0.022f * fastForwardMultiplier
                if (!c.isCollected && c.z in 0.08f..0.22f && c.lane == currentLane) {
                    c.isCollected = true
                    coinsCollected++
                    soundEffects.playCoin()
                    soundEffects.vibrate(15)
                }
                if (c.z <= 0.02f) {
                    toRemoveCoins.add(c)
                }
            }
            coins.removeAll(toRemoveCoins)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1C1917))
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragEnd = {
                        isJumping = false
                        isSliding = false
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        val (dx, dy) = dragAmount
                        if (abs(dy) > abs(dx)) {
                            if (dy < -15f && !isJumping) {
                                isJumping = true
                                isSliding = false
                                soundEffects.playJump()
                            } else if (dy > 15f && !isSliding) {
                                isSliding = true
                                isJumping = false
                                soundEffects.vibrate(20)
                            }
                        } else {
                            if (dx > 20f && currentLane < 2) {
                                currentLane++
                                soundEffects.vibrate(15)
                            } else if (dx < -20f && currentLane > 0) {
                                currentLane--
                                soundEffects.vibrate(15)
                            }
                        }
                    }
                )
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            canvasWidth = size.width
            canvasHeight = size.height

            val horizonY = size.height * 0.35f

            // Temple jungle sky
            drawRect(
                brush = Brush.verticalGradient(
                    listOf(Color(0xFF0F172A), Color(0xFF14532D), Color(0xFF78350F)),
                    startY = 0f,
                    endY = horizonY
                ),
                topLeft = Offset(0f, 0f),
                size = Size(size.width, horizonY)
            )

            // Ancient Stone Pathway 3D Perspective
            val topRoadW = size.width * 0.18f
            val botRoadW = size.width * 0.90f
            val topRoadX = size.width * 0.5f
            val botRoadX = size.width * 0.5f

            val roadP = Path().apply {
                moveTo(topRoadX - topRoadW / 2f, horizonY)
                lineTo(topRoadX + topRoadW / 2f, horizonY)
                lineTo(botRoadX + botRoadW / 2f, size.height)
                lineTo(botRoadX - botRoadW / 2f, size.height)
                close()
            }
            drawPath(roadP, color = Color(0xFF44403C))

            // 3 Lane dividing lines
            val laneOffsets = listOf(-0.33f, 0.33f)
            for (lo in laneOffsets) {
                val tX = topRoadX + (lo * topRoadW / 2f)
                val bX = botRoadX + (lo * botRoadW / 2f)
                drawLine(Color(0xFF78716C), Offset(tX, horizonY), Offset(bX, size.height), strokeWidth = 2f)
            }

            // Draw Obstacles (sorted by z from far to near)
            for (obs in obstacles.sortedByDescending { it.z }) {
                val scale = 1f - obs.z
                if (scale < 0.05f) continue

                val yPos = horizonY + (size.height - horizonY) * (1f - obs.z)
                val curRoadW = topRoadW + (botRoadW - topRoadW) * (1f - obs.z)
                val laneOffsetFrac = when (obs.lane) {
                    0 -> -0.66f
                    1 -> 0.0f
                    else -> 0.66f
                }
                val xPos = topRoadX + (laneOffsetFrac * (curRoadW / 2f))

                val oW = 60f * scale
                val oH = if (obs.isJumpOver) (25f * scale) else (50f * scale)

                if (obs.isJumpOver) {
                    // Tree trunk / fire pit to jump over
                    drawRoundRect(
                        color = Color(0xFFB45309),
                        topLeft = Offset(xPos - oW / 2f, yPos - oH),
                        size = Size(oW, oH),
                        cornerRadius = CornerRadius(4f * scale, 4f * scale)
                    )
                } else {
                    // Low stone arch to slide under
                    drawRoundRect(
                        color = Color(0xFF57534E),
                        topLeft = Offset(xPos - oW / 2f, yPos - oH - 15f),
                        size = Size(oW, oH),
                        cornerRadius = CornerRadius(6f * scale, 6f * scale)
                    )
                }
            }

            // Draw Gold Coins
            for (c in coins.filter { !it.isCollected }.sortedByDescending { it.z }) {
                val scale = 1f - c.z
                if (scale < 0.05f) continue

                val yPos = horizonY + (size.height - horizonY) * (1f - c.z)
                val curRoadW = topRoadW + (botRoadW - topRoadW) * (1f - c.z)
                val laneOffsetFrac = when (c.lane) {
                    0 -> -0.66f
                    1 -> 0.0f
                    else -> 0.66f
                }
                val xPos = topRoadX + (laneOffsetFrac * (curRoadW / 2f))

                val radius = 14f * scale
                drawCircle(Color(0xFFFACC15), radius = radius, center = Offset(xPos, yPos - 15f))
                drawCircle(Color(0xFFCA8A04), radius = radius * 0.6f, center = Offset(xPos, yPos - 15f))
            }

            // Draw Player Runner at bottom
            val pLaneFrac = when (currentLane) {
                0 -> -0.66f
                1 -> 0.0f
                else -> 0.66f
            }
            val pX = botRoadX + (pLaneFrac * (botRoadW / 2f))
            val basePY = size.height * 0.82f
            val pY = if (isJumping) (basePY - 75f) else basePY

            // Runner Shadow
            drawOval(Color.Black.copy(alpha = 0.5f), Offset(pX - 22f, basePY + 15f), Size(44f, 16f))

            // Character body (Adventurer runner)
            if (isSliding) {
                // Sliding low capsule
                drawRoundRect(Color(0xFFE2E8F0), Offset(pX - 30f, pY + 5f), Size(60f, 22f), CornerRadius(10f, 10f))
            } else {
                // Standing / Running
                drawCircle(Color(0xFFFDBA74), radius = 14f, center = Offset(pX, pY - 32f)) // Head
                drawRoundRect(Color(0xFF2563EB), Offset(pX - 16f, pY - 18f), Size(32f, 38f), CornerRadius(6f, 6f)) // Torso
                drawRoundRect(Color(0xFF1E293B), Offset(pX - 12f, pY + 20f), Size(10f, 18f), CornerRadius(3f, 3f)) // Left leg
                drawRoundRect(Color(0xFF1E293B), Offset(pX + 2f, pY + 20f), Size(10f, 18f), CornerRadius(3f, 3f)) // Right leg
            }
        }

        // Header Telemetry
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = Color.Black.copy(alpha = 0.8f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "${metersRan}m",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                )
            }

            Surface(
                color = Color(0xFFFACC15),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "Moedas: $coinsCollected",
                    color = Color.Black,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }

        // Action Guidance
        Text(
            text = "Deslize p/ Cima: PULAR • Deslize p/ Baixo: ESCORREGAR • Incline p/ virar",
            color = Color.White.copy(alpha = 0.75f),
            fontSize = 11.sp,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 20.dp)
        )

        // Game Over Overlay
        if (gameOver) {
            Surface(
                color = Color.Black.copy(alpha = 0.9f),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.align(Alignment.Center)
            ) {
                Column(
                    modifier = Modifier.padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("FIM DE JOGO!", color = Color(0xFFFF453A), fontSize = 24.sp, fontWeight = FontWeight.Black)
                    Text("Distância: ${metersRan}m", color = Color.White, fontSize = 18.sp, modifier = Modifier.padding(top = 8.dp))
                    Text("Moedas: $coinsCollected", color = Color(0xFFFACC15), fontSize = 16.sp, modifier = Modifier.padding(top = 4.dp))
                    Button(
                        onClick = { resetRunner() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0A84FF)),
                        modifier = Modifier.padding(top = 20.dp)
                    ) {
                        Text("Tentar Novamente", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
