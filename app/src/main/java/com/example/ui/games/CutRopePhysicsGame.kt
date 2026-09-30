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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.SoundEffects
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

data class Rope(
    val anchorX: Float,
    val anchorY: Float,
    var isCut: Boolean = false
)

data class StarTarget(
    val x: Float,
    val y: Float,
    var isCollected: Boolean = false
)

@Composable
fun CutRopePhysicsGame(
    gameTitle: String,
    soundEffects: SoundEffects,
    fastForwardMultiplier: Float = 1f,
    onScoreUpdated: (Int) -> Unit = {}
) {
    var score by remember { mutableIntStateOf(0) }
    var starsCollected by remember { mutableIntStateOf(0) }
    var levelWon by remember { mutableStateOf(false) }

    var candyX by remember { mutableFloatStateOf(400f) }
    var candyY by remember { mutableFloatStateOf(450f) }
    var vx by remember { mutableFloatStateOf(3f) }
    var vy by remember { mutableFloatStateOf(0f) }

    var canvasWidth by remember { mutableFloatStateOf(800f) }
    var canvasHeight by remember { mutableFloatStateOf(1200f) }

    val ropes = remember {
        mutableStateListOf(
            Rope(anchorX = 220f, anchorY = 120f),
            Rope(anchorX = 580f, anchorY = 140f)
        )
    }

    val stars = remember {
        mutableStateListOf(
            StarTarget(400f, 320f),
            StarTarget(300f, 580f),
            StarTarget(500f, 620f)
        )
    }

    val cutTrail = remember { mutableStateListOf<Offset>() }

    fun resetLevel() {
        candyX = canvasWidth * 0.5f
        candyY = 380f
        vx = 2.5f
        vy = 0f
        score = 0
        starsCollected = 0
        levelWon = false
        ropes[0] = ropes[0].copy(isCut = false)
        ropes[1] = ropes[1].copy(isCut = false)
        for (i in stars.indices) {
            stars[i] = stars[i].copy(isCollected = false)
        }
    }

    // Physics Simulation Loop
    LaunchedEffect(levelWon, fastForwardMultiplier) {
        if (levelWon) return@LaunchedEffect
        while (!levelWon) {
            val step = (22 / fastForwardMultiplier).toLong().coerceAtLeast(10)
            delay(step)

            if (cutTrail.size > 1) {
                cutTrail.removeAt(0)
            }

            // Gravity
            val gravity = 0.55f * fastForwardMultiplier
            vy += gravity

            // Ropes constraint
            val activeRopes = ropes.filter { !it.isCut }
            for (r in activeRopes) {
                val dx = candyX - r.anchorX
                val dy = candyY - r.anchorY
                val dist = sqrt(dx * dx + dy * dy)
                val targetLength = 320f
                if (dist > targetLength) {
                    val tension = (dist - targetLength) * 0.12f
                    vx -= (dx / dist) * tension
                    vy -= (dy / dist) * tension
                }
            }

            // Damping air friction
            vx *= 0.995f
            vy *= 0.995f

            candyX += vx * fastForwardMultiplier
            candyY += vy * fastForwardMultiplier

            // Star collection
            for (st in stars) {
                if (!st.isCollected) {
                    val dist = sqrt((candyX - st.x) * (candyX - st.x) + (candyY - st.y) * (candyY - st.y))
                    if (dist < 40f) {
                        st.isCollected = true
                        starsCollected++
                        score += 300
                        onScoreUpdated(score)
                        soundEffects.playCoin()
                        soundEffects.vibrate(20)
                    }
                }
            }

            // Monster mouth target at bottom
            val monsterX = canvasWidth * 0.5f
            val monsterY = canvasHeight * 0.85f
            val distToMouth = sqrt((candyX - monsterX) * (candyX - monsterX) + (candyY - monsterY) * (candyY - monsterY))
            if (distToMouth < 60f) {
                levelWon = true
                score += 1000
                onScoreUpdated(score)
                soundEffects.playLand()
                soundEffects.vibrate(60)
            }

            // Reset if candy fell off screen
            if (candyY > canvasHeight + 100f && !levelWon) {
                resetLevel()
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF854D0E)) // Warm cardboard / candy box texture
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            cutTrail.clear()
                            cutTrail.add(offset)
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            val pos = change.position
                            cutTrail.add(pos)
                            if (cutTrail.size > 6) cutTrail.removeAt(0)

                            // Check rope slicing
                            for (i in ropes.indices) {
                                val r = ropes[i]
                                if (!r.isCut) {
                                    val midX = (r.anchorX + candyX) / 2f
                                    val midY = (r.anchorY + candyY) / 2f
                                    val d = sqrt((pos.x - midX) * (pos.x - midX) + (pos.y - midY) * (pos.y - midY))
                                    if (d < 50f) {
                                        ropes[i] = r.copy(isCut = true)
                                        soundEffects.playSlice()
                                        soundEffects.vibrate(30)
                                    }
                                }
                            }
                        },
                        onDragEnd = { cutTrail.clear() },
                        onDragCancel = { cutTrail.clear() }
                    )
                }
        ) {
            canvasWidth = size.width
            canvasHeight = size.height

            // Background subtle striped pattern
            var sx = 0f
            while (sx < size.width) {
                drawLine(Color(0x15FFFFFF), Offset(sx, 0f), Offset(sx, size.height), strokeWidth = 24f)
                sx += 64f
            }

            // Draw Ropes and Anchors
            for (r in ropes) {
                // Anchor Pin
                drawCircle(Color(0xFF334155), radius = 14f, center = Offset(r.anchorX, r.anchorY))
                drawCircle(Color(0xFFF1F5F9), radius = 6f, center = Offset(r.anchorX, r.anchorY))

                if (!r.isCut) {
                    drawLine(
                        color = Color(0xFFD97706),
                        start = Offset(r.anchorX, r.anchorY),
                        end = Offset(candyX, candyY),
                        strokeWidth = 5f
                    )
                }
            }

            // Draw Stars
            for (st in stars) {
                if (!st.isCollected) {
                    drawCircle(Color(0xFFFACC15), radius = 22f, center = Offset(st.x, st.y))
                    drawCircle(Color.White, radius = 9f, center = Offset(st.x - 3f, st.y - 3f))
                }
            }

            // Draw Om Nom Monster at bottom
            val mX = size.width * 0.5f
            val mY = size.height * 0.85f
            val isMouthOpen = (candyY > size.height * 0.65f)

            // Monster Body (Cute Green Blob)
            drawCircle(Color(0xFF22C55E), radius = 54f, center = Offset(mX, mY))
            // Big Eyes
            drawCircle(Color.White, radius = 18f, center = Offset(mX - 20f, mY - 32f))
            drawCircle(Color.White, radius = 18f, center = Offset(mX + 20f, mY - 32f))
            drawCircle(Color.Black, radius = 8f, center = Offset(mX - 20f, mY - 32f))
            drawCircle(Color.Black, radius = 8f, center = Offset(mX + 20f, mY - 32f))

            // Mouth
            if (isMouthOpen) {
                drawOval(Color(0xFF991B1B), Offset(mX - 32f, mY - 8f), Size(64f, 40f))
                // White teeth
                drawCircle(Color.White, radius = 6f, center = Offset(mX - 16f, mY))
                drawCircle(Color.White, radius = 6f, center = Offset(mX + 16f, mY))
            } else {
                drawOval(Color(0xFF15803D), Offset(mX - 22f, mY + 6f), Size(44f, 16f))
            }

            // Draw Swirly Candy
            val cRad = 28f
            drawCircle(Color(0xFFDC2626), radius = cRad, center = Offset(candyX, candyY))
            drawCircle(Color.White, radius = cRad * 0.7f, center = Offset(candyX, candyY), style = Stroke(width = 6f))
            drawCircle(Color(0xFFFACC15), radius = cRad * 0.35f, center = Offset(candyX, candyY))

            // Finger Slicing Blade Trail
            if (cutTrail.size > 1) {
                val bladeP = Path().apply {
                    moveTo(cutTrail.first().x, cutTrail.first().y)
                    for (pt in cutTrail) {
                        lineTo(pt.x, pt.y)
                    }
                }
                drawPath(bladeP, color = Color.White, style = Stroke(width = 6f))
                drawPath(bladeP, color = Color(0xFF67E8F9), style = Stroke(width = 3f))
            }
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
                color = Color.Black.copy(alpha = 0.8f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "Pontos: $score",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                )
            }

            Surface(
                color = Color(0xFFFACC15),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "Estrelas: $starsCollected / 3",
                    color = Color.Black,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }

        // Action Guidance
        Text(
            text = "Deslize o dedo para cortar as cordas e alimentar o monstrinho!",
            color = Color.White.copy(alpha = 0.85f),
            fontSize = 12.sp,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 70.dp)
        )

        // Level Completed Dialog
        if (levelWon) {
            Surface(
                color = Color.Black.copy(alpha = 0.92f),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.align(Alignment.Center)
            ) {
                Column(
                    modifier = Modifier.padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("NÍVEL COMPLETO!", color = Color(0xFF22C55E), fontSize = 24.sp, fontWeight = FontWeight.Black)
                    Text("Você alimentou o Om Nom!", color = Color.White, fontSize = 16.sp, modifier = Modifier.padding(top = 6.dp))
                    Text("Estrelas Coletadas: $starsCollected / 3", color = Color(0xFFFACC15), fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp))
                    Button(
                        onClick = { resetLevel() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF22C55E)),
                        modifier = Modifier.padding(top = 18.dp)
                    ) {
                        Text("Jogar Novamente", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
