package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.SaveStateEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SaveStateDao {
    @Query("SELECT * FROM save_states ORDER BY timestamp DESC")
    fun getAllSaveStates(): Flow<List<SaveStateEntity>>

    @Query("SELECT * FROM save_states WHERE gameId = :gameId ORDER BY timestamp DESC")
    fun getSaveStatesForGame(gameId: String): Flow<List<SaveStateEntity>>

    @Query("SELECT * FROM save_states WHERE id = :id LIMIT 1")
    fun getSaveStateById(id: Long): Flow<SaveStateEntity?>

    @Query("SELECT COUNT(*) FROM save_states WHERE gameId = :gameId")
    fun countSaveStatesForGame(gameId: String): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSaveState(state: SaveStateEntity): Long

    @Update
    suspend fun updateSaveState(state: SaveStateEntity)

    @Query("UPDATE save_states SET label = :newLabel WHERE id = :id")
    suspend fun updateSaveStateLabel(id: Long, newLabel: String)

    @Query("DELETE FROM save_states WHERE id = :stateId")
    suspend fun deleteSaveState(stateId: Long)

    @Query("DELETE FROM save_states WHERE gameId = :gameId")
    suspend fun deleteAllForGame(gameId: String)
}
