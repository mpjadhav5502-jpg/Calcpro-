package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.local.CalculationCategory
import com.example.data.preferences.UserSettings
import com.example.ui.components.CalcKeyButton
import com.example.ui.components.CalcProTopBar
import com.example.ui.components.KeyVariant
import com.example.ui.viewmodel.CalcErrorCode
import com.example.ui.viewmodel.CalculatorUiState
import com.example.ui.viewmodel.CalculatorViewModel
import com.example.ui.theme.CalcDisplayNumberStyle
import com.example.ui.theme.CalcExpressionStyle
import com.example.util.ClipboardShareHelper
import com.example.util.FeedbackHelper
import kotlinx.coroutines.launch

@Composable
fun StandardCalculatorScreen(
    uiState: CalculatorUiState,
    settings: UserSettings,
    viewModel: CalculatorViewModel,
    onBack: () -> Unit,
    onOpenScientific: () -> Unit,
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
                title = stringResource(R.string.tool_standard),
                onBack = onBack,
                actions = {
                    IconButton(
                        onClick = onOpenScientific,
                        modifier = Modifier.testTag("switch_scientific_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Science,
                            contentDescription = stringResource(R.string.tool_scientific)
                        )
                    }
                    IconButton(
                        onClick = onOpenHistory,
                        modifier = Modifier.testTag("open_history_btn")
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
                handleCalculatorKeyEvent(event.key, event.isShiftPressed, viewModel, CalculationCategory.STANDARD)
            } else false
        }
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            val isLandscape = maxWidth > maxHeight && maxWidth >= 560.dp

            val displaySection: @Composable (Modifier) -> Unit = { mod ->
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
                        if (pasted != null) {
                            viewModel.pasteExpression(pasted)
                        } else {
                            scope.launch { snackbarHostState.showSnackbar(context.getString(R.string.nothing_to_paste)) }
                        }
                    },
                    onShare = {
                        val resultText = if (uiState.justEvaluated && errorMessage == null) uiState.finalResult
                        else uiState.livePreview.ifBlank { uiState.finalResult }
                        val body = if (uiState.expression.isNotBlank()) {
                            "${uiState.expression} = $resultText"
                        } else resultText
                        ClipboardShareHelper.shareText(context, context.getString(R.string.app_name), body)
                    },
                    modifier = mod
                )
            }

            val keypadSection: @Composable (Modifier) -> Unit = { mod ->
                Column(
                    modifier = mod,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Memory & Backspace strip
                    Row(
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CalcKeyButton(
                            label = "MC",
                            onClick = { viewModel.memoryClear() },
                            variant = KeyVariant.MEMORY,
                            soundEnabled = settings.soundEnabled,
                            vibrationEnabled = settings.vibrationEnabled,
                            testTag = "key_mc",
                            fontSizeSp = 14,
                            modifier = Modifier.weight(1f).fillMaxHeight()
                        )
                        CalcKeyButton(
                            label = "MR",
                            onClick = { viewModel.memoryRecall() },
                            variant = KeyVariant.MEMORY,
                            soundEnabled = settings.soundEnabled,
                            vibrationEnabled = settings.vibrationEnabled,
                            testTag = "key_mr",
                            fontSizeSp = 14,
                            modifier = Modifier.weight(1f).fillMaxHeight()
                        )
                        CalcKeyButton(
                            label = "M+",
                            onClick = { viewModel.memoryAdd() },
                            variant = KeyVariant.MEMORY,
                            soundEnabled = settings.soundEnabled,
                            vibrationEnabled = settings.vibrationEnabled,
                            testTag = "key_m_plus",
                            fontSizeSp = 14,
                            modifier = Modifier.weight(1f).fillMaxHeight()
                        )
                        CalcKeyButton(
                            label = "M−",
                            onClick = { viewModel.memorySubtract() },
                            variant = KeyVariant.MEMORY,
                            soundEnabled = settings.soundEnabled,
                            vibrationEnabled = settings.vibrationEnabled,
                            testTag = "key_m_minus",
                            fontSizeSp = 14,
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
                                .testTag("key_backspace")
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
                    }

                    val rows = listOf(
                        listOf(
                            Triple("C", KeyVariant.UTILITY) { viewModel.clearAll() },
                            Triple("( )", KeyVariant.UTILITY) { viewModel.appendSmartParenthesis() },
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
                        ),
                        listOf(
                            Triple("±", KeyVariant.DIGIT) { viewModel.toggleSign() },
                            Triple("0", KeyVariant.DIGIT) { viewModel.appendDigit("0") },
                            Triple(".", KeyVariant.DIGIT) { viewModel.appendDot() },
                            Triple("=", KeyVariant.EQUALS) { viewModel.evaluate(CalculationCategory.STANDARD) }
                        )
                    )

                    rows.forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth().weight(1f),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            row.forEach { (label, variant, action) ->
                                val tag = when (label) {
                                    "C" -> "key_clear"
                                    "( )" -> "key_paren"
                                    "%" -> "key_percent"
                                    "÷" -> "key_div"
                                    "×" -> "key_mul"
                                    "−" -> "key_sub"
                                    "+" -> "key_add"
                                    "±" -> "key_sign"
                                    "." -> "key_dot"
                                    "=" -> "key_equals"
                                    else -> "key_$label"
                                }
                                CalcKeyButton(
                                    label = label,
                                    onClick = action,
                                    variant = variant,
                                    soundEnabled = settings.soundEnabled,
                                    vibrationEnabled = settings.vibrationEnabled,
                                    testTag = tag,
                                    fontSizeSp = if (label == "( )") 18 else 22,
                                    modifier = Modifier.weight(1f).fillMaxHeight()
                                )
                            }
                        }
                    }
                }
            }

            if (isLandscape) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    displaySection(Modifier.weight(0.44f).fillMaxHeight())
                    keypadSection(Modifier.weight(0.56f).fillMaxHeight())
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    displaySection(Modifier.fillMaxWidth().weight(0.36f))
                    keypadSection(Modifier.fillMaxWidth().weight(0.64f))
                }
            }
        }
    }
}

@Composable
fun CalculatorDisplayPanel(
    uiState: CalculatorUiState,
    errorMessage: String?,
    onCopy: () -> Unit,
    onPaste: () -> Unit,
    onShare: () -> Unit,
    modifier: Modifier = Modifier,
    extraBadge: @Composable (() -> Unit)? = null
) {
    val exprScroll = rememberScrollState()
    LaunchedEffect(uiState.expression) {
        exprScroll.animateScrollTo(exprScroll.maxValue)
    }

    Surface(
        shape = RoundedCornerShape(26.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (uiState.hasMemory) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "M: ${uiState.memoryValue}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                    extraBadge?.invoke()
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    AssistChip(
                        onClick = onCopy,
                        label = { Text(stringResource(R.string.copy), fontSize = 11.sp) },
                        leadingIcon = {
                            Icon(Icons.Rounded.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                        },
                        modifier = Modifier.testTag("btn_copy_result")
                    )
                    AssistChip(
                        onClick = onPaste,
                        label = { Text(stringResource(R.string.paste), fontSize = 11.sp) },
                        leadingIcon = {
                            Icon(Icons.Rounded.ContentPaste, contentDescription = null, modifier = Modifier.size(14.dp))
                        },
                        modifier = Modifier.testTag("btn_paste_expr")
                    )
                    IconButton(
                        onClick = onShare,
                        modifier = Modifier.size(36.dp).testTag("btn_share_result")
                    ) {
                        Icon(
                            Icons.Rounded.Share,
                            contentDescription = stringResource(R.string.share),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.End
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(exprScroll),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    Text(
                        text = uiState.expression.ifBlank { " " },
                        style = CalcExpressionStyle,
                        color = if (uiState.justEvaluated && errorMessage == null) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                        maxLines = 1,
                        textAlign = TextAlign.End,
                        modifier = Modifier.testTag("calc_expression_text")
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                val displayText = when {
                    errorMessage != null -> errorMessage
                    uiState.justEvaluated -> uiState.finalResult
                    uiState.livePreview.isNotBlank() -> "= ${uiState.livePreview}"
                    else -> uiState.finalResult
                }

                val displayColor = when {
                    errorMessage != null -> MaterialTheme.colorScheme.error
                    !uiState.justEvaluated && uiState.livePreview.isNotBlank() ->
                        MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.onSurface
                }

                val fontSize = when {
                    errorMessage != null -> 22.sp
                    displayText.length > 16 -> 26.sp
                    displayText.length > 11 -> 34.sp
                    else -> 44.sp
                }

                Text(
                    text = displayText,
                    style = CalcDisplayNumberStyle.copy(
                        fontSize = fontSize,
                        fontWeight = FontWeight.Bold
                    ),
                    color = displayColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.End,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("calc_result_text")
                )
            }
        }
    }
}

fun handleCalculatorKeyEvent(
    key: Key,
    isShiftPressed: Boolean,
    viewModel: CalculatorViewModel,
    category: CalculationCategory
): Boolean {
    return when {
        key == Key.Zero || key == Key.NumPad0 -> { viewModel.appendDigit("0"); true }
        key == Key.One || key == Key.NumPad1 -> { viewModel.appendDigit("1"); true }
        key == Key.Two || key == Key.NumPad2 -> { viewModel.appendDigit("2"); true }
        key == Key.Three || key == Key.NumPad3 -> { viewModel.appendDigit("3"); true }
        key == Key.Four || key == Key.NumPad4 -> { viewModel.appendDigit("4"); true }
        key == Key.Five || (key == Key.NumPad5) -> {
            if (isShiftPressed && key == Key.Five) viewModel.appendPostfix("%") else viewModel.appendDigit("5")
            true
        }
        key == Key.Six || key == Key.NumPad6 -> {
            if (isShiftPressed && key == Key.Six) viewModel.appendOperator("^") else viewModel.appendDigit("6")
            true
        }
        key == Key.Seven || key == Key.NumPad7 -> { viewModel.appendDigit("7"); true }
        key == Key.Eight || key == Key.NumPad8 -> {
            if (isShiftPressed && key == Key.Eight) viewModel.appendOperator("×") else viewModel.appendDigit("8")
            true
        }
        key == Key.Nine || key == Key.NumPad9 -> {
            if (isShiftPressed && key == Key.Nine) viewModel.appendOpenParen() else viewModel.appendDigit("9")
            true
        }
        key == Key.Period || key == Key.NumPadDot -> { viewModel.appendDot(); true }
        key == Key.Plus || key == Key.NumPadAdd || (key == Key.Equals && isShiftPressed) -> {
            viewModel.appendOperator("+"); true
        }
        key == Key.Minus || key == Key.NumPadSubtract -> { viewModel.appendOperator("-"); true }
        key == Key.NumPadMultiply -> { viewModel.appendOperator("×"); true }
        key == Key.Slash || key == Key.NumPadDivide -> { viewModel.appendOperator("÷"); true }
        key == Key.Enter || key == Key.NumPadEnter || key == Key.Equals -> {
            viewModel.evaluate(category); true
        }
        key == Key.Backspace -> { viewModel.backspace(); true }
        key == Key.Escape || key == Key.Delete -> { viewModel.clearAll(); true }
        else -> false
    }
}
