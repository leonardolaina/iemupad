package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import com.example.ui.components.SaveStateManagerSheet
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.audio.SoundEffects
import com.example.data.model.GameEntity
import com.example.data.model.SaveStateEntity
import com.example.sensor.TiltSensorManager
import com.example.ui.components.ClassicHomeButton
import com.example.ui.components.EmulatorToolBar
import com.example.ui.components.IosStatusBar
import com.example.ui.games.CutRopePhysicsGame
import com.example.ui.games.DoodleJumpGame
import com.example.ui.games.FlightControlGame
import com.example.ui.games.FruitSlicerGame
import com.example.ui.games.GenericIpaRunner
import com.example.ui.games.RetroRacingGame
import com.example.ui.games.TapTapRevengeGame
import com.example.ui.games.TempleRunnerGame
import com.example.ui.theme.DeepSlate
import com.example.ui.theme.IosBlue
import com.example.ui.theme.IosMint
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmulatorScreen(
    game: GameEntity,
    saveStates: List<SaveStateEntity>,
    tiltSensorManager: TiltSensorManager,
    soundEffects: SoundEffects,
    onSaveState: (slot: Int, label: String, score: Int, inGameLevel: String) -> Unit,
    onLoadState: (SaveStateEntity) -> Unit,
    onDeleteSaveState: (Long) -> Unit,
    onRenameSaveState: (Long, String) -> Unit,
    onExitEmulator: () -> Unit
) {
    BackHandler {
        onExitEmulator()
    }

    DisposableEffect(Unit) {
        tiltSensorManager.startListening()
        onDispose {
            tiltSensorManager.stopListening()
        }
    }

    val tiltData by tiltSensorManager.tiltFlow.collectAsStateWithLifecycle()

    var speedMultiplier by remember { mutableFloatStateOf(1f) }
    var isFpsOverlayVisible by remember { mutableStateOf(false) }
    var isVirtualGamepadVisible by remember { mutableStateOf(false) }
    var currentScore by remember { mutableIntStateOf(0) }
    var currentLevel by remember { mutableStateOf("Fase 1 - Circuito Principal") }

    var showSaveSheet by remember { mutableStateOf(false) }
    val saveSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var loadedStateMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(loadedStateMessage) {
        if (loadedStateMessage != null) {
            delay(3000)
            loadedStateMessage = null
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // iOS Top Status Bar
            IosStatusBar()

            // Emulator floating HUD bar
            EmulatorToolBar(
                fps = 60.0f,
                speedMultiplier = speedMultiplier,
                isFpsOverlayVisible = isFpsOverlayVisible,
                isVirtualGamepadVisible = isVirtualGamepadVisible,
                savedStatesCount = saveStates.size,
                onSpeedCycle = {
                    speedMultiplier = when (speedMultiplier) {
                        1f -> 2f
                        2f -> 4f
                        else -> 1f
                    }
                    soundEffects.vibrate(15)
                },
                onSaveState = {
                    showSaveSheet = true
                },
                onToggleFps = {
                    isFpsOverlayVisible = !isFpsOverlayVisible
                },
                onToggleGamepad = {
                    isVirtualGamepadVisible = !isVirtualGamepadVisible
                },
                onExitEmulator = onExitEmulator,
                modifier = Modifier.fillMaxWidth()
            )

            // Game Emulation View Area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
            ) {
                when {
                    game.category == "Corrida" || game.id == "com.firemint.realracing2" || game.id == "com.gameloft.asphalt6" || game.id == "com.ea.needforspeed" -> {
                        RetroRacingGame(
                            gameTitle = game.title,
                            tiltX = tiltData.tiltX,
                            soundEffects = soundEffects,
                            fastForwardMultiplier = speedMultiplier,
                            onScoreUpdated = { currentScore = it }
                        )
                    }
                    game.category == "Runner" || game.id == "com.imangi.templerun" || game.id == "com.halfbrick.jetpack" -> {
                        TempleRunnerGame(
                            gameTitle = game.title,
                            tiltX = tiltData.tiltX,
                            soundEffects = soundEffects,
                            fastForwardMultiplier = speedMultiplier,
                            onScoreUpdated = { currentScore = it }
                        )
                    }
                    game.category == "Puzzle" || game.id == "com.zeptolab.cuttherope" -> {
                        CutRopePhysicsGame(
                            gameTitle = game.title,
                            soundEffects = soundEffects,
                            fastForwardMultiplier = speedMultiplier,
                            onScoreUpdated = { currentScore = it }
                        )
                    }
                    game.id == "com.firemint.flightcontrol" -> {
                        FlightControlGame(
                            soundEffects = soundEffects,
                            fastForwardMultiplier = speedMultiplier,
                            onScoreUpdated = { currentScore = it }
                        )
                    }
                    game.id == "com.limasky.doodlejump" -> {
                        DoodleJumpGame(
                            tiltX = tiltData.tiltX,
                            soundEffects = soundEffects,
                            fastForwardMultiplier = speedMultiplier,
                            onScoreUpdated = { currentScore = it }
                        )
                    }
                    game.id == "com.taptap.revenge4" -> {
                        TapTapRevengeGame(
                            soundEffects = soundEffects,
                            fastForwardMultiplier = speedMultiplier,
                            onScoreUpdated = { currentScore = it }
                        )
                    }
                    game.id == "com.halfbrick.fruitninja" -> {
                        FruitSlicerGame(
                            soundEffects = soundEffects,
                            fastForwardMultiplier = speedMultiplier,
                            onScoreUpdated = { currentScore = it }
                        )
                    }
                    else -> {
                        GenericIpaRunner(
                            gameTitle = game.title,
                            tiltX = tiltData.tiltX,
                            tiltY = tiltData.tiltY,
                            soundEffects = soundEffects,
                            fastForwardMultiplier = speedMultiplier,
                            onScoreUpdated = { currentScore = it }
                        )
                    }
                }

                // Optional Telemetry / HUD Overlay
                if (isFpsOverlayVisible) {
                    Surface(
                        color = Color.Black.copy(alpha = 0.85f),
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155)),
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("touchHLE JIT v0.2.3", color = IosMint, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                            Text("FPS: 60.0 • V-Sync: ON", color = Color.White, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                            Text("CPU: ARM Cortex-A8 (Dynarmic)", color = Color(0xFF94A3B8), fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                            Text("Shaders: Metal->Vulkan 1.2", color = Color(0xFF64D2FF), fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                            Text("Mem: 42 MB / 512 MB Heap", color = Color(0xFFA1A1AA), fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                            Text("Sensor: TiltX=%.2f TiltY=%.2f".format(tiltData.tiltX, tiltData.tiltY), color = Color(0xFFFFD60A), fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                        }
                    }
                }

                // Virtual Gamepad Overlay (if activated)
                if (isVirtualGamepadVisible) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        // Virtual D-Pad
                        Box(
                            modifier = Modifier
                                .size(130.dp)
                                .align(Alignment.BottomStart)
                                .background(Color.Black.copy(alpha = 0.45f), CircleShape)
                        ) {
                            Icon(
                                Icons.Default.KeyboardArrowUp,
                                contentDescription = "Cima",
                                tint = Color.White,
                                modifier = Modifier
                                    .align(Alignment.TopCenter)
                                    .size(36.dp)
                                    .clickable { soundEffects.vibrate(10) }
                            )
                            Icon(
                                Icons.Default.KeyboardArrowDown,
                                contentDescription = "Baixo",
                                tint = Color.White,
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .size(36.dp)
                                    .clickable { soundEffects.vibrate(10) }
                            )
                            Icon(
                                Icons.Default.KeyboardArrowLeft,
                                contentDescription = "Esquerda",
                                tint = Color.White,
                                modifier = Modifier
                                    .align(Alignment.CenterStart)
                                    .size(36.dp)
                                    .clickable { soundEffects.vibrate(10) }
                            )
                            Icon(
                                Icons.Default.KeyboardArrowRight,
                                contentDescription = "Direita",
                                tint = Color.White,
                                modifier = Modifier
                                    .align(Alignment.CenterEnd)
                                    .size(36.dp)
                                    .clickable { soundEffects.vibrate(10) }
                            )
                        }

                        // Virtual Action Buttons A and B
                        Row(
                            modifier = Modifier.align(Alignment.BottomEnd),
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF0A84FF).copy(alpha = 0.65f))
                                    .clickable {
                                        soundEffects.playHit()
                                        soundEffects.vibrate(15)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text("A", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                            }

                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFFF375F).copy(alpha = 0.65f))
                                    .clickable {
                                        soundEffects.playJump()
                                        soundEffects.vibrate(15)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text("B", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                            }
                        }
                    }
                }
            }

            // Classic iPhone Hardware Home Button (exit emulator)
            ClassicHomeButton(onClick = onExitEmulator)
        }

        // Notification banner when a Save State is restored/loaded
        if (loadedStateMessage != null) {
            Surface(
                color = Color(0xFF1E293B).copy(alpha = 0.95f),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, IosMint),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 40.dp, start = 20.dp, end = 20.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Restore, contentDescription = null, tint = IosMint, modifier = Modifier.size(20.dp))
                    Text(
                        text = loadedStateMessage!!,
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Comprehensive Save State Manager Sheet (List & Create)
        if (showSaveSheet) {
            SaveStateManagerSheet(
                gameTitle = game.title,
                saveStates = saveStates,
                currentScore = currentScore,
                currentLevel = currentLevel,
                sheetState = saveSheetState,
                onDismiss = { showSaveSheet = false },
                onSaveState = { slot, label, score, lvl ->
                    onSaveState(slot, label, score, lvl)
                    loadedStateMessage = "Progresso salvo no Slot $slot com sucesso!"
                },
                onLoadState = { state ->
                    currentScore = state.score
                    currentLevel = state.inGameLevel
                    onLoadState(state)
                    loadedStateMessage = "Estado Restaurado: ${state.label} (${state.score} pts)!"
                },
                onDeleteSaveState = { stateId ->
                    onDeleteSaveState(stateId)
                },
                onRenameSaveState = { stateId, newLabel ->
                    onRenameSaveState(stateId, newLabel)
                }
            )
        }
    }
}
