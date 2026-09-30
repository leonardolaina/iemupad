package com.example.ui.games

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.SoundEffects
import kotlinx.coroutines.delay
import kotlin.math.sqrt
import kotlin.random.Random

data class FlyingFruit(
    val id: Int,
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    val radius: Float = 36f,
    val color: Color,
    val name: String,
    val isBomb: Boolean = false,
    var isSliced: Boolean = false,
    var sliceOffset: Float = 0f
)

@Composable
fun FruitSlicerGame(
    soundEffects: SoundEffects,
    fastForwardMultiplier: Float = 1f,
    onScoreUpdated: (Int) -> Unit = {}
) {
    var score by remember { mutableIntStateOf(0) }
    var lives by remember { mutableIntStateOf(3) }
    var gameOver by remember { mutableStateOf(false) }
    var gameOverReason by remember { mutableStateOf("") }

    val fruits = remember { mutableStateListOf<FlyingFruit>() }
    val bladeTrail = remember { mutableStateListOf<Offset>() }
    var fruitCounter by remember { mutableIntStateOf(1) }

    var canvasWidth by remember { mutableFloatStateOf(800f) }
    var canvasHeight by remember { mutableFloatStateOf(1200f) }

    fun checkSlice(p1: Offset, p2: Offset) {
        for (f in fruits) {
            if (f.isSliced) continue
            val dist = sqrt((f.x - p2.x) * (f.x - p2.x) + (f.y - p2.y) * (f.y - p2.y))
            if (dist < f.radius + 18f) {
                if (f.isBomb) {
                    gameOver = true
                    gameOverReason = "BOMBA DETONADA!"
                    soundEffects.playBoom()
                    return
                } else {
                    f.isSliced = true
                    score += 1
                    onScoreUpdated(score)
                    soundEffects.playSlice()
                    soundEffects.vibrate(25)
                }
            }
        }
    }

    // Physics Loop
    LaunchedEffect(gameOver, fastForwardMultiplier) {
        if (gameOver) return@LaunchedEffect
        var spawnTimer = 0
        while (!gameOver) {
            val step = (24 / fastForwardMultiplier).toLong().coerceAtLeast(10)
            delay(step)

            // Fade blade trail
            if (bladeTrail.size > 1) {
                bladeTrail.removeAt(0)
            }

            spawnTimer++
            if (spawnTimer > 35 && fruits.size < 5) {
                spawnTimer = 0
                val isBomb = Random.nextFloat() < 0.2f
                val fruitColor = when {
                    isBomb -> Color(0xFF27272A)
                    Random.nextFloat() < 0.35f -> Color(0xFFE11D48) // Apple/Watermelon
                    Random.nextFloat() < 0.65f -> Color(0xFFFACC15) // Banana
                    else -> Color(0xFFFB923C) // Orange
                }
                val sx = Random.nextFloat() * (canvasWidth * 0.6f) + (canvasWidth * 0.2f)
                val svx = (Random.nextFloat() - 0.5f) * 6f
                val svy = -Random.nextFloat() * 5f - 16f

                fruits.add(
                    FlyingFruit(
                        id = fruitCounter++,
                        x = sx,
                        y = canvasHeight + 20f,
                        vx = svx,
                        vy = svy,
                        color = fruitColor,
                        name = if (isBomb) "Bomba" else "Fruta",
                        isBomb = isBomb
                    )
                )
            }

            // Update fruit physics
            val gravity = 0.55f * fastForwardMultiplier
            val toRemove = mutableListOf<FlyingFruit>()
            for (f in fruits) {
                f.x += f.vx * fastForwardMultiplier
                f.y += f.vy * fastForwardMultiplier
                f.vy += gravity

                if (f.isSliced) {
                    f.sliceOffset += 4f * fastForwardMultiplier
                }

                // Check dropped fruit
                if (f.y > canvasHeight + 60f && f.vy > 0f) {
                    if (!f.isSliced && !f.isBomb) {
                        lives--
                        soundEffects.vibrate(40)
                        if (lives <= 0) {
                            gameOver = true
                            gameOverReason = "MUITAS FRUTAS PERDIDAS!"
                            soundEffects.playBoom()
                        }
                    }
                    toRemove.add(f)
                }
            }
            fruits.removeAll(toRemove)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF2C1810)) // Classic wooden cutting board background
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            bladeTrail.clear()
                            bladeTrail.add(offset)
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            val prev = bladeTrail.lastOrNull() ?: change.position
                            bladeTrail.add(change.position)
                            if (bladeTrail.size > 8) bladeTrail.removeAt(0)
                            checkSlice(prev, change.position)
                        },
                        onDragEnd = { bladeTrail.clear() },
                        onDragCancel = { bladeTrail.clear() }
                    )
                }
        ) {
            canvasWidth = size.width
            canvasHeight = size.height

            // Subtle wood plank lines
            var wy = 0f
            while (wy < size.height) {
                drawLine(Color(0x22FFFFFF), Offset(0f, wy), Offset(size.width, wy), strokeWidth = 2f)
                wy += 90f
            }

            // Draw Fruits
            for (f in fruits) {
                if (f.isBomb) {
                    // Bomb sphere with fuse
                    drawCircle(Color.Black, radius = f.radius, center = Offset(f.x, f.y))
                    drawCircle(Color.Red, radius = 7f, center = Offset(f.x - 6f, f.y - f.radius - 8f))
                } else if (!f.isSliced) {
                    drawCircle(f.color, radius = f.radius, center = Offset(f.x, f.y))
                    drawCircle(Color.White.copy(alpha = 0.5f), radius = f.radius * 0.4f, center = Offset(f.x - 8f, f.y - 8f))
                } else {
                    // Sliced halves moving apart
                    drawCircle(f.color, radius = f.radius * 0.9f, center = Offset(f.x - f.sliceOffset, f.y))
                    drawCircle(f.color, radius = f.radius * 0.9f, center = Offset(f.x + f.sliceOffset, f.y))
                }
            }

            // Draw Katana Blade Trail
            if (bladeTrail.size > 1) {
                val bladePath = Path()
                bladePath.moveTo(bladeTrail.first().x, bladeTrail.first().y)
                for (pt in bladeTrail) {
                    bladePath.lineTo(pt.x, pt.y)
                }
                drawPath(bladePath, color = Color.White, style = Stroke(width = 7f))
                drawPath(bladePath, color = Color(0xFF64D2FF), style = Stroke(width = 3f))
            }
        }

        // Header Score and Lives (X X X)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = Color.Black.copy(alpha = 0.7f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "Fatias: $score",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                repeat(3) { index ->
                    val hasLife = (index < lives)
                    Text(
                        text = "X",
                        color = if (hasLife) Color(0xFF30D158) else Color(0xFFFF453A),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }

        // Game Over Overlay
        if (gameOver) {
            Surface(
                color = Color.Black.copy(alpha = 0.92f),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.align(Alignment.Center)
            ) {
                Column(
                    modifier = Modifier.padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(gameOverReason, color = Color(0xFFFF453A), fontSize = 22.sp, fontWeight = FontWeight.Black)
                    Text("Total fatiado: $score", color = Color.White, fontSize = 18.sp, modifier = Modifier.padding(top = 8.dp))
                    Button(
                        onClick = {
                            fruits.clear()
                            bladeTrail.clear()
                            score = 0
                            lives = 3
                            gameOver = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9F0A)),
                        modifier = Modifier.padding(top = 18.dp)
                    ) {
                        Text("Jogar Novamente", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
