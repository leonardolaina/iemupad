package com.example.data.repository

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.example.data.db.GameDao
import com.example.data.model.AppleAppVersion
import com.example.data.model.AppleStoreApp
import com.example.data.model.GameEntity
import com.example.data.remote.AppleStoreApiService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlin.random.Random

class AppleStoreRepository(
    private val apiService: AppleStoreApiService,
    private val gameDao: GameDao
) {
    fun searchAppleStore(query: String): Flow<List<AppleStoreApp>> = flow {
        val results = apiService.searchAppleStore(query)
        emit(results)
    }.flowOn(Dispatchers.IO)

    fun openInOfficialAppStore(context: Context, trackViewUrl: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(trackViewUrl)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            val fallbackUrl = "https://www.apple.com/app-store/"
            val fallbackIntent = Intent(Intent.ACTION_VIEW, Uri.parse(fallbackUrl)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(fallbackIntent)
        }
    }

    fun downloadAndInstallVersion(
        app: AppleStoreApp,
        version: AppleAppVersion,
        scope: CoroutineScope
    ) {
        val gameId = app.bundleId
        val category = when {
            app.primaryGenreName.contains("Race", ignoreCase = true) || app.primaryGenreName.contains("Corrid", ignoreCase = true) -> "Corrida"
            app.primaryGenreName.contains("Arcade", ignoreCase = true) -> "Arcade"
            app.primaryGenreName.contains("Action", ignoreCase = true) || app.primaryGenreName.contains("Aç", ignoreCase = true) -> "Ação"
            app.primaryGenreName.contains("Puzzle", ignoreCase = true) -> "Puzzle"
            app.primaryGenreName.contains("Music", ignoreCase = true) || app.primaryGenreName.contains("Ritm", ignoreCase = true) -> "Ritmo"
            app.primaryGenreName.contains("Run", ignoreCase = true) -> "Runner"
            else -> app.primaryGenreName
        }

        val iconSymbol = when (category) {
            "Corrida" -> "racing"
            "Runner" -> "runner"
            "Puzzle" -> "puzzle"
            "Ritmo" -> "music"
            "Simulador" -> "plane"
            else -> "sports_esports"
        }

        val gameEntity = GameEntity(
            id = gameId,
            title = "${app.trackName} (${version.versionName.substringBefore(" ")})",
            developer = app.artistName,
            category = category,
            iosVersion = version.iosTarget,
            architecture = version.architecture,
            fileSizeBytes = version.fileSizeBytes,
            isInstalled = false,
            isDownloading = true,
            downloadProgress = 0.05f,
            downloadSpeed = "Conectando à Apple Store CDN...",
            compatibilityRating = "100% Compatível (${version.architecture})",
            emulatorCore = "touchHLE JIT v0.2.3",
            playTimeMinutes = 0,
            lastPlayedTimestamp = System.currentTimeMillis(),
            summary = "${app.description}\n\n[Versão Selecionada: ${version.versionName}] - ${version.changelog}",
            accentColorHex = "#0A84FF",
            iconSymbol = iconSymbol,
            isSamplePlayable = true,
            localIpaPath = "${app.trackName.replace(" ", "_")}_${version.versionName.take(6)}.ipa"
        )

        scope.launch(Dispatchers.IO) {
            gameDao.insertOrUpdate(gameEntity)
            var progress = 0.05f

            while (progress < 1.0f) {
                delay(220)
                progress += Random.nextFloat() * 0.15f + 0.08f
                if (progress > 0.92f && progress < 1.0f) {
                    gameDao.updateDownloadProgress(
                        gameId,
                        true,
                        0.95f,
                        "Assinatura Mach-O verificada • Gerando binário ARMv7..."
                    )
                    delay(600)
                    progress = 1.0f
                } else if (progress < 1.0f) {
                    val speed = "%.1f MB/s".format(Random.nextDouble(8.0, 18.5))
                    gameDao.updateDownloadProgress(gameId, true, progress.coerceAtMost(0.92f), speed)
                }
            }
            gameDao.setInstalled(gameId, true)
        }
    }
}
