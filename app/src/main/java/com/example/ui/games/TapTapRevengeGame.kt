package com.example.ui.games

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.SoundEffects
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.random.Random

data class RhythmNote(
    val id: Int,
    val lane: Int, // 0, 1, 2
    var yProgress: Float = 0f, // 0.0 top to 1.0 hit bar
    var isHit: Boolean = false
)

@Composable
fun TapTapRevengeGame(
    soundEffects: SoundEffects,
    fastForwardMultiplier: Float = 1f,
    onScoreUpdated: (Int) -> Unit = {}
) {
    var score by remember { mutableIntStateOf(0) }
    var streak by remember { mutableIntStateOf(0) }
    var multiplier by remember { mutableIntStateOf(1) }
    var feedbackText by remember { mutableStateOf("PRONTO!") }
    var feedbackColor by remember { mutableStateOf(Color.White) }
    val notes = remember { mutableStateListOf<RhythmNote>() }
    var noteCounter by remember { mutableIntStateOf(1) }
    var isSongActive by remember { mutableStateOf(true) }
    var canvasWidth by remember { mutableFloatStateOf(800f) }
    var canvasHeight by remember { mutableFloatStateOf(1200f) }

    fun handleLaneTap(laneIndex: Int) {
        val candidate = notes.firstOrNull { it.lane == laneIndex && !it.isHit && it.yProgress in 0.70f..1.12f }
        if (candidate != null) {
            candidate.isHit = true
            val diff = abs(candidate.yProgress - 0.90f)
            if (diff < 0.05f) {
                feedbackText = "PERFEITO!"
                feedbackColor = Color(0xFF64D2FF)
                score += 100 * multiplier
                streak++
                soundEffects.playHit()
                soundEffects.vibrate(20)
            } else if (diff < 0.12f) {
                feedbackText = "BOM!"
                feedbackColor = Color(0xFF30D158)
                score += 50 * multiplier
                streak++
                soundEffects.playHit()
                soundEffects.vibrate(15)
            } else {
                feedbackText = "QUASE!"
                feedbackColor = Color(0xFFFFD60A)
                score += 20 * multiplier
                streak = 0
            }

            multiplier = when {
                streak >= 30 -> 8
                streak >= 15 -> 4
                streak >= 6 -> 2
                else -> 1
            }
            onScoreUpdated(score)
        } else {
            feedbackText = "ERROU"
            feedbackColor = Color(0xFFFF453A)
            streak = 0
            multiplier = 1
            soundEffects.vibrate(35)
        }
    }

    // Music beat & note generation loop
    LaunchedEffect(isSongActive, fastForwardMultiplier) {
        if (!isSongActive) return@LaunchedEffect
        var beatTimer = 0
        while (isSongActive) {
            val step = (28 / fastForwardMultiplier).toLong().coerceAtLeast(10)
            delay(step)

            beatTimer++
            if (beatTimer > 24) {
                beatTimer = 0
                val targetLane = Random.nextInt(3)
                notes.add(RhythmNote(id = noteCounter++, lane = targetLane))
            }

            // Advance notes
            val toRemove = mutableListOf<RhythmNote>()
            for (n in notes) {
                n.yProgress += 0.018f * fastForwardMultiplier
                if (n.yProgress > 1.15f) {
                    if (!n.isHit) {
                        streak = 0
                        multiplier = 1
                        feedbackText = "PERDEU"
                        feedbackColor = Color(0xFFFF453A)
                    }
                    toRemove.add(n)
                }
            }
            notes.removeAll(toRemove)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF0F051D), Color(0xFF1B0B3B), Color(0xFF05010E))
                )
            )
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            canvasWidth = size.width
            canvasHeight = size.height

            val topOrigin = Offset(size.width * 0.5f, size.height * 0.08f)
            val hitY = size.height * 0.85f

            val bottomP0 = Offset(size.width * 0.16f, hitY)
            val bottomP1 = Offset(size.width * 0.50f, hitY)
            val bottomP2 = Offset(size.width * 0.84f, hitY)

            // Draw Perspective Runway Tracks
            drawLine(Color(0x55BF5AF2), topOrigin, Offset(size.width * 0.02f, size.height), strokeWidth = 2f)
            drawLine(Color(0x55BF5AF2), topOrigin, Offset(size.width * 0.33f, size.height), strokeWidth = 2f)
            drawLine(Color(0x55BF5AF2), topOrigin, Offset(size.width * 0.67f, size.height), strokeWidth = 2f)
            drawLine(Color(0x55BF5AF2), topOrigin, Offset(size.width * 0.98f, size.height), strokeWidth = 2f)

            // Hit zone target bar
            drawLine(Color(0xAAFFFFFF), Offset(20f, hitY), Offset(size.width - 20f, hitY), strokeWidth = 4f)

            // Draw target hit circles
            val targetRadii = 36f
            val laneColors = listOf(Color(0xFFFF375F), Color(0xFF30D158), Color(0xFF0A84FF))

            listOf(bottomP0, bottomP1, bottomP2).forEachIndexed { idx, pt ->
                drawCircle(laneColors[idx].copy(alpha = 0.35f), radius = targetRadii, center = pt)
                drawCircle(laneColors[idx], radius = targetRadii, center = pt, style = Stroke(width = 4f))
            }

            // Draw descending rhythm notes
            for (n in notes) {
                if (n.isHit) continue
                val laneTarget = when (n.lane) {
                    0 -> bottomP0
                    1 -> bottomP1
                    else -> bottomP2
                }

                val currX = topOrigin.x + (laneTarget.x - topOrigin.x) * n.yProgress
                val currY = topOrigin.y + (laneTarget.y - topOrigin.y) * n.yProgress
                val noteRadius = 14f + n.yProgress * 22f

                val nColor = laneColors[n.lane]
                drawCircle(nColor, radius = noteRadius, center = Offset(currX, currY))
                drawCircle(Color.White, radius = noteRadius * 0.45f, center = Offset(currX, currY))
            }
        }

        // Header Stats
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, start = 16.dp, end = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
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
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }

                Surface(
                    color = if (multiplier >= 4) Color(0xFFBF5AF2) else Color(0xFF0A84FF),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "REVENGE ${multiplier}X",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }

            // Feedback Banner (PERFEITO, BOM, etc.)
            Text(
                text = feedbackText,
                color = feedbackColor,
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier.padding(top = 8.dp)
            )
            Text(
                text = "Sequência: $streak",
                color = Color(0xFFA1A1AA),
                fontSize = 13.sp
            )
        }

        // 3 Giant Interactive Hit Pads at Bottom
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(130.dp)
                .padding(bottom = 12.dp, start = 12.dp, end = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize()
                    .background(Color(0x33FF375F), shape = RoundedCornerShape(16.dp))
                    .clickable { handleLaneTap(0) },
                contentAlignment = Alignment.Center
            ) {
                Text("TAP 1", color = Color.White.copy(alpha = 0.85f), fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize()
                    .background(Color(0x3330D158), shape = RoundedCornerShape(16.dp))
                    .clickable { handleLaneTap(1) },
                contentAlignment = Alignment.Center
            ) {
                Text("TAP 2", color = Color.White.copy(alpha = 0.85f), fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize()
                    .background(Color(0x330A84FF), shape = RoundedCornerShape(16.dp))
                    .clickable { handleLaneTap(2) },
                contentAlignment = Alignment.Center
            ) {
                Text("TAP 3", color = Color.White.copy(alpha = 0.85f), fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }
}
