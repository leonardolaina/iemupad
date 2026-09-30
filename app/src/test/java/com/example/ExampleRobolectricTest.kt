package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.db.AppDatabase
import com.example.data.model.GameEntity
import com.example.data.model.SaveStateEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  private lateinit var database: AppDatabase

  @Before
  fun setup() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
      .allowMainThreadQueries()
      .build()
  }

  @After
  fun tearDown() {
    database.close()
  }

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("iEmu Games", appName)
  }

  @Test
  fun `verify game entity creation`() {
    val game = GameEntity(
      id = "com.test.game",
      title = "Test iOS Game",
      developer = "Dev",
      category = "Arcade",
      iosVersion = "iOS 4.2.1",
      architecture = "ARMv7 32-bit",
      fileSizeBytes = 25_000_000L,
      isInstalled = true,
      compatibilityRating = "100%",
      summary = "Testing entity",
      accentColorHex = "#0A84FF",
      iconSymbol = "sports_esports"
    )
    assertEquals("Test iOS Game", game.title)
    assertTrue(game.isInstalled)
    assertEquals("iOS 4.2.1", game.iosVersion)
  }

  @Test
  fun `verify save state room database operations`() = runBlocking {
    val saveDao = database.saveStateDao()

    val saveState = SaveStateEntity(
      gameId = "com.firemint.realracing2",
      gameTitle = "Real Racing 2 HD",
      slotNumber = 1,
      label = "Volta Rápida 1:24.3",
      score = 4500,
      inGameLevel = "Circuito de Indianápolis",
      playTimeSeconds = 120L,
      isAutoSave = false
    )

    val insertedId = saveDao.insertSaveState(saveState)
    assertTrue(insertedId > 0)

    val gameSaves = saveDao.getSaveStatesForGame("com.firemint.realracing2").first()
    assertEquals(1, gameSaves.size)
    assertEquals("Volta Rápida 1:24.3", gameSaves[0].label)
    assertEquals(4500, gameSaves[0].score)
    assertEquals(1, gameSaves[0].slotNumber)

    // Update label test
    saveDao.updateSaveStateLabel(insertedId, "Recorde Atualizado")
    val updatedSaves = saveDao.getSaveStatesForGame("com.firemint.realracing2").first()
    assertEquals("Recorde Atualizado", updatedSaves[0].label)

    // Delete test
    saveDao.deleteSaveState(insertedId)
    val emptySaves = saveDao.getSaveStatesForGame("com.firemint.realracing2").first()
    assertTrue(emptySaves.isEmpty())
  }
}
