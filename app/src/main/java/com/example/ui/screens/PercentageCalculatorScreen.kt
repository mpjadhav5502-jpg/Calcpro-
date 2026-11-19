package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BookmarkAdd
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.preferences.UserSettings
import com.example.domain.math.NumberFormatter
import com.example.domain.math.PercentageMode
import com.example.ui.components.CalcProTopBar
import com.example.ui.theme.CalcExpressionStyle
import com.example.ui.viewmodel.FinancialToolsViewModel
import com.example.ui.viewmodel.PercentageUiState
import com.example.util.ClipboardShareHelper
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PercentageCalculatorScreen(
    uiState: PercentageUiState,
    settings: UserSettings,
    viewModel: FinancialToolsViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val (labelA, labelB) = when (uiState.mode) {
        PercentageMode.PERCENT_OF_NUMBER -> "Percentage (%)" to "Of Number"
        PercentageMode.PERCENT_INCREASE -> "Original Value" to "Increase By (%)"
        PercentageMode.PERCENT_DECREASE -> "Original Value" to "Decrease By (%)"
        PercentageMode.PERCENT_DIFFERENCE -> "Value A" to "Value B"
    }

    val formattedAnswer = uiState.breakdown?.let {
        val base = NumberFormatter.format(it.finalAnswer, settings.decimalPrecision, settings.thousandsSeparator)
        if (uiState.mode == PercentageMode.PERCENT_DIFFERENCE) "$base%" else base
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CalcProTopBar(
                title = stringResource(R.string.tool_percentage),
                onBack = onBack
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier.widthIn(max = 600.dp).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val modes = listOf(
                        PercentageMode.PERCENT_OF_NUMBER to R.string.pct_mode_of,
                        PercentageMode.PERCENT_INCREASE to R.string.pct_mode_increase,
                        PercentageMode.PERCENT_DECREASE to R.string.pct_mode_decrease,
                        PercentageMode.PERCENT_DIFFERENCE to R.string.pct_mode_difference
                    )
                    modes.forEach { (mode, labelRes) ->
                        FilterChip(
                            selected = uiState.mode == mode,
                            onClick = { viewModel.setPercentageMode(mode) },
                            label = { Text(stringResource(labelRes)) },
                            modifier = Modifier.testTag("pct_mode_${mode.name.lowercase()}")
                        )
                    }
                }

                OutlinedTextField(
                    value = uiState.inputA,
                    onValueChange = { viewModel.setPercentageInputs(it, uiState.inputB) },
                    label = { Text(labelA) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().testTag("pct_input_a")
                )

                OutlinedTextField(
                    value = uiState.inputB,
                    onValueChange = { viewModel.setPercentageInputs(uiState.inputA, it) },
                    label = { Text(labelB) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().testTag("pct_input_b")
                )

                if (uiState.breakdown != null && formattedAnswer != null) {
                    Card(
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = stringResource(R.string.final_answer),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Row {
                                    IconButton(onClick = {
                                        ClipboardShareHelper.copyText(context, "CalcPro", formattedAnswer)
                                        scope.launch { snackbarHostState.showSnackbar(context.getString(R.string.copied_to_clipboard)) }
                                    }) {
                                        Icon(Icons.Rounded.ContentCopy, contentDescription = stringResource(R.string.copy))
                                    }
                                    IconButton(onClick = {
                                        ClipboardShareHelper.shareText(
                                            context,
                                            context.getString(R.string.tool_percentage),
                                            "${uiState.breakdown.summaryExpression} = $formattedAnswer"
                                        )
                                    }) {
                                        Icon(Icons.Rounded.Share, contentDescription = stringResource(R.string.share))
                                    }
                                }
                            }
                            Text(
                                text = formattedAnswer,
                                style = MaterialTheme.typography.displayMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.testTag("pct_final_answer")
                            )
                        }
                    }

                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.calculation_steps),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            uiState.breakdown.steps.forEach { step ->
                                Text(
                                    text = step,
                                    style = CalcExpressionStyle.copy(
                                        fontSize = MaterialTheme.typography.bodyLarge.fontSize
                                    ),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Button(
                        onClick = {
                            viewModel.savePercentageToHistory()
                            scope.launch { snackbarHostState.showSnackbar(context.getString(R.string.saved_to_history)) }
                        },
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth().height(50.dp).testTag("pct_save_btn")
                    ) {
                        Icon(Icons.Rounded.BookmarkAdd, contentDescription = null)
                        Spacer(modifier = Modifier.padding(4.dp))
                        Text(stringResource(R.string.save_history))
                    }
                }
            }
        }
    }
}
