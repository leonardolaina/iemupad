package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "games")
data class GameEntity(
    @PrimaryKey val id: String,
    val title: String,
    val developer: String,
    val category: String,
    val iosVersion: String,
    val architecture: String,
    val fileSizeBytes: Long,
    val isInstalled: Boolean,
    val isDownloading: Boolean = false,
    val downloadProgress: Float = 0f,
    val downloadSpeed: String = "",
    val compatibilityRating: String,
    val emulatorCore: String = "touchHLE JIT v0.2.3",
    val playTimeMinutes: Int = 0,
    val lastPlayedTimestamp: Long = 0L,
    val summary: String,
    val accentColorHex: String,
    val iconSymbol: String,
    val isSamplePlayable: Boolean = true,
    val localIpaPath: String? = null
)
