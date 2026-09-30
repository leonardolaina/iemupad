package com.example.ui.games

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.SoundEffects
import kotlinx.coroutines.delay
import kotlin.math.sqrt

@Composable
fun GenericIpaRunner(
    gameTitle: String,
    tiltX: Float,
    tiltY: Float,
    soundEffects: SoundEffects,
    fastForwardMultiplier: Float = 1f,
    onScoreUpdated: (Int) -> Unit = {}
) {
    var score by remember { mutableIntStateOf(0) }
    var timeRemaining by remember { mutableIntStateOf(60) }
    var reachedGoal by remember { mutableStateOf(false) }

    var ballX by remember { mutableFloatStateOf(400f) }
    var ballY by remember { mutableFloatStateOf(600f) }
    var vx by remember { mutableFloatStateOf(0f) }
    var vy by remember { mutableFloatStateOf(0f) }

    var canvasWidth by remember { mutableFloatStateOf(800f) }
    var canvasHeight by remember { mutableFloatStateOf(1200f) }

    val collectedBananas = remember { mutableStateListOf<Offset>() }
    val initialBananas = remember {
        mutableStateListOf(
            Offset(200f, 300f),
            Offset(600f, 350f),
            Offset(400f, 450f),
            Offset(300f, 750f),
            Offset(550f, 800f),
            Offset(220f, 950f)
        )
    }

    // Reset / init
    fun resetLabyrinth() {
        ballX = canvasWidth / 2f
        ballY = canvasHeight * 0.85f
        vx = 0f
        vy = 0f
        score = 0
        timeRemaining = 60
        reachedGoal = false
        collectedBananas.clear()
    }

    LaunchedEffect(canvasWidth) {
        if (canvasWidth > 100f && ballX == 400f) {
            resetLabyrinth()
        }
    }

    // Physics Loop
    LaunchedEffect(reachedGoal, fastForwardMultiplier) {
        if (reachedGoal) return@LaunchedEffect
        var secondTimer = 0
        while (!reachedGoal && timeRemaining > 0) {
            val step = (24 / fastForwardMultiplier).toLong().coerceAtLeast(10)
            delay(step)

            secondTimer++
            if (secondTimer > 40) {
                secondTimer = 0
                timeRemaining--
            }

            // Apply sensor tilt to velocity
            vx = (vx * 0.94f) + (tiltX * 1.5f * fastForwardMultiplier)
            vy = (vy * 0.94f) + (tiltY * 1.5f * fastForwardMultiplier)

            ballX += vx
            ballY += vy

            // Wall collisions
            val radius = 26f
            if (ballX < radius) {
                ballX = radius
                vx = -vx * 0.6f
                soundEffects.vibrate(12)
            }
            if (ballX > canvasWidth - radius) {
                ballX = canvasWidth - radius
                vx = -vx * 0.6f
                soundEffects.vibrate(12)
            }
            if (ballY < radius) {
                ballY = radius
                vy = -vy * 0.6f
                soundEffects.vibrate(12)
            }
            if (ballY > canvasHeight - radius) {
                ballY = canvasHeight - radius
                vy = -vy * 0.6f
                soundEffects.vibrate(12)
            }

            // Banana collection
            val hitBanana = initialBananas.firstOrNull { b ->
                !collectedBananas.contains(b) &&
                    sqrt((ballX - b.x) * (ballX - b.x) + (ballY - b.y) * (ballY - b.y)) < 42f
            }
            if (hitBanana != null) {
                collectedBananas.add(hitBanana)
                score += 100
                onScoreUpdated(score)
                soundEffects.playJump()
                soundEffects.vibrate(20)
            }

            // Goal detection (Arch at top center)
            val goalCenter = Offset(canvasWidth * 0.5f, 130f)
            val distToGoal = sqrt((ballX - goalCenter.x) * (ballX - goalCenter.x) + (ballY - goalCenter.y) * (ballY - goalCenter.y))
            if (distToGoal < 60f) {
                reachedGoal = true
                score += (timeRemaining * 50) + 1000
                onScoreUpdated(score)
                soundEffects.playLand()
                soundEffects.vibrate(60)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF0F172A), Color(0xFF1E293B), Color(0xFF0B132B))
                )
            )
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            canvasWidth = size.width
            canvasHeight = size.height

            // 3D Grid perspective floor
            val horizonY = size.height * 0.05f
            var gy = horizonY
            while (gy < size.height) {
                drawLine(Color(0x2238BDF8), Offset(0f, gy), Offset(size.width, gy), strokeWidth = 1.5f)
                gy += 65f
            }
            var gx = 0f
            while (gx < size.width) {
                drawLine(Color(0x2238BDF8), Offset(gx, horizonY), Offset(gx, size.height), strokeWidth = 1.5f)
                gx += 65f
            }

            // Draw Goal Arch at top
            val goalX = size.width * 0.5f
            val goalY = 130f
            drawCircle(Color(0xFFFFD60A).copy(alpha = 0.35f), radius = 55f, center = Offset(goalX, goalY))
            drawCircle(Color(0xFFFFD60A), radius = 55f, center = Offset(goalX, goalY), style = Stroke(width = 5f))

            // Draw Collectible Bananas / Rings
            for (b in initialBananas) {
                if (!collectedBananas.contains(b)) {
                    drawCircle(Color(0xFFFFCC00), radius = 16f, center = b)
                    drawCircle(Color.White, radius = 6f, center = Offset(b.x - 3f, b.y - 3f))
                }
            }

            // Draw Monkey Sphere (AiAi Ball with translucent shine)
            val bRad = 26f
            // Drop shadow
            drawCircle(Color.Black.copy(alpha = 0.4f), radius = bRad * 1.1f, center = Offset(ballX + 6f, ballY + 10f))
            // Outer sphere
            drawCircle(Color(0xFF38BDF8).copy(alpha = 0.75f), radius = bRad, center = Offset(ballX, ballY))
            drawCircle(Color.White, radius = bRad, center = Offset(ballX, ballY), style = Stroke(width = 3f))
            // Inner monkey face / orb core
            drawCircle(Color(0xFFFF9500), radius = bRad * 0.55f, center = Offset(ballX, ballY))
            // Specular reflection highlight
            drawCircle(Color.White.copy(alpha = 0.8f), radius = 6f, center = Offset(ballX - 8f, ballY - 8f))
        }

        // Header telemetry HUD
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
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
                    Text(gameTitle, color = Color(0xFF64D2FF), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text("Pontos: $score", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Black)
                }
            }

            Surface(
                color = if (timeRemaining <= 10) Color(0xFFFF453A) else Color(0xFF30D158),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "Tempo: ${timeRemaining}s",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                )
            }
        }

        // Subtitle instructions
        Text(
            text = "Incline seu aparelho (Acelerômetro) para rolar a esfera até o GOAL!",
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 12.sp,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp)
        )

        // Goal reached banner
        if (reachedGoal) {
            Surface(
                color = Color.Black.copy(alpha = 0.9f),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.align(Alignment.Center)
            ) {
                Column(
                    modifier = Modifier.padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("GOAL! FASE COMPLETA!", color = Color(0xFFFFD60A), fontSize = 24.sp, fontWeight = FontWeight.Black)
                    Text("Pontuação Total: $score pts", color = Color.White, fontSize = 18.sp, modifier = Modifier.padding(top = 8.dp))
                    Button(
                        onClick = { resetLabyrinth() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0A84FF)),
                        modifier = Modifier.padding(top = 20.dp)
                    ) {
                        Text("Jogar Próxima Fase")
                    }
                }
            }
        }
    }
}
