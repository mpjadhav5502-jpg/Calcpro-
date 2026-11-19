package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.CalculationCategory
import com.example.data.preferences.SettingsDataStore
import com.example.data.preferences.UserSettings
import com.example.data.repository.CurrencyRepository
import com.example.data.repository.CurrencySnapshot
import com.example.data.repository.HistoryRepository
import com.example.data.repository.SupportedCurrency
import com.example.domain.math.NumberFormatter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CurrencyUiState(
    val currencies: List<SupportedCurrency> = CurrencyRepository.supportedCurrencies,
    val fromCurrency: SupportedCurrency = CurrencyRepository.supportedCurrencies[1], // USD
    val toCurrency: SupportedCurrency = CurrencyRepository.supportedCurrencies[0],   // INR
    val amountInput: String = "100",
    val convertedResult: String? = null,
    val unitRateDisplay: String? = null,
    val isManualOverrideActive: Boolean = false,
    val isFetching: Boolean = false,
    val fetchError: Boolean = false,
    val snapshot: CurrencySnapshot = CurrencySnapshot(emptyMap(), null, null, isOutdated = true, hasAnyRate = false)
)

class CurrencyViewModel(
    private val currencyRepository: CurrencyRepository,
    private val historyRepository: HistoryRepository,
    private val settingsDataStore: SettingsDataStore
) : ViewModel() {

    private var settings = UserSettings()

    private val _uiState = MutableStateFlow(CurrencyUiState())
    val uiState: StateFlow<CurrencyUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            settingsDataStore.settingsFlow.collectLatest { s ->
                settings = s
                recompute()
            }
        }
        viewModelScope.launch {
            currencyRepository.observeSnapshot().collectLatest { snap ->
                _uiState.update { it.copy(snapshot = snap) }
                recompute()
            }
        }
        fetchLiveRates()
    }

    fun fetchLiveRates() {
        viewModelScope.launch {
            _uiState.update { it.copy(isFetching = true, fetchError = false) }
            val result = currencyRepository.fetchLiveRates()
            _uiState.update {
                it.copy(
                    isFetching = false,
                    fetchError = result.isFailure
                )
            }
        }
    }

    fun setFromCurrency(currency: SupportedCurrency) {
        _uiState.update { it.copy(fromCurrency = currency) }
        recompute()
    }

    fun setToCurrency(currency: SupportedCurrency) {
        _uiState.update { it.copy(toCurrency = currency) }
        recompute()
    }

    fun swapCurrencies() {
        _uiState.update { it.copy(fromCurrency = it.toCurrency, toCurrency = it.fromCurrency) }
        recompute()
    }

    fun setAmountInput(amount: String) {
        _uiState.update { it.copy(amountInput = amount) }
        recompute()
    }

    fun applyManualRate(rateText: String) {
        val parsed = rateText.toDoubleOrNull() ?: return
        if (parsed <= 0.0) return
        val state = _uiState.value
        viewModelScope.launch {
            currencyRepository.setManualPairRate(
                fromCode = state.fromCurrency.code,
                toCode = state.toCurrency.code,
                pairRate = parsed
            )
        }
    }

    fun clearManualRates() {
        viewModelScope.launch {
            currencyRepository.clearAllManualOverrides()
        }
    }

    fun saveToHistory() {
        val state = _uiState.value
        val res = state.convertedResult ?: return
        viewModelScope.launch {
            historyRepository.saveCalculation(
                expression = "${state.amountInput} ${state.fromCurrency.code} → ${state.toCurrency.code}",
                result = "$res ${state.toCurrency.code}",
                category = CalculationCategory.CURRENCY
            )
        }
    }

    private fun recompute() {
        val state = _uiState.value
        val amountBd = state.amountInput.toBigDecimalOrNull()
        val rates = state.snapshot.ratesByCode

        val pairRate = currencyRepository.getEffectivePairRate(
            state.fromCurrency.code,
            state.toCurrency.code,
            rates
        )
        val unitRateText = pairRate?.let {
            "1 ${state.fromCurrency.code} = ${NumberFormatter.formatPlain(it, 4)} ${state.toCurrency.code}"
        }
        val convertedText = if (amountBd != null) {
            currencyRepository.convert(
                amountBd,
                state.fromCurrency.code,
                state.toCurrency.code,
                rates
            )?.let {
                NumberFormatter.format(it, settings.decimalPrecision.coerceAtMost(4), settings.thousandsSeparator)
            }
        } else null

        val manualActive = currencyRepository.isPairUsingManualOverride(
            state.fromCurrency.code,
            state.toCurrency.code,
            rates
        )

        _uiState.update {
            it.copy(
                convertedResult = convertedText,
                unitRateDisplay = unitRateText,
                isManualOverrideActive = manualActive
            )
        }
    }

    class Factory(
        private val currencyRepository: CurrencyRepository,
        private val historyRepository: HistoryRepository,
        private val settingsDataStore: SettingsDataStore
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return CurrencyViewModel(currencyRepository, historyRepository, settingsDataStore) as T
        }
    }
}
