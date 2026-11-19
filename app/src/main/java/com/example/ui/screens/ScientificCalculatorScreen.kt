package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material.icons.rounded.Calculate
import androidx.compose.material.icons.rounded.History
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.CalculationCategory
import com.example.data.preferences.UserSettings
import com.example.domain.math.AngleMode
import com.example.ui.components.CalcKeyButton
import com.example.ui.components.CalcProTopBar
import com.example.ui.components.KeyVariant
import com.example.ui.viewmodel.CalcErrorCode
import com.example.ui.viewmodel.CalculatorUiState
import com.example.ui.viewmodel.CalculatorViewModel
import com.example.util.ClipboardShareHelper
import com.example.util.FeedbackHelper
import kotlinx.coroutines.launch

@Composable
fun ScientificCalculatorScreen(
    uiState: CalculatorUiState,
    settings: UserSettings,
    viewModel: CalculatorViewModel,
    onBack: () -> Unit,
    onOpenStandard: () -> Unit,
    onOpenHistory: () -> Unit
) {
    val context = LocalContext.current
    val view = LocalView.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val errorMessage = when (uiState.errorCode) {
        CalcErrorCode.DIV_ZERO -> stringResource(R.string.err_div_zero)
        CalcErrorCode.SYNTAX -> stringResource(R.string.err_syntax)
        CalcErrorCode.DOMAIN -> stringResource(R.string.err_domain)
        CalcErrorCode.OVERFLOW -> stringResource(R.string.err_overflow)
        CalcErrorCode.NONE -> null
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CalcProTopBar(
                title = stringResource(R.string.tool_scientific),
                onBack = onBack,
                actions = {
                    IconButton(
                        onClick = onOpenStandard,
                        modifier = Modifier.testTag("switch_standard_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Calculate,
                            contentDescription = stringResource(R.string.tool_standard)
                        )
                    }
                    IconButton(
                        onClick = onOpenHistory,
                        modifier = Modifier.testTag("sci_history_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.History,
                            contentDescription = stringResource(R.string.tool_history)
                        )
                    }
                }
            )
        },
        modifier = Modifier.onPreviewKeyEvent { event ->
            if (event.type == KeyEventType.KeyDown) {
                handleCalculatorKeyEvent(event.key, event.isShiftPressed, viewModel, CalculationCategory.SCIENTIFIC)
            } else false
        }
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            val isLandscape = maxWidth > maxHeight && maxWidth >= 560.dp

            val displayBlock: @Composable (Modifier) -> Unit = { mod ->
                CalculatorDisplayPanel(
                    uiState = uiState,
                    errorMessage = errorMessage,
                    onCopy = {
                        val toCopy = if (uiState.justEvaluated && errorMessage == null) uiState.finalResult
                        else uiState.livePreview.ifBlank { uiState.finalResult }
                        ClipboardShareHelper.copyText(context, "CalcPro", toCopy)
                        scope.launch { snackbarHostState.showSnackbar(context.getString(R.string.copied_to_clipboard)) }
                    },
                    onPaste = {
                        val pasted = ClipboardShareHelper.pasteNumericOrExpression(context)
                        if (pasted != null) viewModel.pasteExpression(pasted)
                        else scope.launch { snackbarHostState.showSnackbar(context.getString(R.string.nothing_to_paste)) }
                    },
                    onShare = {
                        val resultText = if (uiState.justEvaluated && errorMessage == null) uiState.finalResult
                        else uiState.livePreview.ifBlank { uiState.finalResult }
                        val body = if (uiState.expression.isNotBlank()) "${uiState.expression} = $resultText" else resultText
                        ClipboardShareHelper.shareText(context, context.getString(R.string.app_name), body)
                    },
                    extraBadge = {
                        Surface(
                            onClick = { viewModel.toggleAngleMode() },
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("badge_angle_mode")
                        ) {
                            Text(
                                text = if (uiState.angleMode == AngleMode.DEG) "DEG" else "RAD",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    },
                    modifier = mod
                )
            }

            val scientificPad: @Composable (Modifier) -> Unit = { mod ->
                val sciRows = listOf(
                    listOf(
                        Triple(if (uiState.angleMode == AngleMode.DEG) "DEG" else "RAD", KeyVariant.UTILITY) { viewModel.toggleAngleMode() },
                        Triple("(", KeyVariant.SCIENTIFIC) { viewModel.appendOpenParen() },
                        Triple(")", KeyVariant.SCIENTIFIC) { viewModel.appendCloseParen() },
                        Triple("π", KeyVariant.SCIENTIFIC) { viewModel.appendConstant("π") },
                        Triple("e", KeyVariant.SCIENTIFIC) { viewModel.appendConstant("e") }
                    ),
                    listOf(
                        Triple("sin", KeyVariant.SCIENTIFIC) { viewModel.appendFunction("sin(") },
                        Triple("cos", KeyVariant.SCIENTIFIC) { viewModel.appendFunction("cos(") },
                        Triple("tan", KeyVariant.SCIENTIFIC) { viewModel.appendFunction("tan(") },
                        Triple("log", KeyVariant.SCIENTIFIC) { viewModel.appendFunction("log(") },
                        Triple("ln", KeyVariant.SCIENTIFIC) { viewModel.appendFunction("ln(") }
                    ),
                    listOf(
                        Triple("asin", KeyVariant.SCIENTIFIC) { viewModel.appendFunction("asin(") },
                        Triple("acos", KeyVariant.SCIENTIFIC) { viewModel.appendFunction("acos(") },
                        Triple("atan", KeyVariant.SCIENTIFIC) { viewModel.appendFunction("atan(") },
                        Triple("√", KeyVariant.SCIENTIFIC) { viewModel.appendFunction("sqrt(") },
                        Triple("∛", KeyVariant.SCIENTIFIC) { viewModel.appendFunction("cbrt(") }
                    ),
                    listOf(
                        Triple("x²", KeyVariant.SCIENTIFIC) { viewModel.appendPostfix("²") },
                        Triple("x³", KeyVariant.SCIENTIFIC) { viewModel.appendPostfix("³") },
                        Triple("xʸ", KeyVariant.SCIENTIFIC) { viewModel.appendOperator("^") },
                        Triple("n!", KeyVariant.SCIENTIFIC) { viewModel.appendPostfix("!") },
                        Triple("mod", KeyVariant.SCIENTIFIC) { viewModel.appendOperator("mod") }
                    )
                )

                Column(
                    modifier = mod,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    sciRows.forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth().weight(1f),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            row.forEach { (label, variant, action) ->
                                CalcKeyButton(
                                    label = label,
                                    onClick = action,
                                    variant = variant,
                                    soundEnabled = settings.soundEnabled,
                                    vibrationEnabled = settings.vibrationEnabled,
                                    testTag = "sci_${label.lowercase()}",
                                    cornerRadius = 14.dp,
                                    fontSizeSp = 14,
                                    modifier = Modifier.weight(1f).fillMaxHeight()
                                )
                            }
                        }
                    }
                }
            }

            val basicPad: @Composable (Modifier) -> Unit = { mod ->
                val basicRows = listOf(
                    listOf(
                        Triple("C", KeyVariant.UTILITY) { viewModel.clearAll() },
                        Triple("±", KeyVariant.UTILITY) { viewModel.toggleSign() },
                        Triple("%", KeyVariant.UTILITY) { viewModel.appendPostfix("%") },
                        Triple("÷", KeyVariant.OPERATOR) { viewModel.appendOperator("÷") }
                    ),
                    listOf(
                        Triple("7", KeyVariant.DIGIT) { viewModel.appendDigit("7") },
                        Triple("8", KeyVariant.DIGIT) { viewModel.appendDigit("8") },
                        Triple("9", KeyVariant.DIGIT) { viewModel.appendDigit("9") },
                        Triple("×", KeyVariant.OPERATOR) { viewModel.appendOperator("×") }
                    ),
                    listOf(
                        Triple("4", KeyVariant.DIGIT) { viewModel.appendDigit("4") },
                        Triple("5", KeyVariant.DIGIT) { viewModel.appendDigit("5") },
                        Triple("6", KeyVariant.DIGIT) { viewModel.appendDigit("6") },
                        Triple("−", KeyVariant.OPERATOR) { viewModel.appendOperator("-") }
                    ),
                    listOf(
                        Triple("1", KeyVariant.DIGIT) { viewModel.appendDigit("1") },
                        Triple("2", KeyVariant.DIGIT) { viewModel.appendDigit("2") },
                        Triple("3", KeyVariant.DIGIT) { viewModel.appendDigit("3") },
                        Triple("+", KeyVariant.OPERATOR) { viewModel.appendOperator("+") }
                    )
                )

                Column(
                    modifier = mod,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    basicRows.forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth().weight(1f),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            row.forEach { (label, variant, action) ->
                                CalcKeyButton(
                                    label = label,
                                    onClick = action,
                                    variant = variant,
                                    soundEnabled = settings.soundEnabled,
                                    vibrationEnabled = settings.vibrationEnabled,
                                    testTag = "sci_key_$label",
                                    cornerRadius = 16.dp,
                                    fontSizeSp = 20,
                                    modifier = Modifier.weight(1f).fillMaxHeight()
                                )
                            }
                        }
                    }

                    // Bottom row: 0, ., Backspace, =
                    Row(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        CalcKeyButton(
                            label = "0",
                            onClick = { viewModel.appendDigit("0") },
                            variant = KeyVariant.DIGIT,
                            soundEnabled = settings.soundEnabled,
                            vibrationEnabled = settings.vibrationEnabled,
                            testTag = "sci_key_0",
                            cornerRadius = 16.dp,
                            fontSizeSp = 20,
                            modifier = Modifier.weight(1f).fillMaxHeight()
                        )
                        CalcKeyButton(
                            label = ".",
                            onClick = { viewModel.appendDot() },
                            variant = KeyVariant.DIGIT,
                            soundEnabled = settings.soundEnabled,
                            vibrationEnabled = settings.vibrationEnabled,
                            testTag = "sci_key_dot",
                            cornerRadius = 16.dp,
                            fontSizeSp = 20,
                            modifier = Modifier.weight(1f).fillMaxHeight()
                        )
                        Surface(
                            onClick = {
                                FeedbackHelper.performKeyFeedback(
                                    view,
                                    context,
                                    settings.soundEnabled,
                                    settings.vibrationEnabled
                                )
                                viewModel.backspace()
                            },
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .testTag("sci_key_backspace")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.Backspace,
                                    contentDescription = stringResource(R.string.delete),
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        CalcKeyButton(
                            label = "=",
                            onClick = { viewModel.evaluate(CalculationCategory.SCIENTIFIC) },
                            variant = KeyVariant.EQUALS,
                            soundEnabled = settings.soundEnabled,
                            vibrationEnabled = settings.vibrationEnabled,
                            testTag = "sci_key_equals",
                            cornerRadius = 16.dp,
                            fontSizeSp = 22,
                            modifier = Modifier.weight(1f).fillMaxHeight()
                        )
                    }
                }
            }

            if (isLandscape) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    displayBlock(Modifier.fillMaxWidth().weight(0.32f))
                    Row(
                        modifier = Modifier.fillMaxWidth().weight(0.68f),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        scientificPad(Modifier.weight(1f).fillMaxHeight())
                        basicPad(Modifier.weight(1f).fillMaxHeight())
                    }
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    displayBlock(Modifier.fillMaxWidth().weight(0.27f))
                    scientificPad(Modifier.fillMaxWidth().weight(0.33f))
                    basicPad(Modifier.fillMaxWidth().weight(0.40f))
                }
            }
        }
    }
}
