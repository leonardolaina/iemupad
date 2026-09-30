package com.example.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.SoundEffects
import com.example.data.db.AppDatabase
import com.example.data.model.AppleAppVersion
import com.example.data.model.AppleStoreApp
import com.example.data.model.GameEntity
import com.example.data.model.SaveStateEntity
import com.example.data.remote.AppleStoreApiService
import com.example.data.repository.AppleStoreRepository
import com.example.data.repository.GameRepository
import com.example.sensor.MotionSensorService
import com.example.sensor.MotionTiltState
import com.example.sensor.TiltSensorManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class AppTab {
    EXPLORE,
    LIBRARY,
    SETTINGS
}

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getInstance(application)
    private val repository = GameRepository(database.gameDao(), database.saveStateDao())
    private val appleStoreRepo = AppleStoreRepository(AppleStoreApiService(), database.gameDao())

    val tiltSensorManager = TiltSensorManager(application)
    val soundEffects = SoundEffects(application)

    // MotionSensorService state flow
    val motionSensorState: StateFlow<MotionTiltState> = MotionSensorService.motionState

    val allGames: StateFlow<List<GameEntity>> = repository.allGames
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val installedGames: StateFlow<List<GameEntity>> = repository.installedGames
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _currentTab = MutableStateFlow(AppTab.EXPLORE)
    val currentTab: StateFlow<AppTab> = _currentTab.asStateFlow()

    private val _selectedGameForDetail = MutableStateFlow<GameEntity?>(null)
    val selectedGameForDetail: StateFlow<GameEntity?> = _selectedGameForDetail.asStateFlow()

    private val _selectedAppleApp = MutableStateFlow<AppleStoreApp?>(null)
    val selectedAppleApp: StateFlow<AppleStoreApp?> = _selectedAppleApp.asStateFlow()

    private val _appleStoreResults = MutableStateFlow<List<AppleStoreApp>>(emptyList())
    val appleStoreResults: StateFlow<List<AppleStoreApp>> = _appleStoreResults.asStateFlow()

    private val _isSearchingStore = MutableStateFlow(false)
    val isSearchingStore: StateFlow<Boolean> = _isSearchingStore.asStateFlow()

    private val _showCalibrationDialog = MutableStateFlow(false)
    val showCalibrationDialog: StateFlow<Boolean> = _showCalibrationDialog.asStateFlow()

    private val _activeEmulatedGame = MutableStateFlow<GameEntity?>(null)
    val activeEmulatedGame: StateFlow<GameEntity?> = _activeEmulatedGame.asStateFlow()

    private val _isImportDialogOpen = MutableStateFlow(false)
    val isImportDialogOpen: StateFlow<Boolean> = _isImportDialogOpen.asStateFlow()

    val allSaveStates: StateFlow<List<SaveStateEntity>> = repository.allSaveStates
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _loadedStateEvent = MutableStateFlow<SaveStateEntity?>(null)
    val loadedStateEvent: StateFlow<SaveStateEntity?> = _loadedStateEvent.asStateFlow()

    val currentSaveStates: StateFlow<List<SaveStateEntity>> = _activeEmulatedGame
        .flatMapLatest { game ->
            if (game != null) repository.getSaveStates(game.id) else flowOf(emptyList())
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        // Start background Android MotionSensorService for device tilt mapping
        MotionSensorService.startService(application)

        viewModelScope.launch {
            repository.initializeCatalogIfEmpty()
            // Initial Apple Store live query
            searchAppleStore("classic games")
        }
    }

    fun setTab(tab: AppTab) {
        _currentTab.value = tab
    }

    fun openGameDetail(game: GameEntity) {
        _selectedGameForDetail.value = game
    }

    fun closeGameDetail() {
        _selectedGameForDetail.value = null
    }

    fun openAppleAppDetail(app: AppleStoreApp) {
        _selectedAppleApp.value = app
    }

    fun closeAppleAppDetail() {
        _selectedAppleApp.value = null
    }

    fun openCalibrationDialog() {
        _showCalibrationDialog.value = true
    }

    fun closeCalibrationDialog() {
        _showCalibrationDialog.value = false
    }

    fun searchAppleStore(query: String) {
        viewModelScope.launch {
            _isSearchingStore.value = true
            appleStoreRepo.searchAppleStore(query.ifEmpty { "racing games" }).collect { results ->
                _appleStoreResults.value = results
                _isSearchingStore.value = false
            }
        }
    }

    fun openInOfficialStore(context: Context, trackViewUrl: String) {
        appleStoreRepo.openInOfficialAppStore(context, trackViewUrl)
    }

    fun downloadAppleStoreVersion(app: AppleStoreApp, version: AppleAppVersion) {
        appleStoreRepo.downloadAndInstallVersion(app, version, viewModelScope)
        soundEffects.playCoin()
    }

    fun startDownload(game: GameEntity) {
        repository.startDownload(game.id, viewModelScope)
    }

    fun uninstallGame(game: GameEntity) {
        viewModelScope.launch {
            repository.uninstallGame(game.id)
        }
    }

    fun launchEmulator(game: GameEntity) {
        _selectedGameForDetail.value = null
        _selectedAppleApp.value = null
        _activeEmulatedGame.value = game
        viewModelScope.launch {
            repository.recordPlaySession(game.id, 1)
        }
    }

    fun exitEmulator() {
        _activeEmulatedGame.value = null
    }

    fun openImportDialog() {
        _isImportDialogOpen.value = true
    }

    fun closeImportDialog() {
        _isImportDialogOpen.value = false
    }

    fun importIpa(fileName: String, customTitle: String) {
        viewModelScope.launch {
            repository.importIpaFile(fileName, 45_000_000L, customTitle)
            _isImportDialogOpen.value = false
        }
    }

    fun saveGameState(slot: Int, label: String, score: Int, inGameLevel: String = "Fase 1") {
        val game = _activeEmulatedGame.value ?: return
        viewModelScope.launch {
            repository.saveGameState(
                gameId = game.id,
                gameTitle = game.title,
                slot = slot,
                label = label,
                score = score,
                progressInfo = "Ponto salvo com pontuação: $score",
                inGameLevel = inGameLevel,
                playTimeSeconds = System.currentTimeMillis() / 1000L
            )
            soundEffects.playCoin()
            soundEffects.vibrate(25)
        }
    }

    fun loadSaveState(state: SaveStateEntity) {
        _loadedStateEvent.value = state
        soundEffects.playLand()
        soundEffects.vibrate(40)
    }

    fun clearLoadedEvent() {
        _loadedStateEvent.value = null
    }

    fun renameSaveState(id: Long, newLabel: String) {
        viewModelScope.launch {
            repository.updateSaveStateLabel(id, newLabel)
        }
    }

    fun deleteSaveState(stateId: Long) {
        viewModelScope.launch {
            repository.deleteSaveState(stateId)
        }
    }

    override fun onCleared() {
        super.onCleared()
        soundEffects.release()
    }
}
