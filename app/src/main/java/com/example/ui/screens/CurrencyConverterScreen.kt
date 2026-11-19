package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BookmarkAdd
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.repository.SupportedCurrency
import com.example.ui.components.CalcProTopBar
import com.example.ui.viewmodel.CurrencyUiState
import com.example.ui.viewmodel.CurrencyViewModel
import com.example.util.ClipboardShareHelper
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date

@Composable
fun CurrencyConverterScreen(
    uiState: CurrencyUiState,
    viewModel: CurrencyViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var showManualDialog by remember { mutableStateOf(false) }

    val formattedTimestamp = remember(uiState.snapshot.fetchedAtMillis) {
        uiState.snapshot.fetchedAtMillis?.let { ms ->
            DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(ms))
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CalcProTopBar(
                title = stringResource(R.string.tool_currency),
                onBack = onBack,
                actions = {
                    IconButton(
                        onClick = { viewModel.fetchLiveRates() },
                        enabled = !uiState.isFetching,
                        modifier = Modifier.testTag("currency_refresh_btn")
                    ) {
                        if (uiState.isFetching) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Rounded.Refresh, contentDescription = stringResource(R.string.refresh))
                        }
                    }
                }
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
                // Status / Cache Warning Banner
                if (!uiState.snapshot.hasAnyRate) {
                    Surface(
                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                Icons.Rounded.WarningAmber,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                            Text(
                                text = stringResource(R.string.currency_never_updated),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                } else if (uiState.snapshot.isOutdated || uiState.fetchError) {
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                Icons.Rounded.WarningAmber,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = if (uiState.fetchError) stringResource(R.string.currency_fetch_error)
                                else stringResource(R.string.currency_outdated_warning),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }

                if (formattedTimestamp != null) {
                    Text(
                        text = stringResource(R.string.currency_last_updated, formattedTimestamp),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.testTag("currency_last_updated_text")
                    )
                }

                OutlinedTextField(
                    value = uiState.amountInput,
                    onValueChange = { viewModel.setAmountInput(it) },
                    label = { Text(stringResource(R.string.gst_amount)) },
                    prefix = { Text("${uiState.fromCurrency.symbol} ") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().testTag("currency_amount_input")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CurrencyPickerButton(
                        label = stringResource(R.string.from_label),
                        selected = uiState.fromCurrency,
                        currencies = uiState.currencies,
                        onSelected = { viewModel.setFromCurrency(it) },
                        modifier = Modifier.weight(1f).testTag("currency_from_picker")
                    )

                    IconButton(
                        onClick = { viewModel.swapCurrencies() },
                        modifier = Modifier.testTag("currency_swap_btn")
                    ) {
                        Icon(
                            Icons.Rounded.SwapHoriz,
                            contentDescription = stringResource(R.string.swap),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    CurrencyPickerButton(
                        label = stringResource(R.string.to_label),
                        selected = uiState.toCurrency,
                        currencies = uiState.currencies,
                        onSelected = { viewModel.setToCurrency(it) },
                        modifier = Modifier.weight(1f).testTag("currency_to_picker")
                    )
                }

                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${uiState.toCurrency.flagEmoji} ${uiState.toCurrency.name}",
                                style = MaterialTheme.typography.labelLarge
                            )
                            if (uiState.convertedResult != null) {
                                IconButton(onClick = {
                                    ClipboardShareHelper.copyText(context, "CalcPro Currency", uiState.convertedResult)
                                    scope.launch { snackbarHostState.showSnackbar(context.getString(R.string.copied_to_clipboard)) }
                                }) {
                                    Icon(Icons.Rounded.ContentCopy, contentDescription = stringResource(R.string.copy))
                                }
                            }
                        }

                        Text(
                            text = if (uiState.convertedResult != null) {
                                "${uiState.toCurrency.symbol} ${uiState.convertedResult}"
                            } else "—",
                            style = MaterialTheme.typography.displayMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.testTag("currency_converted_text")
                        )

                        uiState.unitRateDisplay?.let { rateStr ->
                            Text(
                                text = rateStr,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                            )
                        }

                        if (uiState.isManualOverrideActive) {
                            Surface(
                                color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.currency_manual_badge),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.tertiary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = { showManualDialog = true },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f).height(48.dp).testTag("currency_manual_btn")
                    ) {
                        Icon(Icons.Rounded.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.padding(4.dp))
                        Text(stringResource(R.string.currency_set_manual), maxLines = 1)
                    }

                    if (uiState.isManualOverrideActive) {
                        OutlinedButton(
                            onClick = { viewModel.clearManualRates() },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1f).height(48.dp).testTag("currency_reset_manual_btn")
                        ) {
                            Text(stringResource(R.string.currency_clear_manual), maxLines = 1)
                        }
                    }
                }

                Button(
                    onClick = {
                        viewModel.saveToHistory()
                        scope.launch { snackbarHostState.showSnackbar(context.getString(R.string.saved_to_history)) }
                    },
                    enabled = uiState.convertedResult != null,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().height(50.dp).testTag("currency_save_btn")
                ) {
                    Icon(Icons.Rounded.BookmarkAdd, contentDescription = null)
                    Spacer(modifier = Modifier.padding(4.dp))
                    Text(stringResource(R.string.save_history))
                }
            }
        }
    }

    if (showManualDialog) {
        var manualRateInput by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showManualDialog = false },
            title = { Text(stringResource(R.string.currency_set_manual)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "1 ${uiState.fromCurrency.code} = ? ${uiState.toCurrency.code}",
                        style = MaterialTheme.typography.titleMedium
                    )
                    OutlinedTextField(
                        value = manualRateInput,
                        onValueChange = { manualRateInput = it },
                        label = { Text("Rate (${uiState.toCurrency.code})") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("currency_manual_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.applyManualRate(manualRateInput)
                        showManualDialog = false
                    },
                    modifier = Modifier.testTag("currency_manual_confirm")
                ) {
                    Text(stringResource(R.string.confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showManualDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@Composable
private fun CurrencyPickerButton(
    label: String,
    selected: SupportedCurrency,
    currencies: List<SupportedCurrency>,
    onSelected: (SupportedCurrency) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        OutlinedButton(
            onClick = { expanded = true },
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Column(horizontalAlignment = Alignment.Start, modifier = Modifier.fillMaxWidth()) {
                Text(label, style = MaterialTheme.typography.labelSmall)
                Text(
                    "${selected.flagEmoji} ${selected.code}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
            }
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            currencies.forEach { c ->
                DropdownMenuItem(
                    text = { Text("${c.flagEmoji} ${c.code} — ${c.name}") },
                    onClick = {
                        onSelected(c)
                        expanded = false
                    }
                )
            }
        }
    }
}
