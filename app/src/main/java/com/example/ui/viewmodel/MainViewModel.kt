package com.example.ui.viewmodel

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.ads.AdManager
import com.example.data.preferences.AppLanguage
import com.example.data.preferences.AppThemeMode
import com.example.data.preferences.SettingsDataStore
import com.example.data.preferences.UserSettings
import com.example.data.repository.BillingProductUi
import com.example.data.repository.BillingPurchaseState
import com.example.data.repository.BillingRepository
import com.example.data.repository.HistoryRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class MainUiState(
    val settings: UserSettings = UserSettings(),
    val isPremium: Boolean = false,
    val billingReady: Boolean = false,
    val products: List<BillingProductUi> = emptyList(),
    val purchaseState: BillingPurchaseState = BillingPurchaseState.Idle
)

class MainViewModel(
    private val settingsDataStore: SettingsDataStore,
    private val historyRepository: HistoryRepository,
    private val billingRepository: BillingRepository,
    private val adManager: AdManager
) : ViewModel() {

    val uiState: StateFlow<MainUiState> = combine(
        settingsDataStore.settingsFlow,
        billingRepository.isPremium,
        billingRepository.billingReady,
        billingRepository.products,
        billingRepository.purchaseState
    ) { settings, isPremium, billingReady, products, purchaseState ->
        if (!isPremium) {
            adManager.initialize(isPremium = false)
        }
        MainUiState(
            settings = settings,
            isPremium = isPremium,
            billingReady = billingReady,
            products = products,
            purchaseState = purchaseState
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = MainUiState()
    )

    fun setThemeMode(mode: AppThemeMode) {
        viewModelScope.launch {
            settingsDataStore.setThemeMode(mode)
        }
    }

    fun setLanguage(language: AppLanguage) {
        viewModelScope.launch {
            settingsDataStore.setLanguage(language)
        }
    }

    fun setDecimalPrecision(precision: Int) {
        viewModelScope.launch {
            settingsDataStore.setDecimalPrecision(precision)
        }
    }

    fun setThousandsSeparator(enabled: Boolean) {
        viewModelScope.launch {
            settingsDataStore.setThousandsSeparator(enabled)
        }
    }

    fun setSoundEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsDataStore.setSoundEnabled(enabled)
        }
    }

    fun setVibrationEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsDataStore.setVibrationEnabled(enabled)
        }
    }

    fun completeOnboarding() {
        viewModelScope.launch {
            settingsDataStore.setOnboardingCompleted(true)
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            historyRepository.clearAll()
        }
    }

    fun launchPurchase(activity: Activity, product: BillingProductUi) {
        billingRepository.launchPurchase(activity, product)
    }

    fun restorePurchases() {
        billingRepository.restorePurchases(silent = false)
    }

    fun refreshBillingProducts() {
        billingRepository.connectAndQuery()
    }

    fun clearPurchaseMessage() {
        billingRepository.clearPurchaseMessage()
    }

    class Factory(
        private val settingsDataStore: SettingsDataStore,
        private val historyRepository: HistoryRepository,
        private val billingRepository: BillingRepository,
        private val adManager: AdManager
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MainViewModel(settingsDataStore, historyRepository, billingRepository, adManager) as T
        }
    }
}
