package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.CalculationCategory
import com.example.data.preferences.SettingsDataStore
import com.example.data.preferences.UserSettings
import com.example.data.repository.HistoryRepository
import com.example.domain.math.ConversionUnit
import com.example.domain.math.NumberFormatter
import com.example.domain.math.UnitCategory
import com.example.domain.math.UnitConverterEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class UnitConversionRow(
    val unit: ConversionUnit,
    val formattedValue: String
)

data class UnitConverterUiState(
    val selectedCategory: UnitCategory = UnitCategory.LENGTH,
    val availableUnits: List<ConversionUnit> = UnitConverterEngine.unitsByCategory[UnitCategory.LENGTH].orEmpty(),
    val fromUnit: ConversionUnit = UnitConverterEngine.unitsByCategory[UnitCategory.LENGTH]!![2], // Meter
    val toUnit: ConversionUnit = UnitConverterEngine.unitsByCategory[UnitCategory.LENGTH]!![3],   // Kilometer
    val inputValue: String = "1000",
    val convertedOutput: String = "1",
    val allUnitComparisons: List<UnitConversionRow> = emptyList()
)

class UnitConverterViewModel(
    private val historyRepository: HistoryRepository,
    private val settingsDataStore: SettingsDataStore
) : ViewModel() {

    private var settings = UserSettings()

    private val _uiState = MutableStateFlow(UnitConverterUiState())
    val uiState: StateFlow<UnitConverterUiState> = _uiState.asStateFlow()

    init {
        recompute()
        viewModelScope.launch {
            settingsDataStore.settingsFlow.collectLatest { s ->
                settings = s
                recompute()
            }
        }
    }

    fun selectCategory(category: UnitCategory) {
        val units = UnitConverterEngine.unitsByCategory[category].orEmpty()
        if (units.size < 2) return
        _uiState.update {
            it.copy(
                selectedCategory = category,
                availableUnits = units,
                fromUnit = units.first(),
                toUnit = units[1]
            )
        }
        recompute()
    }

    fun setFromUnit(unit: ConversionUnit) {
        _uiState.update { it.copy(fromUnit = unit) }
        recompute()
    }

    fun setToUnit(unit: ConversionUnit) {
        _uiState.update { it.copy(toUnit = unit) }
        recompute()
    }

    fun swapUnits() {
        _uiState.update { it.copy(fromUnit = it.toUnit, toUnit = it.fromUnit) }
        recompute()
    }

    fun setInputValue(value: String) {
        _uiState.update { it.copy(inputValue = value) }
        recompute()
    }

    private fun recompute() {
        val state = _uiState.value
        val inputBd = state.inputValue.toBigDecimalOrNull()
        if (inputBd == null) {
            _uiState.update { it.copy(convertedOutput = "—", allUnitComparisons = emptyList()) }
            return
        }
        val mainOut = UnitConverterEngine.convert(inputBd, state.fromUnit, state.toUnit)
        val mainFmt = NumberFormatter.format(mainOut, settings.decimalPrecision, settings.thousandsSeparator)

        val comparisons = state.availableUnits.map { target ->
            val converted = UnitConverterEngine.convert(inputBd, state.fromUnit, target)
            UnitConversionRow(
                unit = target,
                formattedValue = NumberFormatter.format(converted, settings.decimalPrecision, settings.thousandsSeparator)
            )
        }

        _uiState.update {
            it.copy(
                convertedOutput = mainFmt,
                allUnitComparisons = comparisons
            )
        }
    }

    fun saveToHistory() {
        val state = _uiState.value
        if (state.inputValue.toBigDecimalOrNull() == null) return
        viewModelScope.launch {
            historyRepository.saveCalculation(
                expression = "${state.inputValue} ${state.fromUnit.symbol} → ${state.toUnit.symbol}",
                result = "${state.convertedOutput} ${state.toUnit.symbol}",
                category = CalculationCategory.UNIT
            )
        }
    }

    class Factory(
        private val historyRepository: HistoryRepository,
        private val settingsDataStore: SettingsDataStore
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return UnitConverterViewModel(historyRepository, settingsDataStore) as T
        }
    }
}
