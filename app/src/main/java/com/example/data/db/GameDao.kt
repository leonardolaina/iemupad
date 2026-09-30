package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.GameEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GameDao {
    @Query("SELECT * FROM games ORDER BY title ASC")
    fun getAllGames(): Flow<List<GameEntity>>

    @Query("SELECT * FROM games WHERE isInstalled = 1 ORDER BY lastPlayedTimestamp DESC, title ASC")
    fun getInstalledGames(): Flow<List<GameEntity>>

    @Query("SELECT * FROM games WHERE id = :gameId LIMIT 1")
    fun getGameById(gameId: String): Flow<GameEntity?>

    @Query("SELECT * FROM games WHERE id = :gameId LIMIT 1")
    suspend fun getGameByIdOnce(gameId: String): GameEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(game: GameEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(games: List<GameEntity>)

    @Update
    suspend fun update(game: GameEntity)

    @Query("UPDATE games SET isDownloading = :isDownloading, downloadProgress = :progress, downloadSpeed = :speed WHERE id = :gameId")
    suspend fun updateDownloadProgress(gameId: String, isDownloading: Boolean, progress: Float, speed: String)

    @Query("UPDATE games SET isInstalled = :isInstalled, isDownloading = 0, downloadProgress = 1.0 WHERE id = :gameId")
    suspend fun setInstalled(gameId: String, isInstalled: Boolean)

    @Query("UPDATE games SET lastPlayedTimestamp = :timestamp, playTimeMinutes = playTimeMinutes + :addedMinutes WHERE id = :gameId")
    suspend fun updatePlayTime(gameId: String, timestamp: Long, addedMinutes: Int)

    @Query("DELETE FROM games WHERE id = :gameId")
    suspend fun deleteGame(gameId: String)
}
