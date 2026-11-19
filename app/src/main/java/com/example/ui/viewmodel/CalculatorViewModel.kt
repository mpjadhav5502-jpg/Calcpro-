package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.CalculationCategory
import com.example.data.preferences.SettingsDataStore
import com.example.data.preferences.UserSettings
import com.example.data.repository.HistoryRepository
import com.example.domain.math.AngleMode
import com.example.domain.math.EvalResult
import com.example.domain.math.MathEvalError
import com.example.domain.math.MathEvaluator
import com.example.domain.math.NumberFormatter
import com.example.domain.math.stripTrailingZerosSafe
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigDecimal

enum class CalcErrorCode {
    NONE,
    DIV_ZERO,
    SYNTAX,
    DOMAIN,
    OVERFLOW
}

data class CalculatorUiState(
    val expression: String = "",
    val livePreview: String = "",
    val finalResult: String = "0",
    val errorCode: CalcErrorCode = CalcErrorCode.NONE,
    val justEvaluated: Boolean = false,
    val angleMode: AngleMode = AngleMode.DEG,
    val isInverse: Boolean = false,
    val memoryValue: String = "0",
    val hasMemory: Boolean = false
)

class CalculatorViewModel(
    private val historyRepository: HistoryRepository,
    private val settingsDataStore: SettingsDataStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(CalculatorUiState())
    val uiState: StateFlow<CalculatorUiState> = _uiState.asStateFlow()

    private var currentSettings: UserSettings = UserSettings()

    init {
        viewModelScope.launch {
            settingsDataStore.settingsFlow.collectLatest { settings ->
                currentSettings = settings
                _uiState.update { state ->
                    state.copy(
                        memoryValue = settings.memoryValue,
                        hasMemory = settings.hasMemory
                    )
                }
                refreshLivePreview()
            }
        }
    }

    fun appendDigit(digit: String) {
        _uiState.update { state ->
            val base = if (state.justEvaluated) "" else state.expression
            state.copy(
                expression = base + digit,
                justEvaluated = false,
                errorCode = CalcErrorCode.NONE
            )
        }
        refreshLivePreview()
    }

    fun appendDot() {
        _uiState.update { state ->
            val base = if (state.justEvaluated) "0" else state.expression
            // Prevent multiple decimal dots in the active number segment
            val lastNumber = base.takeLastWhile { it.isDigit() || it == '.' }
            if (lastNumber.contains('.')) return@update state
            val prefix = if (base.isEmpty() || base.last() !in '0'..'9') "0." else "."
            state.copy(
                expression = base + prefix,
                justEvaluated = false,
                errorCode = CalcErrorCode.NONE
            )
        }
        refreshLivePreview()
    }

    fun appendOperator(op: String) {
        _uiState.update { state ->
            var expr = if (state.justEvaluated && state.errorCode == CalcErrorCode.NONE && state.finalResult != "0") {
                state.finalResult.replace(",", "")
            } else if (state.justEvaluated) {
                if (state.expression.isNotEmpty() && state.errorCode == CalcErrorCode.NONE) state.finalResult.replace(",", "") else ""
            } else {
                state.expression
            }

            if (expr.isEmpty()) {
                if (op == "-") {
                    return@update state.copy(expression = "-", justEvaluated = false, errorCode = CalcErrorCode.NONE)
                }
                return@update state
            }

            val operators = setOf('+', '-', '×', '÷', '*', '/', '^')
            val last = expr.last()
            if (last in operators && op != "%") {
                // Allow unary minus after ×, ÷, ^, (
                if (op == "-" && (last == '×' || last == '÷' || last == '^')) {
                    expr += op
                } else if (expr.length > 1 || last != '-') {
                    expr = expr.dropLast(1) + op
                }
            } else {
                expr += op
            }
            state.copy(expression = expr, justEvaluated = false, errorCode = CalcErrorCode.NONE)
        }
        refreshLivePreview()
    }

    fun appendSmartParenthesis() {
        _uiState.update { state ->
            val expr = if (state.justEvaluated) "" else state.expression
            val openCount = expr.count { it == '(' }
            val closeCount = expr.count { it == ')' }
            val last = expr.lastOrNull()
            val token = if (
                openCount > closeCount &&
                last != null &&
                (last.isDigit() || last == ')' || last == 'π' || last == 'e' || last == '%' || last == '!')
            ) {
                ")"
            } else {
                "("
            }
            state.copy(expression = expr + token, justEvaluated = false, errorCode = CalcErrorCode.NONE)
        }
        refreshLivePreview()
    }

    fun appendOpenParen() {
        _uiState.update { state ->
            val base = if (state.justEvaluated) "" else state.expression
            state.copy(expression = "$base(", justEvaluated = false, errorCode = CalcErrorCode.NONE)
        }
        refreshLivePreview()
    }

    fun appendCloseParen() {
        _uiState.update { state ->
            val base = if (state.justEvaluated) "" else state.expression
            val openCount = base.count { it == '(' }
            val closeCount = base.count { it == ')' }
            if (openCount <= closeCount) return@update state
            state.copy(expression = "$base)", justEvaluated = false, errorCode = CalcErrorCode.NONE)
        }
        refreshLivePreview()
    }

    fun appendFunction(funcPrefix: String) {
        _uiState.update { state ->
            val base = if (state.justEvaluated && state.errorCode == CalcErrorCode.NONE && state.expression.isNotEmpty()) {
                val res = state.finalResult.replace(",", "")
                return@update state.copy(
                    expression = "$funcPrefix$res)",
                    justEvaluated = false,
                    errorCode = CalcErrorCode.NONE
                )
            } else if (state.justEvaluated) {
                ""
            } else {
                state.expression
            }
            state.copy(expression = base + funcPrefix, justEvaluated = false, errorCode = CalcErrorCode.NONE)
        }
        refreshLivePreview()
    }

    fun appendPostfix(symbol: String) {
        _uiState.update { state ->
            val base = if (state.justEvaluated && state.errorCode == CalcErrorCode.NONE && state.expression.isNotEmpty()) {
                state.finalResult.replace(",", "")
            } else {
                state.expression
            }
            if (base.isEmpty()) return@update state
            state.copy(expression = base + symbol, justEvaluated = false, errorCode = CalcErrorCode.NONE)
        }
        refreshLivePreview()
    }

    fun appendConstant(constant: String) {
        _uiState.update { state ->
            val base = if (state.justEvaluated) "" else state.expression
            state.copy(expression = base + constant, justEvaluated = false, errorCode = CalcErrorCode.NONE)
        }
        refreshLivePreview()
    }

    fun toggleSign() {
        _uiState.update { state ->
            val expr = if (state.justEvaluated && state.errorCode == CalcErrorCode.NONE && state.expression.isNotEmpty()) {
                state.finalResult.replace(",", "")
            } else {
                state.expression
            }
            if (expr.isEmpty()) {
                return@update state.copy(expression = "-", justEvaluated = false, errorCode = CalcErrorCode.NONE)
            }
            val updated = if (expr.startsWith("-(") && expr.endsWith(")")) {
                expr.substring(2, expr.length - 1)
            } else if (expr.all { it.isDigit() || it == '.' || it == '-' }) {
                if (expr.startsWith("-")) expr.drop(1) else "-$expr"
            } else {
                "-($expr)"
            }
            state.copy(expression = updated, justEvaluated = false, errorCode = CalcErrorCode.NONE)
        }
        refreshLivePreview()
    }

    fun backspace() {
        _uiState.update { state ->
            if (state.expression.isEmpty()) return@update state
            val funcs = listOf("asin(", "acos(", "atan(", "cbrt(", "sqrt(", "sin(", "cos(", "tan(", "log(", "ln(", "mod")
            val matched = funcs.firstOrNull { state.expression.endsWith(it) }
            val next = if (matched != null) {
                state.expression.dropLast(matched.length)
            } else {
                state.expression.dropLast(1)
            }
            state.copy(expression = next, justEvaluated = false, errorCode = CalcErrorCode.NONE)
        }
        refreshLivePreview()
    }

    fun clearAll() {
        _uiState.update { state ->
            state.copy(
                expression = "",
                livePreview = "",
                finalResult = "0",
                errorCode = CalcErrorCode.NONE,
                justEvaluated = false
            )
        }
    }

    fun toggleAngleMode() {
        _uiState.update { state ->
            val next = if (state.angleMode == AngleMode.DEG) AngleMode.RAD else AngleMode.DEG
            state.copy(angleMode = next)
        }
        refreshLivePreview()
    }

    fun toggleInverse() {
        _uiState.update { state -> state.copy(isInverse = !state.isInverse) }
    }

    fun pasteExpression(raw: String) {
        if (raw.isBlank()) return
        _uiState.update { state ->
            val base = if (state.justEvaluated) "" else state.expression
            state.copy(expression = base + raw, justEvaluated = false, errorCode = CalcErrorCode.NONE)
        }
        refreshLivePreview()
    }

    fun loadExpressionFromHistory(expression: String) {
        _uiState.update { state ->
            state.copy(expression = expression, justEvaluated = false, errorCode = CalcErrorCode.NONE)
        }
        refreshLivePreview()
    }

    fun evaluate(category: CalculationCategory = CalculationCategory.STANDARD) {
        val state = _uiState.value
        val expr = autoCloseParentheses(state.expression)
        if (expr.isBlank()) return

        when (val res = MathEvaluator.evaluate(expr, state.angleMode)) {
            is EvalResult.Success -> {
                val formatted = NumberFormatter.format(
                    res.value,
                    currentSettings.decimalPrecision,
                    currentSettings.thousandsSeparator
                )
                _uiState.update {
                    it.copy(
                        expression = expr,
                        finalResult = formatted,
                        livePreview = "",
                        justEvaluated = true,
                        errorCode = CalcErrorCode.NONE
                    )
                }
                viewModelScope.launch {
                    historyRepository.saveCalculation(
                        expression = expr,
                        result = formatted,
                        category = category
                    )
                }
            }
            is EvalResult.Failure -> {
                val code = when (res.error) {
                    is MathEvalError.DivisionByZero -> CalcErrorCode.DIV_ZERO
                    is MathEvalError.DomainError -> CalcErrorCode.DOMAIN
                    is MathEvalError.Overflow -> CalcErrorCode.OVERFLOW
                    is MathEvalError.SyntaxError -> CalcErrorCode.SYNTAX
                }
                _uiState.update {
                    it.copy(
                        errorCode = code,
                        livePreview = "",
                        justEvaluated = true
                    )
                }
            }
        }
    }

    fun memoryClear() {
        viewModelScope.launch {
            settingsDataStore.setMemory("0", false)
        }
    }

    fun memoryRecall() {
        val mem = _uiState.value.memoryValue
        if (!_uiState.value.hasMemory) return
        _uiState.update { state ->
            val base = if (state.justEvaluated) "" else state.expression
            state.copy(expression = base + mem, justEvaluated = false, errorCode = CalcErrorCode.NONE)
        }
        refreshLivePreview()
    }

    fun memoryAdd() {
        val currentVal = currentEvaluatedBigDecimal() ?: return
        val existing = _uiState.value.memoryValue.toBigDecimalOrNull() ?: BigDecimal.ZERO
        val updated = existing.add(currentVal).stripTrailingZerosSafe()
        viewModelScope.launch {
            settingsDataStore.setMemory(updated.toPlainString(), true)
        }
    }

    fun memorySubtract() {
        val currentVal = currentEvaluatedBigDecimal() ?: return
        val existing = _uiState.value.memoryValue.toBigDecimalOrNull() ?: BigDecimal.ZERO
        val updated = existing.subtract(currentVal).stripTrailingZerosSafe()
        viewModelScope.launch {
            settingsDataStore.setMemory(updated.toPlainString(), true)
        }
    }

    private fun currentEvaluatedBigDecimal(): BigDecimal? {
        val state = _uiState.value
        val expr = autoCloseParentheses(state.expression)
        if (expr.isBlank()) {
            return state.finalResult.replace(",", "").toBigDecimalOrNull()
        }
        return when (val res = MathEvaluator.evaluate(expr, state.angleMode)) {
            is EvalResult.Success -> res.value
            is EvalResult.Failure -> null
        }
    }

    private fun refreshLivePreview() {
        val state = _uiState.value
        if (state.expression.isBlank() || state.justEvaluated) {
            _uiState.update { it.copy(livePreview = "") }
            return
        }
        val candidate = autoCloseParentheses(state.expression)
        when (val res = MathEvaluator.evaluate(candidate, state.angleMode)) {
            is EvalResult.Success -> {
                val formatted = NumberFormatter.format(
                    res.value,
                    currentSettings.decimalPrecision,
                    currentSettings.thousandsSeparator
                )
                _uiState.update { it.copy(livePreview = formatted) }
            }
            is EvalResult.Failure -> {
                _uiState.update { it.copy(livePreview = "") }
            }
        }
    }

    private fun autoCloseParentheses(expr: String): String {
        val open = expr.count { it == '(' }
        val close = expr.count { it == ')' }
        return if (open > close) expr + ")".repeat(open - close) else expr
    }

    class Factory(
        private val historyRepository: HistoryRepository,
        private val settingsDataStore: SettingsDataStore
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return CalculatorViewModel(historyRepository, settingsDataStore) as T
        }
    }
}
