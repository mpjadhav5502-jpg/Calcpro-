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
import com.example.ui.viewmodel.FinancialToolsViewModel
import com.example.ui.viewmodel.GstUiState
import com.example.util.ClipboardShareHelper
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GstCalculatorScreen(
    uiState: GstUiState,
    settings: UserSettings,
    viewModel: FinancialToolsViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val gstSlabs = listOf("3", "5", "12", "18", "28")

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CalcProTopBar(
                title = stringResource(R.string.tool_gst),
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FilterChip(
                        selected = !uiState.isInclusive,
                        onClick = { viewModel.setGstInputs(uiState.amount, uiState.rate, isInclusive = false) },
                        label = { Text(stringResource(R.string.gst_add)) },
                        modifier = Modifier.weight(1f).testTag("gst_mode_add")
                    )
                    FilterChip(
                        selected = uiState.isInclusive,
                        onClick = { viewModel.setGstInputs(uiState.amount, uiState.rate, isInclusive = true) },
                        label = { Text(stringResource(R.string.gst_remove)) },
                        modifier = Modifier.weight(1f).testTag("gst_mode_remove")
                    )
                }

                OutlinedTextField(
                    value = uiState.amount,
                    onValueChange = { viewModel.setGstInputs(it, uiState.rate, uiState.isInclusive) },
                    label = { Text(stringResource(R.string.gst_amount)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().testTag("gst_amount_input")
                )

                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    gstSlabs.forEach { slab ->
                        FilterChip(
                            selected = uiState.rate == slab,
                            onClick = { viewModel.setGstInputs(uiState.amount, slab, uiState.isInclusive) },
                            label = { Text("$slab%") },
                            modifier = Modifier.testTag("gst_slab_$slab")
                        )
                    }
                }

                OutlinedTextField(
                    value = uiState.rate,
                    onValueChange = { viewModel.setGstInputs(uiState.amount, it, uiState.isInclusive) },
                    label = { Text(stringResource(R.string.gst_rate)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().testTag("gst_rate_input")
                )

                uiState.breakdown?.let { b ->
                    val baseFmt = NumberFormatter.format(b.baseAmount, 2, settings.thousandsSeparator)
                    val taxFmt = NumberFormatter.format(b.totalGst, 2, settings.thousandsSeparator)
                    val halfFmt = NumberFormatter.format(b.cgst, 2, settings.thousandsSeparator)
                    val totalFmt = NumberFormatter.format(b.totalAmount, 2, settings.thousandsSeparator)

                    Card(
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = stringResource(R.string.gst_total_price),
                                    style = MaterialTheme.typography.labelLarge
                                )
                                Row {
                                    IconButton(onClick = {
                                        ClipboardShareHelper.copyText(context, "CalcPro GST", totalFmt)
                                        scope.launch { snackbarHostState.showSnackbar(context.getString(R.string.copied_to_clipboard)) }
                                    }) {
                                        Icon(Icons.Rounded.ContentCopy, contentDescription = stringResource(R.string.copy))
                                    }
                                    IconButton(onClick = {
                                        ClipboardShareHelper.shareText(
                                            context,
                                            context.getString(R.string.tool_gst),
                                            "Base: $baseFmt\nGST (${uiState.rate}%): $taxFmt (CGST $halfFmt + SGST $halfFmt)\nTotal: $totalFmt"
                                        )
                                    }) {
                                        Icon(Icons.Rounded.Share, contentDescription = stringResource(R.string.share))
                                    }
                                }
                            }
                            Text(
                                text = totalFmt,
                                style = MaterialTheme.typography.displayMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.testTag("gst_total_text")
                            )
                            HorizontalDivider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.16f))
                            SummaryRow(stringResource(R.string.gst_base_price), baseFmt)
                            SummaryRow(stringResource(R.string.gst_tax_amount), taxFmt)
                            SummaryRow("CGST (${uiState.rate}% ÷ 2)", halfFmt)
                            SummaryRow("SGST (${uiState.rate}% ÷ 2)", halfFmt)
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
                            viewModel.saveGstToHistory()
                            scope.launch { snackbarHostState.showSnackbar(context.getString(R.string.saved_to_history)) }
                        },
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth().height(50.dp).testTag("gst_save_btn")
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

@Composable
fun SummaryRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium)
        Text(text = value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
    }
}
