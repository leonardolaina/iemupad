package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Games
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.outlined.Games
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.AppTab
import com.example.ui.MainViewModel
import com.example.ui.components.AppleStoreDetailSheet
import com.example.ui.components.ImportIpaDialog
import com.example.ui.components.SensorCalibrationDialog
import com.example.ui.screens.EmulatorScreen
import com.example.ui.screens.ExploreScreen
import com.example.ui.screens.GameDetailSheet
import com.example.ui.screens.LibraryScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.DeepSlate
import com.example.ui.theme.IosBlue
import com.example.ui.theme.MidnightBlack
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.SurfaceBorder
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val viewModel: MainViewModel = viewModel()
                val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
                val allGames by viewModel.allGames.collectAsStateWithLifecycle()
                val installedGames by viewModel.installedGames.collectAsStateWithLifecycle()
                val selectedGameForDetail by viewModel.selectedGameForDetail.collectAsStateWithLifecycle()
                val activeEmulatedGame by viewModel.activeEmulatedGame.collectAsStateWithLifecycle()
                val isImportDialogOpen by viewModel.isImportDialogOpen.collectAsStateWithLifecycle()
                val currentSaveStates by viewModel.currentSaveStates.collectAsStateWithLifecycle()

                val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

                val allSaveStates by viewModel.allSaveStates.collectAsStateWithLifecycle()
                val appleStoreResults by viewModel.appleStoreResults.collectAsStateWithLifecycle()
                val isSearchingStore by viewModel.isSearchingStore.collectAsStateWithLifecycle()
                val selectedAppleApp by viewModel.selectedAppleApp.collectAsStateWithLifecycle()
                val showCalibrationDialog by viewModel.showCalibrationDialog.collectAsStateWithLifecycle()
                val motionSensorState by viewModel.motionSensorState.collectAsStateWithLifecycle()

                val appleSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

                // If currently playing in emulator, show EmulatorScreen in full immersion
                if (activeEmulatedGame != null) {
                    EmulatorScreen(
                        game = activeEmulatedGame!!,
                        saveStates = currentSaveStates,
                        tiltSensorManager = viewModel.tiltSensorManager,
                        soundEffects = viewModel.soundEffects,
                        onSaveState = { slot, label, score, lvl ->
                            viewModel.saveGameState(slot, label, score, lvl)
                        },
                        onLoadState = { state ->
                            viewModel.loadSaveState(state)
                        },
                        onDeleteSaveState = { stateId ->
                            viewModel.deleteSaveState(stateId)
                        },
                        onRenameSaveState = { stateId, newLabel ->
                            viewModel.renameSaveState(stateId, newLabel)
                        },
                        onExitEmulator = {
                            viewModel.exitEmulator()
                        }
                    )
                } else {
                    Scaffold(
                        contentWindowInsets = WindowInsets.safeDrawing,
                        containerColor = MidnightBlack,
                        bottomBar = {
                            NavigationBar(
                                containerColor = DeepSlate,
                                tonalElevation = 8.dp,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                                    .testTag("bottom_nav_bar")
                            ) {
                                NavigationBarItem(
                                    selected = currentTab == AppTab.EXPLORE,
                                    onClick = { viewModel.setTab(AppTab.EXPLORE) },
                                    icon = {
                                        Icon(
                                            imageVector = if (currentTab == AppTab.EXPLORE) Icons.Filled.Storefront else Icons.Outlined.Storefront,
                                            contentDescription = "iStore"
                                        )
                                    },
                                    label = {
                                        Text(
                                            "iStore",
                                            fontSize = 12.sp,
                                            fontWeight = if (currentTab == AppTab.EXPLORE) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = Color.White,
                                        selectedTextColor = IosBlue,
                                        indicatorColor = IosBlue,
                                        unselectedIconColor = TextSecondary,
                                        unselectedTextColor = TextSecondary
                                    ),
                                    modifier = Modifier.testTag("tab_explore")
                                )

                                NavigationBarItem(
                                    selected = currentTab == AppTab.LIBRARY,
                                    onClick = { viewModel.setTab(AppTab.LIBRARY) },
                                    icon = {
                                        Icon(
                                            imageVector = if (currentTab == AppTab.LIBRARY) Icons.Filled.Games else Icons.Outlined.Games,
                                            contentDescription = "Biblioteca"
                                        )
                                    },
                                    label = {
                                        Text(
                                            "Biblioteca",
                                            fontSize = 12.sp,
                                            fontWeight = if (currentTab == AppTab.LIBRARY) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = Color.White,
                                        selectedTextColor = IosBlue,
                                        indicatorColor = IosBlue,
                                        unselectedIconColor = TextSecondary,
                                        unselectedTextColor = TextSecondary
                                    ),
                                    modifier = Modifier.testTag("tab_library")
                                )

                                NavigationBarItem(
                                    selected = currentTab == AppTab.SETTINGS,
                                    onClick = { viewModel.setTab(AppTab.SETTINGS) },
                                    icon = {
                                        Icon(
                                            imageVector = if (currentTab == AppTab.SETTINGS) Icons.Filled.Settings else Icons.Outlined.Settings,
                                            contentDescription = "Ajustes"
                                        )
                                    },
                                    label = {
                                        Text(
                                            "Ajustes",
                                            fontSize = 12.sp,
                                            fontWeight = if (currentTab == AppTab.SETTINGS) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = Color.White,
                                        selectedTextColor = IosBlue,
                                        indicatorColor = IosBlue,
                                        unselectedIconColor = TextSecondary,
                                        unselectedTextColor = TextSecondary
                                    ),
                                    modifier = Modifier.testTag("tab_settings")
                                )
                            }
                        }
                    ) { innerPadding ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        ) {
                            when (currentTab) {
                                AppTab.EXPLORE -> {
                                    ExploreScreen(
                                        games = allGames,
                                        appleStoreApps = appleStoreResults,
                                        isSearchingStore = isSearchingStore,
                                        onSearchAppleStore = { viewModel.searchAppleStore(it) },
                                        onSelectAppleApp = { viewModel.openAppleAppDetail(it) },
                                        onGameSelected = { viewModel.openGameDetail(it) },
                                        onDownloadGame = { viewModel.startDownload(it) },
                                        onPlayGame = { viewModel.launchEmulator(it) },
                                        onOpenImportIpa = { viewModel.openImportDialog() },
                                        onOpenCalibration = { viewModel.openCalibrationDialog() }
                                    )
                                }
                                AppTab.LIBRARY -> {
                                    LibraryScreen(
                                        installedGames = installedGames,
                                        allSaveStates = allSaveStates,
                                        onGameSelected = { viewModel.openGameDetail(it) },
                                        onPlayGame = { viewModel.launchEmulator(it) },
                                        onLoadSaveState = { state ->
                                            val targetGame = allGames.firstOrNull { it.id == state.gameId }
                                            if (targetGame != null) {
                                                viewModel.launchEmulator(targetGame)
                                                viewModel.loadSaveState(state)
                                            }
                                        },
                                        onDeleteSaveState = { viewModel.deleteSaveState(it) },
                                        onOpenImportIpa = { viewModel.openImportDialog() },
                                        onNavigateExplore = { viewModel.setTab(AppTab.EXPLORE) }
                                    )
                                }
                                AppTab.SETTINGS -> {
                                    SettingsScreen()
                                }
                            }
                        }

                        // Detail Sheet
                        if (selectedGameForDetail != null) {
                            GameDetailSheet(
                                game = selectedGameForDetail,
                                sheetState = sheetState,
                                onDismiss = { viewModel.closeGameDetail() },
                                onPlay = { viewModel.launchEmulator(it) },
                                onDownload = { viewModel.startDownload(it) },
                                onUninstall = { viewModel.uninstallGame(it) }
                            )
                        }

                        // Import IPA Dialog
                        if (isImportDialogOpen) {
                            ImportIpaDialog(
                                onDismiss = { viewModel.closeImportDialog() },
                                onImportSuccess = { fileName, customTitle ->
                                    viewModel.importIpa(fileName, customTitle)
                                }
                            )
                        }

                        // Apple Store Detail & Version Downloader Sheet
                        if (selectedAppleApp != null) {
                            AppleStoreDetailSheet(
                                app = selectedAppleApp,
                                sheetState = appleSheetState,
                                onDismiss = { viewModel.closeAppleAppDetail() },
                                onOpenStoreLink = { viewModel.openInOfficialStore(this@MainActivity, it) },
                                onDownloadVersion = { app, version ->
                                    viewModel.downloadAppleStoreVersion(app, version)
                                }
                            )
                        }

                        // Motion Sensor Calibration & Bubble Level Dialog
                        if (showCalibrationDialog) {
                            SensorCalibrationDialog(
                                motionState = motionSensorState,
                                onDismiss = { viewModel.closeCalibrationDialog() }
                            )
                        }
                    }
                }
            }
        }
    }
}
