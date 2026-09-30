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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.SoundEffects
import kotlinx.coroutines.delay
import kotlin.random.Random

data class Platform(
    var x: Float,
    var y: Float,
    val width: Float = 110f,
    val height: Float = 22f,
    val isMoving: Boolean = false,
    var vx: Float = 2.5f,
    val hasSpring: Boolean = false
)

@Composable
fun DoodleJumpGame(
    tiltX: Float,
    soundEffects: SoundEffects,
    fastForwardMultiplier: Float = 1f,
    onScoreUpdated: (Int) -> Unit = {}
) {
    var score by remember { mutableIntStateOf(0) }
    var maxHeightClimbed by remember { mutableFloatStateOf(0f) }
    var gameOver by remember { mutableStateOf(false) }

    var doodlerX by remember { mutableFloatStateOf(400f) }
    var doodlerY by remember { mutableFloatStateOf(700f) }
    var velocityY by remember { mutableFloatStateOf(-12f) }
    var manualTilt by remember { mutableFloatStateOf(0f) }

    val platforms = remember { mutableStateListOf<Platform>() }
    var canvasWidth by remember { mutableFloatStateOf(800f) }
    var canvasHeight by remember { mutableFloatStateOf(1200f) }

    fun resetGame() {
        platforms.clear()
        doodlerX = canvasWidth / 2f
        doodlerY = canvasHeight * 0.7f
        velocityY = -14f
        score = 0
        maxHeightClimbed = 0f
        gameOver = false

        // Base platform
        platforms.add(Platform(doodlerX - 55f, doodlerY + 40f))

        // Initial platforms
        var curY = doodlerY - 60f
        while (curY > -600f) {
            val px = Random.nextFloat() * (canvasWidth - 140f) + 20f
            val isMoving = Random.nextFloat() < 0.25f
            val hasSpring = !isMoving && Random.nextFloat() < 0.2f
            platforms.add(Platform(px, curY, isMoving = isMoving, hasSpring = hasSpring))
            curY -= Random.nextFloat() * 70f + 65f
        }
    }

    LaunchedEffect(canvasWidth, canvasHeight) {
        if (canvasWidth > 100f && platforms.isEmpty()) {
            resetGame()
        }
    }

    // Physics loop
    LaunchedEffect(gameOver, fastForwardMultiplier) {
        if (gameOver) return@LaunchedEffect
        while (!gameOver) {
            val step = (22 / fastForwardMultiplier).toLong().coerceAtLeast(10)
            delay(step)

            // Horizontal motion: combination of sensor tilt and manual arrow buttons
            val combinedTilt = (tiltX * 2.2f + manualTilt).coerceIn(-1.5f, 1.5f)
            doodlerX += combinedTilt * 9f * fastForwardMultiplier

            // Screen wrap
            if (doodlerX < -20f) doodlerX = canvasWidth + 10f
            if (doodlerX > canvasWidth + 20f) doodlerX = -10f

            // Gravity & Vertical motion
            val gravity = 0.55f * fastForwardMultiplier
            velocityY += gravity
            doodlerY += velocityY * fastForwardMultiplier

            // Moving platforms update
            for (p in platforms) {
                if (p.isMoving) {
                    p.x += p.vx * fastForwardMultiplier
                    if (p.x < 10f || p.x + p.width > canvasWidth - 10f) {
                        p.vx = -p.vx
                    }
                }
            }

            // Camera scroll upwards when doodler is in upper half
            val threshold = canvasHeight * 0.45f
            if (doodlerY < threshold) {
                val deltaY = threshold - doodlerY
                doodlerY = threshold
                maxHeightClimbed += deltaY
                score = (maxHeightClimbed / 10f).toInt()
                onScoreUpdated(score)

                for (p in platforms) {
                    p.y += deltaY
                }

                // Remove off-screen platforms and generate new ones above
                platforms.removeAll { it.y > canvasHeight + 50f }

                var highestY = platforms.minOfOrNull { it.y } ?: 0f
                while (highestY > -400f) {
                    val nextY = highestY - (Random.nextFloat() * 75f + 65f)
                    val px = Random.nextFloat() * (canvasWidth - 140f) + 20f
                    val isMoving = Random.nextFloat() < 0.3f
                    val hasSpring = !isMoving && Random.nextFloat() < 0.22f
                    platforms.add(Platform(px, nextY, isMoving = isMoving, hasSpring = hasSpring))
                    highestY = nextY
                }
            }

            // Platform collision (only when falling downwards)
            if (velocityY > 0f) {
                val footY = doodlerY + 28f
                for (p in platforms) {
                    if (doodlerX in (p.x - 20f)..(p.x + p.width + 10f) &&
                        footY in (p.y)..(p.y + p.height + 14f)
                    ) {
                        if (p.hasSpring) {
                            velocityY = -24f // Super spring jump!
                            soundEffects.playJump()
                            soundEffects.vibrate(45)
                        } else {
                            velocityY = -14.5f // Normal bounce
                            soundEffects.playJump()
                        }
                        break
                    }
                }
            }

            // Game over when falling below screen
            if (doodlerY > canvasHeight + 80f) {
                gameOver = true
                soundEffects.playBoom()
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF9F5DE)) // Classic vintage graph paper tone
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            canvasWidth = size.width
            canvasHeight = size.height

            // Background subtle grid lines
            val gridSpacing = 40f
            var gx = 0f
            while (gx < size.width) {
                drawLine(Color(0x18000000), Offset(gx, 0f), Offset(gx, size.height), strokeWidth = 1f)
                gx += gridSpacing
            }
            var gy = 0f
            while (gy < size.height) {
                drawLine(Color(0x18000000), Offset(0f, gy), Offset(size.width, gy), strokeWidth = 1f)
                gy += gridSpacing
            }

            // Platforms
            for (p in platforms) {
                val platColor = if (p.isMoving) Color(0xFF007AFF) else Color(0xFF5DBB63)
                drawRoundRect(
                    color = platColor,
                    topLeft = Offset(p.x, p.y),
                    size = Size(p.width, p.height),
                    cornerRadius = CornerRadius(10f, 10f)
                )

                // Spring indicator
                if (p.hasSpring) {
                    val sx = p.x + p.width / 2f - 10f
                    val sy = p.y - 14f
                    drawRoundRect(
                        color = Color(0xFFFF9500),
                        topLeft = Offset(sx, sy),
                        size = Size(20f, 14f),
                        cornerRadius = CornerRadius(4f, 4f)
                    )
                }
            }

            // Doodler Character
            val currentTilt = (tiltX * 2.2f + manualTilt).coerceIn(-1.5f, 1.5f)
            val dRadius = 24f
            // Body (Golden / Olive cute monster)
            drawCircle(Color(0xFF8BC34A), radius = dRadius, center = Offset(doodlerX, doodlerY))
            // Snout/nose
            val facingRight = (currentTilt >= 0f)
            val noseOffsetX = if (facingRight) 16f else -16f
            drawRoundRect(
                color = Color(0xFF7CB342),
                topLeft = Offset(doodlerX + noseOffsetX - 8f, doodlerY - 5f),
                size = Size(18f, 12f),
                cornerRadius = CornerRadius(6f, 6f)
            )
            // Eyes
            val eyeX = if (facingRight) doodlerX + 6f else doodlerX - 14f
            drawCircle(Color.White, radius = 6f, center = Offset(eyeX, doodlerY - 8f))
            drawCircle(Color.Black, radius = 3f, center = Offset(eyeX + (if (facingRight) 2f else -2f), doodlerY - 8f))

            // Legs
            drawCircle(Color(0xFF689F38), radius = 5f, center = Offset(doodlerX - 10f, doodlerY + 24f))
            drawCircle(Color(0xFF689F38), radius = 5f, center = Offset(doodlerX + 10f, doodlerY + 24f))
        }

        // Header Score
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = Color.Black.copy(alpha = 0.75f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "Pontos: $score",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                )
            }

            Surface(
                color = Color(0xFF30D158).copy(alpha = 0.9f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "Incline ou use Setas",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }

        // Manual on-screen controls at bottom
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = 20.dp, start = 20.dp, end = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(
                onClick = {},
                modifier = Modifier
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onPress = {
                                manualTilt = -1.0f
                                soundEffects.vibrate(10)
                                tryAwaitRelease()
                                manualTilt = 0f
                            }
                        )
                    }
                    .background(Color.Black.copy(alpha = 0.5f), shape = RoundedCornerShape(16.dp))
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
                                manualTilt = 1.0f
                                soundEffects.vibrate(10)
                                tryAwaitRelease()
                                manualTilt = 0f
                            }
                        )
                    }
                    .background(Color.Black.copy(alpha = 0.5f), shape = RoundedCornerShape(16.dp))
                    .padding(8.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Direita", tint = Color.White)
            }
        }

        // Game Over Dialog
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
                    Text("GAME OVER!", color = Color(0xFFFF453A), fontSize = 26.sp, fontWeight = FontWeight.Black)
                    Text("Altura alcançada: $score pts", color = Color.White, fontSize = 18.sp, modifier = Modifier.padding(top = 8.dp))
                    Button(
                        onClick = { resetGame() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF30D158)),
                        modifier = Modifier.padding(top = 20.dp)
                    ) {
                        Text("Tentar Novamente", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
