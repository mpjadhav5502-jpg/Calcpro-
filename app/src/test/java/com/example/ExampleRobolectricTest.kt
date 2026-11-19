package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.CalcProDatabase
import com.example.data.local.CalculationCategory
import com.example.data.local.CurrencyRateEntity
import com.example.data.preferences.AppLanguage
import com.example.data.preferences.AppThemeMode
import com.example.data.preferences.SettingsDataStore
import com.example.data.remote.CurrencyApiService
import com.example.data.remote.ExchangeRateResponse
import com.example.data.repository.CurrencyRepository
import com.example.data.repository.HistoryRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.math.BigDecimal

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    private lateinit var context: Context
    private lateinit var database: CalcProDatabase
    private lateinit var historyRepository: HistoryRepository

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, CalcProDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        historyRepository = HistoryRepository(database.historyDao())
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun readAppNameFromContext() {
        val appName = context.getString(R.string.app_name)
        assertEquals("CalcPro", appName)
    }

    @Test
    fun roomHistorySaveSearchDeleteAndClear() = runBlocking {
        historyRepository.saveCalculation("25 + 25", "50", CalculationCategory.STANDARD)
        historyRepository.saveCalculation("sin(30)", "0.5", CalculationCategory.SCIENTIFIC)

        val all = historyRepository.getAllHistory().first()
        assertEquals(2, all.size)

        val filtered = historyRepository.searchHistory("sin").first()
        assertEquals(1, filtered.size)
        assertEquals("0.5", filtered.first().result)

        historyRepository.deleteRecord(filtered.first().id)
        assertEquals(1, historyRepository.getAllHistory().first().size)

        historyRepository.clearAll()
        assertTrue(historyRepository.getAllHistory().first().isEmpty())
    }

    @Test
    fun settingsDataStorePersistsThemeLanguageAndPrecision() = runBlocking {
        val store = SettingsDataStore(context)
        store.setThemeMode(AppThemeMode.LIGHT)
        store.setLanguage(AppLanguage.HINDI)
        store.setDecimalPrecision(4)

        val loaded = store.settingsFlow.first()
        assertEquals(AppThemeMode.LIGHT, loaded.themeMode)
        assertEquals(AppLanguage.HINDI, loaded.language)
        assertEquals(4, loaded.decimalPrecision)
    }

    @Test
    fun currencyRepositoryOfflineCacheAndManualOverride() = runBlocking {
        val fakeApi = object : CurrencyApiService {
            override suspend fun getLatestUsdRates(): ExchangeRateResponse {
                return ExchangeRateResponse(
                    result = "success",
                    baseCode = "USD",
                    timeLastUpdateUnix = 1700000000L,
                    timeLastUpdateUtc = "Tue, 14 Nov 2023 22:13:20 +0000",
                    rates = mapOf("USD" to 1.0, "INR" to 83.5, "EUR" to 0.92)
                )
            }
        }
        val repo = CurrencyRepository(database.currencyRateDao(), fakeApi)
        val fetchRes = repo.fetchLiveRates()
        assertTrue(fetchRes.isSuccess)

        val snap = repo.observeSnapshot().first()
        assertTrue(snap.hasAnyRate)
        assertFalse(snap.isOutdated)

        val converted = repo.convert(BigDecimal("10"), "USD", "INR", snap.ratesByCode)
        assertEquals(0, converted!!.compareTo(BigDecimal("835.0")))

        // Outdated cache check (> 24h old)
        val oldTime = System.currentTimeMillis() - (25L * 60L * 60L * 1000L)
        database.currencyRateDao().upsertAll(
            listOf(
                CurrencyRateEntity("INR", 83.5, "Old", oldTime),
                CurrencyRateEntity("USD", 1.0, "Old", oldTime),
                CurrencyRateEntity("EUR", 0.92, "Old", oldTime)
            )
        )
        val outdatedSnap = repo.observeSnapshot().first()
        assertTrue(outdatedSnap.isOutdated)

        // Manual override test
        repo.setManualPairRate("USD", "INR", 85.0)
        val manualSnap = repo.observeSnapshot().first()
        val manualConverted = repo.convert(BigDecimal("2"), "USD", "INR", manualSnap.ratesByCode)
        assertEquals(0, manualConverted!!.compareTo(BigDecimal("170.0")))
    }
}
