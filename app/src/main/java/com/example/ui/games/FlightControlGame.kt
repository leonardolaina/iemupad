package com.example.ui.games

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

enum class AircraftType(val color: Color, val speed: Float, val targetType: String) {
    JET(Color(0xFFFF3B30), 2.2f, "RUNWAY_A"),
    BIPLANE(Color(0xFFFFCC00), 1.4f, "RUNWAY_B"),
    HELICOPTER(Color(0xFF007AFF), 1.1f, "HELIPAD")
}

data class Aircraft(
    val id: Int,
    var x: Float,
    var y: Float,
    var heading: Float,
    val type: AircraftType,
    val path: MutableList<Offset> = mutableListOf(),
    var isLanding: Boolean = false,
    var landingProgress: Float = 0f
)

@Composable
fun FlightControlGame(
    soundEffects: SoundEffects,
    fastForwardMultiplier: Float = 1f,
    onScoreUpdated: (Int) -> Unit = {}
) {
    var score by remember { mutableIntStateOf(0) }
    var gameOver by remember { mutableStateOf(false) }
    val aircraftList = remember { mutableStateListOf<Aircraft>() }
    var nextId by remember { mutableIntStateOf(1) }
    var activeAircraftId by remember { mutableStateOf<Int?>(null) }
    var canvasWidth by remember { mutableFloatStateOf(800f) }
    var canvasHeight by remember { mutableFloatStateOf(1200f) }

    // Game loop
    LaunchedEffect(gameOver, fastForwardMultiplier) {
        if (gameOver) return@LaunchedEffect
        var spawnTimer = 0
        while (!gameOver) {
            val stepTime = (30 / fastForwardMultiplier).toLong().coerceAtLeast(10)
            delay(stepTime)

            // Spawn planes
            spawnTimer++
            if (spawnTimer > 110 && aircraftList.size < 7) {
                spawnTimer = 0
                val edge = Random.nextInt(4)
                val type = AircraftType.values().random()
                val (sx, sy, initHeading) = when (edge) {
                    0 -> Triple(Random.nextFloat() * canvasWidth, 10f, Math.PI.toFloat() / 2f)
                    1 -> Triple(canvasWidth - 10f, Random.nextFloat() * canvasHeight, Math.PI.toFloat())
                    2 -> Triple(Random.nextFloat() * canvasWidth, canvasHeight - 10f, -Math.PI.toFloat() / 2f)
                    else -> Triple(10f, Random.nextFloat() * canvasHeight, 0f)
                }
                aircraftList.add(Aircraft(nextId++, sx, sy, initHeading, type))
            }

            // Update aircraft positions
            val toRemove = mutableListOf<Aircraft>()
            for (craft in aircraftList) {
                if (craft.isLanding) {
                    craft.landingProgress += 0.05f
                    if (craft.landingProgress >= 1f) {
                        toRemove.add(craft)
                        score++
                        onScoreUpdated(score)
                        soundEffects.playLand()
                    }
                    continue
                }

                if (craft.path.isNotEmpty()) {
                    val target = craft.path.first()
                    val dx = target.x - craft.x
                    val dy = target.y - craft.y
                    val dist = sqrt(dx * dx + dy * dy)
                    if (dist < 12f) {
                        craft.path.removeAt(0)
                    } else {
                        craft.heading = atan2(dy, dx)
                        craft.x += cos(craft.heading) * craft.type.speed * fastForwardMultiplier
                        craft.y += sin(craft.heading) * craft.type.speed * fastForwardMultiplier
                    }
                } else {
                    craft.x += cos(craft.heading) * craft.type.speed * fastForwardMultiplier
                    craft.y += sin(craft.heading) * craft.type.speed * fastForwardMultiplier
                }

                // Bounce off edges if no path
                if (craft.x < 20f || craft.x > canvasWidth - 20f) {
                    craft.heading = Math.PI.toFloat() - craft.heading
                }
                if (craft.y < 20f || craft.y > canvasHeight - 20f) {
                    craft.heading = -craft.heading
                }

                // Check runway docking
                val inRunwayA = craft.type == AircraftType.JET &&
                    craft.x in (canvasWidth * 0.15f)..(canvasWidth * 0.45f) &&
                    craft.y in (canvasHeight * 0.4f)..(canvasHeight * 0.6f)
                val inRunwayB = craft.type == AircraftType.BIPLANE &&
                    craft.x in (canvasWidth * 0.55f)..(canvasWidth * 0.85f) &&
                    craft.y in (canvasHeight * 0.6f)..(canvasHeight * 0.8f)
                val inHelipad = craft.type == AircraftType.HELICOPTER &&
                    sqrt((craft.x - canvasWidth * 0.5f) * (craft.x - canvasWidth * 0.5f) +
                        (craft.y - canvasHeight * 0.25f) * (craft.y - canvasHeight * 0.25f)) < 40f

                if ((inRunwayA || inRunwayB || inHelipad) && !craft.isLanding) {
                    craft.isLanding = true
                    craft.path.clear()
                }
            }

            aircraftList.removeAll(toRemove)

            // Collision check
            for (i in 0 until aircraftList.size) {
                val a = aircraftList[i]
                if (a.isLanding) continue
                for (j in i + 1 until aircraftList.size) {
                    val b = aircraftList[j]
                    if (b.isLanding) continue
                    val dx = a.x - b.x
                    val dy = a.y - b.y
                    val dist = sqrt(dx * dx + dy * dy)
                    if (dist < 32f) {
                        gameOver = true
                        soundEffects.playBoom()
                        break
                    }
                }
                if (gameOver) break
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF2E6F40)) // Airfield grassy green
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            val touched = aircraftList.firstOrNull { craft ->
                                val dx = craft.x - offset.x
                                val dy = craft.y - offset.y
                                sqrt(dx * dx + dy * dy) < 55f
                            }
                            if (touched != null && !touched.isLanding) {
                                activeAircraftId = touched.id
                                touched.path.clear()
                                touched.path.add(offset)
                                soundEffects.vibrate(15)
                            }
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            val craft = aircraftList.firstOrNull { it.id == activeAircraftId }
                            if (craft != null && !craft.isLanding) {
                                craft.path.add(change.position)
                            }
                        },
                        onDragEnd = { activeAircraftId = null },
                        onDragCancel = { activeAircraftId = null }
                    )
                }
        ) {
            canvasWidth = size.width
            canvasHeight = size.height

            // Draw Runway A (Jet tarmac)
            val rAW = size.width * 0.32f
            val rAH = size.height * 0.22f
            val rAX = size.width * 0.15f
            val rAY = size.height * 0.4f
            drawRect(Color(0xFF333333), Offset(rAX, rAY), Size(rAW, rAH))
            drawRect(Color(0xFFFF3B30), Offset(rAX, rAY), Size(rAW, rAH), style = Stroke(width = 3f))

            // Runway B (Biplane tarmac)
            val rBW = size.width * 0.32f
            val rBH = size.height * 0.22f
            val rBX = size.width * 0.55f
            val rBY = size.height * 0.6f
            drawRect(Color(0xFF3A3A3C), Offset(rBX, rBY), Size(rBW, rBH))
            drawRect(Color(0xFFFFCC00), Offset(rBX, rBY), Size(rBW, rBH), style = Stroke(width = 3f))

            // Helipad (Blue circle with H)
            val hCenter = Offset(size.width * 0.5f, size.height * 0.25f)
            drawCircle(Color(0xFF1C1C1E), radius = 42f, center = hCenter)
            drawCircle(Color(0xFF007AFF), radius = 42f, center = hCenter, style = Stroke(width = 4f))

            // Draw Paths
            for (craft in aircraftList) {
                if (craft.path.size > 1) {
                    val p = Path()
                    p.moveTo(craft.x, craft.y)
                    for (pt in craft.path) {
                        p.lineTo(pt.x, pt.y)
                    }
                    drawPath(
                        path = p,
                        color = craft.type.color.copy(alpha = 0.75f),
                        style = Stroke(width = 4f)
                    )
                }
            }

            // Draw Aircraft
            for (craft in aircraftList) {
                val scale = if (craft.isLanding) (1f - craft.landingProgress * 0.5f) else 1f
                val radius = 18f * scale

                // Danger warning circle if planes are close
                val isClose = aircraftList.any { other ->
                    other.id != craft.id && !other.isLanding &&
                        sqrt((craft.x - other.x) * (craft.x - other.x) + (craft.y - other.y) * (craft.y - other.y)) < 65f
                }

                if (isClose) {
                    drawCircle(
                        color = Color.Red.copy(alpha = 0.6f),
                        radius = 35f,
                        center = Offset(craft.x, craft.y),
                        style = Stroke(width = 2.5f)
                    )
                }

                drawCircle(craft.type.color, radius = radius, center = Offset(craft.x, craft.y))
                drawCircle(Color.White, radius = radius * 0.45f, center = Offset(craft.x, craft.y))

                // Nose direction indicator
                val noseX = craft.x + cos(craft.heading) * radius * 1.6f
                val noseY = craft.y + sin(craft.heading) * radius * 1.6f
                drawLine(
                    color = Color.White,
                    start = Offset(craft.x, craft.y),
                    end = Offset(noseX, noseY),
                    strokeWidth = 3f
                )
            }
        }

        // HUD overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text(
                text = "Pousos: $score",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .background(Color.Black.copy(alpha = 0.6f), shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            )

            if (gameOver) {
                androidx.compose.material3.Surface(
                    color = Color.Black.copy(alpha = 0.85f),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                    modifier = Modifier.align(Alignment.Center)
                ) {
                    androidx.compose.foundation.layout.Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("COLISÃO AÉREA!", color = Color(0xFFFF3B30), fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        Text("Aviões pousados: $score", color = Color.White, fontSize = 18.sp, modifier = Modifier.padding(top = 8.dp))
                        androidx.compose.material3.Button(
                            onClick = {
                                aircraftList.clear()
                                score = 0
                                gameOver = false
                            },
                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = Color(0xFF007AFF)),
                            modifier = Modifier.padding(top = 16.dp)
                        ) {
                            Text("Jogar Novamente")
                        }
                    }
                }
            }
        }
    }
}
