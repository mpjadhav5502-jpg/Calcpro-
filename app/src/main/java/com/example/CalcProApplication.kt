package com.example

import android.app.Application
import com.example.data.ads.AdManager
import com.example.data.local.CalcProDatabase
import com.example.data.preferences.SettingsDataStore
import com.example.data.remote.CurrencyApiService
import com.example.data.repository.BillingRepository
import com.example.data.repository.CurrencyRepository
import com.example.data.repository.HistoryRepository

class AppContainer(application: Application) {
    val database: CalcProDatabase by lazy { CalcProDatabase.getInstance(application) }
    val settingsDataStore: SettingsDataStore by lazy { SettingsDataStore(application) }
    val historyRepository: HistoryRepository by lazy { HistoryRepository(database.historyDao()) }
    val currencyApiService: CurrencyApiService by lazy { CurrencyApiService.create() }
    val currencyRepository: CurrencyRepository by lazy {
        CurrencyRepository(database.currencyRateDao(), currencyApiService)
    }
    val billingRepository: BillingRepository by lazy { BillingRepository(application) }
    val adManager: AdManager by lazy { AdManager(application) }
}

class CalcProApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
