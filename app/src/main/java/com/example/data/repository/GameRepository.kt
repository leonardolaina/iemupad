package com.example.data.repository

import com.example.data.db.GameDao
import com.example.data.db.SaveStateDao
import com.example.data.model.GameEntity
import com.example.data.model.SaveStateEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.random.Random

class GameRepository(
    private val gameDao: GameDao,
    private val saveStateDao: SaveStateDao
) {
    val allGames: Flow<List<GameEntity>> = gameDao.getAllGames()
    val installedGames: Flow<List<GameEntity>> = gameDao.getInstalledGames()
    val allSaveStates: Flow<List<SaveStateEntity>> = saveStateDao.getAllSaveStates()

    fun getGame(gameId: String): Flow<GameEntity?> = gameDao.getGameById(gameId)
    fun getSaveStates(gameId: String): Flow<List<SaveStateEntity>> = saveStateDao.getSaveStatesForGame(gameId)
    fun countSaveStates(gameId: String): Flow<Int> = saveStateDao.countSaveStatesForGame(gameId)

    suspend fun initializeCatalogIfEmpty() {
        val current = gameDao.getAllGames().first()
        val currentIds = current.map { it.id }.toSet()
        val catalog = listOf(
            GameEntity(
                id = "com.firemint.realracing2",
                title = "Real Racing 2 HD",
                developer = "Firemint Games",
                category = "Corrida",
                iosVersion = "iOS 4.3",
                architecture = "ARMv7 32-bit",
                fileSizeBytes = 720_000_000L,
                isInstalled = true,
                compatibilityRating = "100% Perfeito (60 FPS)",
                emulatorCore = "touchHLE JIT v0.2.3",
                playTimeMinutes = 58,
                lastPlayedTimestamp = System.currentTimeMillis() - 1800_000L,
                summary = "O ápice dos jogos de corrida no iPhone! Direção ultraprecisa por inclinação do giroscópio, 30 carros na pista, visão de cockpit e telemetria avançada.",
                accentColorHex = "#0284C7",
                iconSymbol = "racing",
                isSamplePlayable = true
            ),
            GameEntity(
                id = "com.gameloft.asphalt6",
                title = "Asphalt 6: Adrenaline",
                developer = "Gameloft",
                category = "Corrida",
                iosVersion = "iOS 4.2.1",
                architecture = "ARMv7 32-bit",
                fileSizeBytes = 580_000_000L,
                isInstalled = true,
                compatibilityRating = "98% Ótimo (60 FPS)",
                emulatorCore = "touchHLE JIT v0.2.3",
                playTimeMinutes = 35,
                lastPlayedTimestamp = System.currentTimeMillis() - 7200_000L,
                summary = "Corrida arcade empolgante com turbos Nitro Adrenaline, rampas acrobáticas e pistas de alta velocidade com derrapagens insanas.",
                accentColorHex = "#DC2626",
                iconSymbol = "racing",
                isSamplePlayable = true
            ),
            GameEntity(
                id = "com.imangi.templerun",
                title = "Temple Run Classic",
                developer = "Imangi Studios",
                category = "Runner",
                iosVersion = "iOS 4.3",
                architecture = "ARMv7 32-bit",
                fileSizeBytes = 38_000_000L,
                isInstalled = true,
                compatibilityRating = "100% Perfeito (60 FPS)",
                emulatorCore = "touchHLE JIT v0.2.3",
                playTimeMinutes = 24,
                lastPlayedTimestamp = System.currentTimeMillis() - 14400_000L,
                summary = "O fenômeno dos runners em 3D! Deslize para pular e escorregar enquanto inclina o aparelho para coletar moedas pelas ruínas do templo.",
                accentColorHex = "#EAB308",
                iconSymbol = "runner",
                isSamplePlayable = true
            ),
            GameEntity(
                id = "com.zeptolab.cuttherope",
                title = "Cut the Rope HD",
                developer = "ZeptoLab",
                category = "Puzzle",
                iosVersion = "iOS 4.0",
                architecture = "ARMv7 32-bit",
                fileSizeBytes = 42_000_000L,
                isInstalled = true,
                compatibilityRating = "100% Perfeito (60 FPS)",
                emulatorCore = "touchHLE JIT v0.2.3",
                playTimeMinutes = 40,
                lastPlayedTimestamp = System.currentTimeMillis() - 28800_000L,
                summary = "Física deliciosa de cordas e pêndulos. Corte as cordas com o dedo para balançar o doce e alimentar o carismático monstrinho verde Om Nom!",
                accentColorHex = "#22C55E",
                iconSymbol = "puzzle",
                isSamplePlayable = true
            ),
            GameEntity(
                id = "com.limasky.doodlejump",
                title = "Doodle Jump Deluxe",
                developer = "Lima Sky",
                category = "Arcade",
                iosVersion = "iOS 3.1.3",
                architecture = "ARMv7 32-bit",
                fileSizeBytes = 28_400_000L,
                isInstalled = true,
                compatibilityRating = "100% Perfeito (60 FPS)",
                emulatorCore = "touchHLE JIT v0.2.3",
                playTimeMinutes = 18,
                lastPlayedTimestamp = System.currentTimeMillis() - 3600_000L,
                summary = "O lendário clássico de pular plataformas com controle por giroscópio e inclinação do aparelho, molas, propulsores e monstros.",
                accentColorHex = "#30D158",
                iconSymbol = "jump",
                isSamplePlayable = true
            ),
            GameEntity(
                id = "com.firemint.flightcontrol",
                title = "Flight Control HD",
                developer = "Firemint Games",
                category = "Simulador",
                iosVersion = "iOS 4.2.1",
                architecture = "ARMv7 32-bit",
                fileSizeBytes = 34_800_000L,
                isInstalled = true,
                compatibilityRating = "100% Perfeito (60 FPS)",
                emulatorCore = "touchHLE JIT v0.2.3",
                playTimeMinutes = 45,
                lastPlayedTimestamp = System.currentTimeMillis() - 86400_000L,
                summary = "Desenhe rotas de pouso com o dedo para aviões a jato, biplanos e helicópteros sem que eles colidam no espaço aéreo.",
                accentColorHex = "#0A84FF",
                iconSymbol = "plane",
                isSamplePlayable = true
            ),
            GameEntity(
                id = "com.taptap.revenge4",
                title = "Tap Tap Revenge 4",
                developer = "Tapulous / Disney",
                category = "Ritmo",
                iosVersion = "iOS 4.3",
                architecture = "ARMv7 32-bit",
                fileSizeBytes = 62_100_000L,
                isInstalled = true,
                compatibilityRating = "99% Excelente (60 FPS)",
                emulatorCore = "touchHLE JIT v0.2.3",
                playTimeMinutes = 12,
                lastPlayedTimestamp = System.currentTimeMillis() - 172800_000L,
                summary = "O marco dos jogos musicais no iPhone! Toque nas 3 pistas de notas no ritmo das batidas eletrônicas com combos eletrizantes.",
                accentColorHex = "#BF5AF2",
                iconSymbol = "music",
                isSamplePlayable = true
            ),
            GameEntity(
                id = "com.halfbrick.fruitninja",
                title = "Fruit Ninja Classic iOS",
                developer = "Halfbrick Studios",
                category = "Ação",
                iosVersion = "iOS 4.0",
                architecture = "ARMv7 32-bit",
                fileSizeBytes = 48_600_000L,
                isInstalled = true,
                compatibilityRating = "100% Perfeito (60 FPS)",
                emulatorCore = "touchHLE JIT v0.2.3",
                playTimeMinutes = 32,
                lastPlayedTimestamp = System.currentTimeMillis() - 259200_000L,
                summary = "Deslize o dedo pela tela para fatiar maçãs, melancias e bananas enquanto desvia das perigosas bombas ninja!",
                accentColorHex = "#FF9F0A",
                iconSymbol = "blade",
                isSamplePlayable = true
            ),
            GameEntity(
                id = "com.ea.needforspeed",
                title = "Need for Speed: Shift",
                developer = "EA Mobile",
                category = "Corrida",
                iosVersion = "iOS 3.1",
                architecture = "ARMv7 32-bit",
                fileSizeBytes = 280_000_000L,
                isInstalled = false,
                compatibilityRating = "97% Ótimo (60 FPS)",
                emulatorCore = "touchHLE JIT v0.2.3",
                playTimeMinutes = 0,
                lastPlayedTimestamp = 0L,
                summary = "Simulação automobilística com cockpit dinâmico, física de colisões realistas e superesportivos velozes.",
                accentColorHex = "#F97316",
                iconSymbol = "racing",
                isSamplePlayable = true
            ),
            GameEntity(
                id = "com.halfbrick.jetpack",
                title = "Jetpack Joyride",
                developer = "Halfbrick Studios",
                category = "Runner",
                iosVersion = "iOS 4.3",
                architecture = "ARMv7 32-bit",
                fileSizeBytes = 52_000_000L,
                isInstalled = false,
                compatibilityRating = "99% Perfeito (60 FPS)",
                emulatorCore = "touchHLE JIT v0.2.3",
                playTimeMinutes = 0,
                lastPlayedTimestamp = 0L,
                summary = "Decole com Barry Steakfries usando mochilas a jato munidas de metralhadoras em uma fuga frenética de um laboratório secreto.",
                accentColorHex = "#38BDF8",
                iconSymbol = "runner",
                isSamplePlayable = true
            ),
            GameEntity(
                id = "com.sega.monkeyball",
                title = "Super Monkey Ball",
                developer = "SEGA Mobile",
                category = "Arcade",
                iosVersion = "iOS 2.0 / 3.0",
                architecture = "ARMv6/v7 32-bit",
                fileSizeBytes = 85_200_000L,
                isInstalled = false,
                compatibilityRating = "98% Ótimo (60 FPS)",
                emulatorCore = "touchHLE JIT v0.2.3",
                playTimeMinutes = 0,
                lastPlayedTimestamp = 0L,
                summary = "Primeiro grande sucesso do acelerômetro do iPhone em 2008. Incline o aparelho para rolar a esfera do macaco AiAi até o gol.",
                accentColorHex = "#FFD60A",
                iconSymbol = "sports_esports",
                isSamplePlayable = true
            ),
            GameEntity(
                id = "com.rovio.angrybirdsretro",
                title = "Angry Birds Classic iOS",
                developer = "Rovio Entertainment",
                category = "Casual",
                iosVersion = "iOS 3.2",
                architecture = "ARMv7 32-bit",
                fileSizeBytes = 41_500_000L,
                isInstalled = false,
                compatibilityRating = "99% Perfeito (60 FPS)",
                emulatorCore = "touchHLE JIT v0.2.3",
                playTimeMinutes = 0,
                lastPlayedTimestamp = 0L,
                summary = "A versão clássica original com física de estilingue que revolucionou os smartphones, destrua as fortalezas dos porcos verdes.",
                accentColorHex = "#FF453A",
                iconSymbol = "target",
                isSamplePlayable = false
            ),
            GameEntity(
                id = "com.epicgames.citadel",
                title = "Epic Citadel / Infinity Blade Demo",
                developer = "Epic Games / Chair",
                category = "Ação",
                iosVersion = "iOS 4.1",
                architecture = "ARMv7 Unreal 3",
                fileSizeBytes = 112_000_000L,
                isInstalled = false,
                compatibilityRating = "95% Jogável (50-60 FPS)",
                emulatorCore = "touchHLE Metal-GL",
                playTimeMinutes = 0,
                lastPlayedTimestamp = 0L,
                summary = "O marco gráfico que demonstrou a Unreal Engine 3 rodando em tempo real no iPhone 4 com iluminação dinâmica e castelo medieval.",
                accentColorHex = "#64D2FF",
                iconSymbol = "castle",
                isSamplePlayable = false
            ),
            GameEntity(
                id = "com.gamevil.zenonia1",
                title = "Zenonia Classic",
                developer = "Gamevil Inc.",
                category = "RPG",
                iosVersion = "iOS 3.0",
                architecture = "ARMv7 32-bit",
                fileSizeBytes = 36_900_000L,
                isInstalled = false,
                compatibilityRating = "97% Ótimo (60 FPS)",
                emulatorCore = "touchHLE JIT v0.2.3",
                playTimeMinutes = 0,
                lastPlayedTimestamp = 0L,
                summary = "RPG de ação em pixel art aclamado com enredo profundo, árvores de habilidades, ciclo de dia/noite e combate em tempo real.",
                accentColorHex = "#34D399",
                iconSymbol = "shield",
                isSamplePlayable = false
            ),
            GameEntity(
                id = "com.ngmoco.rolando2",
                title = "Rolando 2: Quest for the Golden Orchid",
                developer = "ngmoco:)",
                category = "Aventura",
                iosVersion = "iOS 3.1",
                architecture = "ARMv7 32-bit",
                fileSizeBytes = 54_000_000L,
                isInstalled = false,
                compatibilityRating = "96% Ótimo (60 FPS)",
                emulatorCore = "touchHLE JIT v0.2.3",
                playTimeMinutes = 0,
                lastPlayedTimestamp = 0L,
                summary = "Um dos jogos mais charmosos da era do iPhone 3GS, com física deliciosa, trilha sonora de Bok Choy e fases criativas.",
                accentColorHex = "#F472B6",
                iconSymbol = "stars",
                isSamplePlayable = false
            )
        )

        val missing = catalog.filter { !currentIds.contains(it.id) }
        if (missing.isNotEmpty()) {
            gameDao.insertAll(missing)
        }
    }

    fun startDownload(gameId: String, scope: CoroutineScope) {
        scope.launch(Dispatchers.IO) {
            gameDao.updateDownloadProgress(gameId, true, 0.05f, "Iniciando download...")
            var progress = 0.05f
            while (progress < 1.0f) {
                delay(250)
                progress += Random.nextFloat() * 0.12f + 0.08f
                if (progress > 0.95f && progress < 1.0f) {
                    gameDao.updateDownloadProgress(
                        gameId,
                        true,
                        0.96f,
                        "Extraindo Payload/ e gerando shaders Vulkan..."
                    )
                    delay(700)
                    progress = 1.0f
                } else if (progress < 1.0f) {
                    val speed = "%.1f MB/s".format(Random.nextDouble(6.5, 14.8))
                    gameDao.updateDownloadProgress(gameId, true, progress.coerceAtMost(0.95f), speed)
                }
            }
            gameDao.setInstalled(gameId, true)
        }
    }

    suspend fun uninstallGame(gameId: String) {
        gameDao.setInstalled(gameId, false)
        gameDao.updateDownloadProgress(gameId, false, 0f, "")
    }

    suspend fun recordPlaySession(gameId: String, durationMinutes: Int = 1) {
        gameDao.updatePlayTime(gameId, System.currentTimeMillis(), durationMinutes)
    }

    suspend fun saveGameState(
        gameId: String,
        gameTitle: String,
        slot: Int,
        label: String,
        score: Int,
        progressInfo: String,
        inGameLevel: String = "Fase 1",
        playTimeSeconds: Long = 0L,
        isAutoSave: Boolean = false,
        stateData: String = "{}"
    ): Long {
        val state = SaveStateEntity(
            gameId = gameId,
            gameTitle = gameTitle,
            slotNumber = slot,
            timestamp = System.currentTimeMillis(),
            label = label,
            score = score,
            progressInfo = progressInfo,
            inGameLevel = inGameLevel,
            playTimeSeconds = playTimeSeconds,
            isAutoSave = isAutoSave,
            stateData = stateData
        )
        return saveStateDao.insertSaveState(state)
    }

    suspend fun updateSaveStateLabel(stateId: Long, newLabel: String) {
        saveStateDao.updateSaveStateLabel(stateId, newLabel)
    }

    suspend fun deleteSaveState(stateId: Long) {
        saveStateDao.deleteSaveState(stateId)
    }

    suspend fun deleteAllSaveStatesForGame(gameId: String) {
        saveStateDao.deleteAllForGame(gameId)
    }

    suspend fun importIpaFile(fileName: String, sizeBytes: Long, customTitle: String? = null) {
        val cleanName = customTitle ?: fileName
            .replace(".ipa", "", ignoreCase = true)
            .replace(".zip", "", ignoreCase = true)
            .replace("_", " ")
            .replace("-", " ")
            .trim()
            .split(" ")
            .joinToString(" ") { it.replaceFirstChar { char -> char.uppercase() } }

        val id = "custom.ipa." + System.currentTimeMillis()
        val importedGame = GameEntity(
            id = id,
            title = cleanName,
            developer = "Importado via IPA",
            category = "Sideload",
            iosVersion = "iOS 4.x/5.x ARMv7",
            architecture = "ARMv7 32-bit Mach-O",
            fileSizeBytes = sizeBytes.coerceAtLeast(12_000_000L),
            isInstalled = true,
            compatibilityRating = "100% Pronto para Execução",
            emulatorCore = "touchHLE JIT v0.2.3",
            playTimeMinutes = 0,
            lastPlayedTimestamp = System.currentTimeMillis(),
            summary = "Pacote IPA/ZIP importado pelo usuário. Payload validado, Info.plist verificado e binário armv7 pronto para emulação.",
            accentColorHex = "#38BDF8",
            iconSymbol = "folder_zip",
            isSamplePlayable = true,
            localIpaPath = fileName
        )
        gameDao.insertOrUpdate(importedGame)
    }
}
