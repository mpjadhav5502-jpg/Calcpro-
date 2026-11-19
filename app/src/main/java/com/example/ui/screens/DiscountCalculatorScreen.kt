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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import com.example.ui.components.CalcProTopBar
import com.example.ui.theme.CalcExpressionStyle
import com.example.ui.viewmodel.DiscountUiState
import com.example.ui.viewmodel.FinancialToolsViewModel
import com.example.util.ClipboardShareHelper
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DiscountCalculatorScreen(
    uiState: DiscountUiState,
    settings: UserSettings,
    viewModel: FinancialToolsViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val discountPresets = listOf("10", "15", "20", "25", "30", "50")

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CalcProTopBar(
                title = stringResource(R.string.tool_discount),
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
                OutlinedTextField(
                    value = uiState.originalPrice,
                    onValueChange = {
                        viewModel.setDiscountInputs(it, uiState.discountPercent, uiState.extraTaxPercent)
                    },
                    label = { Text(stringResource(R.string.discount_original_price)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().testTag("discount_price_input")
                )

                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    discountPresets.forEach { preset ->
                        FilterChip(
                            selected = uiState.discountPercent == preset,
                            onClick = {
                                viewModel.setDiscountInputs(uiState.originalPrice, preset, uiState.extraTaxPercent)
                            },
                            label = { Text("$preset%") },
                            modifier = Modifier.testTag("discount_preset_$preset")
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = uiState.discountPercent,
                        onValueChange = {
                            viewModel.setDiscountInputs(uiState.originalPrice, it, uiState.extraTaxPercent)
                        },
                        label = { Text(stringResource(R.string.discount_percent)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.weight(1f).testTag("discount_pct_input")
                    )

                    OutlinedTextField(
                        value = uiState.extraTaxPercent,
                        onValueChange = {
                            viewModel.setDiscountInputs(uiState.originalPrice, uiState.discountPercent, it)
                        },
                        label = { Text(stringResource(R.string.discount_extra_tax)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.weight(1f).testTag("discount_tax_input")
                    )
                }

                uiState.breakdown?.let { b ->
                    val finalFmt = NumberFormatter.format(b.finalPrice, 2, settings.thousandsSeparator)
                    val savingsFmt = NumberFormatter.format(b.savingsAmount, 2, settings.thousandsSeparator)
                    val taxFmt = NumberFormatter.format(b.taxAmount, 2, settings.thousandsSeparator)

                    Card(
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = stringResource(R.string.discount_final_price),
                                    style = MaterialTheme.typography.labelLarge
                                )
                                Row {
                                    IconButton(onClick = {
                                        ClipboardShareHelper.copyText(context, "CalcPro Discount", finalFmt)
                                        scope.launch { snackbarHostState.showSnackbar(context.getString(R.string.copied_to_clipboard)) }
                                    }) {
                                        Icon(Icons.Rounded.ContentCopy, contentDescription = stringResource(R.string.copy))
                                    }
                                    IconButton(onClick = {
                                        ClipboardShareHelper.shareText(
                                            context,
                                            context.getString(R.string.tool_discount),
                                            "Original: ${uiState.originalPrice}\nDiscount: ${uiState.discountPercent}%\nSaved: $savingsFmt\nFinal Price: $finalFmt"
                                        )
                                    }) {
                                        Icon(Icons.Rounded.Share, contentDescription = stringResource(R.string.share))
                                    }
                                }
                            }
                            Text(
                                text = finalFmt,
                                style = MaterialTheme.typography.displayMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.testTag("discount_final_text")
                            )
                            val pctRatio = (b.discountPercent.toFloat() / 100f).coerceIn(0f, 1f)
                            LinearProgressIndicator(
                                progress = { pctRatio },
                                color = MaterialTheme.colorScheme.tertiary,
                                trackColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.4f),
                                modifier = Modifier.fillMaxWidth().height(8.dp)
                            )
                            HorizontalDivider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.16f))
                            SummaryRow(stringResource(R.string.discount_you_save), savingsFmt)
                            SummaryRow(stringResource(R.string.discount_extra_tax), taxFmt)
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
                            b.steps.forEach { step ->
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
                            viewModel.saveDiscountToHistory()
                            scope.launch { snackbarHostState.showSnackbar(context.getString(R.string.saved_to_history)) }
                        },
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth().height(50.dp).testTag("discount_save_btn")
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
