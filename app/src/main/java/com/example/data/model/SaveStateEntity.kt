package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "save_states")
data class SaveStateEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val gameId: String,
    val gameTitle: String,
    val slotNumber: Int,
    val timestamp: Long = System.currentTimeMillis(),
    val label: String,
    val emulatorVersion: String = "iOS 4.2.1 ARMv7",
    val score: Int = 0,
    val progressInfo: String = "",
    val inGameLevel: String = "Fase 1",
    val playTimeSeconds: Long = 0L,
    val isAutoSave: Boolean = false,
    val stateData: String = "{}"
)
