package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.CalculationCategory
import com.example.data.preferences.SettingsDataStore
import com.example.data.preferences.UserSettings
import com.example.data.repository.HistoryRepository
import com.example.domain.math.CalculationBreakdown
import com.example.domain.math.DiscountBreakdown
import com.example.domain.math.FinancialCalculators
import com.example.domain.math.GstBreakdown
import com.example.domain.math.NumberFormatter
import com.example.domain.math.PercentageMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigDecimal

data class PercentageUiState(
    val mode: PercentageMode = PercentageMode.PERCENT_OF_NUMBER,
    val inputA: String = "50",
    val inputB: String = "200",
    val breakdown: CalculationBreakdown? = null
)

data class GstUiState(
    val amount: String = "1000",
    val rate: String = "18",
    val isInclusive: Boolean = false,
    val breakdown: GstBreakdown? = null
)

data class DiscountUiState(
    val originalPrice: String = "1500",
    val discountPercent: String = "20",
    val extraTaxPercent: String = "0",
    val breakdown: DiscountBreakdown? = null
)

class FinancialToolsViewModel(
    private val historyRepository: HistoryRepository,
    private val settingsDataStore: SettingsDataStore
) : ViewModel() {

    private var settings = UserSettings()

    private val _percentageState = MutableStateFlow(PercentageUiState())
    val percentageState: StateFlow<PercentageUiState> = _percentageState.asStateFlow()

    private val _gstState = MutableStateFlow(GstUiState())
    val gstState: StateFlow<GstUiState> = _gstState.asStateFlow()

    private val _discountState = MutableStateFlow(DiscountUiState())
    val discountState: StateFlow<DiscountUiState> = _discountState.asStateFlow()

    init {
        recomputeAll()
        viewModelScope.launch {
            settingsDataStore.settingsFlow.collectLatest { s ->
                settings = s
                recomputeAll()
            }
        }
    }

    private fun recomputeAll() {
        recomputePercentage()
        recomputeGst()
        recomputeDiscount()
    }

    fun setPercentageMode(mode: PercentageMode) {
        _percentageState.update { it.copy(mode = mode) }
        recomputePercentage()
    }

    fun setPercentageInputs(a: String, b: String) {
        _percentageState.update { it.copy(inputA = a, inputB = b) }
        recomputePercentage()
    }

    private fun recomputePercentage() {
        val state = _percentageState.value
        val a = state.inputA.toBigDecimalOrNull()
        val b = state.inputB.toBigDecimalOrNull()
        val res = if (a != null && b != null) {
            FinancialCalculators.calculatePercentage(state.mode, a, b, settings.decimalPrecision)
        } else null
        _percentageState.update { it.copy(breakdown = res) }
    }

    fun savePercentageToHistory() {
        val breakdown = _percentageState.value.breakdown ?: return
        val formatted = NumberFormatter.format(
            breakdown.finalAnswer,
            settings.decimalPrecision,
            settings.thousandsSeparator
        )
        viewModelScope.launch {
            historyRepository.saveCalculation(
                expression = breakdown.summaryExpression,
                result = formatted,
                category = CalculationCategory.PERCENTAGE
            )
        }
    }

    fun setGstInputs(amount: String, rate: String, isInclusive: Boolean) {
        _gstState.update { it.copy(amount = amount, rate = rate, isInclusive = isInclusive) }
        recomputeGst()
    }

    private fun recomputeGst() {
        val state = _gstState.value
        val amt = state.amount.toBigDecimalOrNull()
        val rate = state.rate.toBigDecimalOrNull()
        val res = if (amt != null && rate != null && amt >= BigDecimal.ZERO && rate >= BigDecimal.ZERO) {
            FinancialCalculators.calculateGst(amt, rate, state.isInclusive, settings.decimalPrecision.coerceAtMost(4))
        } else null
        _gstState.update { it.copy(breakdown = res) }
    }

    fun saveGstToHistory() {
        val state = _gstState.value
        val breakdown = state.breakdown ?: return
        val modeTag = if (breakdown.isInclusive) "Remove GST" else "Add GST"
        val totalFmt = NumberFormatter.format(breakdown.totalAmount, 2, settings.thousandsSeparator)
        val gstFmt = NumberFormatter.format(breakdown.totalGst, 2, settings.thousandsSeparator)
        viewModelScope.launch {
            historyRepository.saveCalculation(
                expression = "$modeTag (${state.rate}% on ${state.amount})",
                result = "Total: $totalFmt (GST: $gstFmt)",
                category = CalculationCategory.GST
            )
        }
    }

    fun setDiscountInputs(original: String, discountPct: String, taxPct: String) {
        _discountState.update {
            it.copy(originalPrice = original, discountPercent = discountPct, extraTaxPercent = taxPct)
        }
        recomputeDiscount()
    }

    private fun recomputeDiscount() {
        val state = _discountState.value
        val orig = state.originalPrice.toBigDecimalOrNull()
        val disc = state.discountPercent.toBigDecimalOrNull()
        val tax = state.extraTaxPercent.ifBlank { "0" }.toBigDecimalOrNull()
        val res = if (orig != null && disc != null && tax != null && orig >= BigDecimal.ZERO && disc >= BigDecimal.ZERO) {
            FinancialCalculators.calculateDiscount(orig, disc, tax, 2)
        } else null
        _discountState.update { it.copy(breakdown = res) }
    }

    fun saveDiscountToHistory() {
        val state = _discountState.value
        val breakdown = state.breakdown ?: return
        val finalFmt = NumberFormatter.format(breakdown.finalPrice, 2, settings.thousandsSeparator)
        val saveFmt = NumberFormatter.format(breakdown.savingsAmount, 2, settings.thousandsSeparator)
        viewModelScope.launch {
            historyRepository.saveCalculation(
                expression = "${state.originalPrice} − ${state.discountPercent}% off",
                result = "$finalFmt (Saved $saveFmt)",
                category = CalculationCategory.DISCOUNT
            )
        }
    }

    class Factory(
        private val historyRepository: HistoryRepository,
        private val settingsDataStore: SettingsDataStore
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return FinancialToolsViewModel(historyRepository, settingsDataStore) as T
        }
    }
}
