package com.kiraya.diary.ui.rent

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kiraya.diary.R
import com.kiraya.diary.core.date.DateUtils
import com.kiraya.diary.ui.components.ConfirmDialog
import com.kiraya.diary.ui.components.DateField
import com.kiraya.diary.ui.components.FormScaffold
import com.kiraya.diary.ui.components.KirayaTopBar
import com.kiraya.diary.ui.components.LocalMoneyDisplay
import com.kiraya.diary.ui.components.MoneyField
import com.kiraya.diary.ui.components.MoneyText
import com.kiraya.diary.ui.components.RentStatusPill
import com.kiraya.diary.ui.components.TextInput

@Composable
fun MonthlyRentScreen(
    onBack: () -> Unit,
    onTenantClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MonthlyRentViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val createdMsg = state.lastCreated?.let { pluralStringResource(R.plurals.rent_generated, it, it) }
    LaunchedEffect(createdMsg) {
        if (createdMsg != null) {
            snackbar.showSnackbar(createdMsg)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = { KirayaTopBar(title = stringResource(R.string.monthly_rent_title), onBack = onBack) },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = padding.calculateTopPadding() + 8.dp,
                bottom = padding.calculateBottomPadding() + 24.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                MonthSwitcher(
                    month = state.month,
                    onPrevious = viewModel::previousMonth,
                    onNext = viewModel::nextMonth,
                    canGoNext = state.canGoNext,
                )
            }
            item {
                ElevatedCard(Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(16.dp)) {
                        Total(R.string.home_due, state.total, Modifier.weight(1f))
                        Total(R.string.home_collected, state.paid, Modifier.weight(1f))
                        Total(R.string.home_pending, state.remaining, Modifier.weight(1f))
                    }
                }
            }
            item {
                OutlinedButton(
                    onClick = viewModel::generateNow,
                    enabled = !state.generating,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(stringResource(R.string.rent_generate_now)) }
            }
            if (state.loaded && state.rows.isEmpty()) {
                item {
                    Text(
                        stringResource(R.string.rent_month_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 16.dp),
                    )
                }
            }
            items(state.rows, key = { it.tenant.tenant.id }) { row ->
                MonthRowCard(row, onClick = { onTenantClick(row.tenant.tenant.id) })
            }
        }
    }
}

@Composable
fun MonthSwitcher(month: Int, onPrevious: () -> Unit, onNext: () -> Unit, canGoNext: Boolean) {
    // The back icon is auto-mirrored, so arrows follow the reading direction (RTL for Urdu).
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onPrevious) {
            Icon(painterResource(R.drawable.ic_back), stringResource(R.string.month_previous))
        }
        Text(
            DateUtils.formatMonth(month),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center,
        )
        IconButton(onClick = onNext, enabled = canGoNext) {
            Icon(
                painterResource(R.drawable.ic_back),
                stringResource(R.string.month_next),
                modifier = Modifier.scale(scaleX = -1f, scaleY = 1f),
            )
        }
    }
}

@Composable
private fun Total(label: Int, amount: Long, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(stringResource(label), style = MaterialTheme.typography.bodySmall)
        MoneyText(amount, style = MaterialTheme.typography.titleSmall)
    }
}

@Composable
private fun MonthRowCard(row: MonthRow, onClick: () -> Unit) {
    val money = LocalMoneyDisplay.current
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(row.tenant.tenant.name, style = MaterialTheme.typography.titleMedium)
                val place = listOfNotNull(row.tenant.propertyName, row.tenant.unitName).joinToString(" · ")
                if (place.isNotEmpty()) {
                    Text(place, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                val detail = if (row.bills > 0) {
                    stringResource(R.string.rent_plus_bills, money.format(row.rent), money.format(row.bills))
                } else {
                    money.format(row.rent)
                }
                Text(detail, style = MaterialTheme.typography.bodySmall)
            }
            Spacer(Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End) {
                RentStatusPill(row.status)
                if (row.remaining > 0) {
                    Spacer(Modifier.height(4.dp))
                    MoneyText(row.remaining, style = MaterialTheme.typography.titleSmall)
                }
            }
        }
    }
}

@Composable
fun RentEntryEditScreen(
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RentEntryEditViewModel = hiltViewModel(),
) {
    val form by viewModel.form.collectAsStateWithLifecycle()
    var confirmDelete by remember { mutableStateOf(false) }
    LaunchedEffect(form.done) { if (form.done) onDone() }
    if (!form.loaded) return
    FormScaffold(
        title = stringResource(R.string.rent_entry_edit),
        onBack = onDone,
        onSave = viewModel::save,
        modifier = modifier,
    ) {
        Text(
            "${form.tenantName} · ${DateUtils.formatMonth(form.month)}",
            style = MaterialTheme.typography.titleMedium,
        )
        MoneyField(
            value = form.amount,
            onValueChange = viewModel::onAmount,
            label = stringResource(R.string.rent_entry_amount),
            isError = form.amountError,
            supportingText = if (form.amountError) stringResource(R.string.error_amount) else null,
        )
        DateField(stringResource(R.string.rent_entry_due_date), form.dueDate, viewModel::onDueDate)
        TextInput(form.note, viewModel::onNote, stringResource(R.string.field_notes), singleLine = false)
        TextButton(onClick = { confirmDelete = true }) {
            Text(stringResource(R.string.rent_entry_waive), color = MaterialTheme.colorScheme.error)
        }
    }
    if (confirmDelete) {
        ConfirmDialog(
            title = stringResource(R.string.rent_entry_waive),
            message = stringResource(R.string.rent_entry_waive_message),
            confirmLabel = stringResource(R.string.rent_entry_waive),
            onConfirm = viewModel::waive,
            onDismiss = { confirmDelete = false },
        )
    }
}
