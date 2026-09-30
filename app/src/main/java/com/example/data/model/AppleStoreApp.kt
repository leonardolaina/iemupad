package com.example.data.model

data class AppleAppVersion(
    val versionName: String,
    val releaseDate: String,
    val iosTarget: String,
    val architecture: String = "ARMv7 32-bit",
    val fileSizeBytes: Long = 45_000_000L,
    val isLegacyArmCompatible: Boolean = true,
    val changelog: String = "Lançamento estável e otimizações para processadores Apple A4/A5/A6."
)

data class AppleStoreApp(
    val trackId: Long,
    val trackName: String,
    val bundleId: String,
    val artistName: String,
    val currentVersion: String,
    val releaseDate: String,
    val currentVersionReleaseDate: String,
    val description: String,
    val releaseNotes: String = "",
    val artworkUrl512: String,
    val trackViewUrl: String,
    val primaryGenreName: String,
    val averageUserRating: Float = 4.8f,
    val userRatingCount: Int = 12000,
    val fileSizeBytes: Long = 55_000_000L,
    val minimumOsVersion: String = "iOS 4.3",
    val screenshotUrls: List<String> = emptyList(),
    val availableVersions: List<AppleAppVersion> = emptyList()
)
