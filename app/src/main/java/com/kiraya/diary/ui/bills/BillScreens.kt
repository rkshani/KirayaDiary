package com.kiraya.diary.ui.bills

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kiraya.diary.R
import com.kiraya.diary.core.date.DateUtils
import com.kiraya.diary.core.money.Money
import com.kiraya.diary.domain.model.BillType
import com.kiraya.diary.domain.model.BillWithShares
import com.kiraya.diary.domain.model.SplitMode
import com.kiraya.diary.ui.components.ChoiceChips
import com.kiraya.diary.ui.components.ConfirmDialog
import com.kiraya.diary.ui.components.DateField
import com.kiraya.diary.ui.components.EmptyState
import com.kiraya.diary.ui.components.FormScaffold
import com.kiraya.diary.ui.components.KirayaTopBar
import com.kiraya.diary.ui.components.LocalMoneyDisplay
import com.kiraya.diary.ui.components.MoneyField
import com.kiraya.diary.ui.components.MoneyText
import com.kiraya.diary.ui.components.PickerField
import com.kiraya.diary.ui.components.PickerOption
import com.kiraya.diary.ui.components.SectionTitle
import com.kiraya.diary.ui.components.TextInput
import com.kiraya.diary.ui.components.labelRes
import com.kiraya.diary.ui.rent.MonthSwitcher

@Composable
fun BillsScreen(
    onBack: () -> Unit,
    onAdd: () -> Unit,
    onOpen: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BillsViewModel = hiltViewModel(),
) {
    val bills by viewModel.bills.collectAsStateWithLifecycle()
    Scaffold(
        modifier = modifier,
        topBar = { KirayaTopBar(title = stringResource(R.string.bills_title), onBack = onBack) },
        floatingActionButton = {
            if (!bills.isNullOrEmpty()) {
                ExtendedFloatingActionButton(
                    onClick = onAdd,
                    icon = { Icon(painterResource(R.drawable.ic_add), contentDescription = null) },
                    text = { Text(stringResource(R.string.bill_add)) },
                )
            }
        },
    ) { padding ->
        val list = bills ?: return@Scaffold
        if (list.isEmpty()) {
            EmptyState(
                iconRes = R.drawable.ic_bill,
                title = stringResource(R.string.bills_empty_title),
                message = stringResource(R.string.bills_empty_message),
                modifier = Modifier.padding(padding),
                action = { Button(onClick = onAdd) { Text(stringResource(R.string.bill_add)) } },
            )
            return@Scaffold
        }
        LazyColumn(
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = padding.calculateTopPadding() + 8.dp,
                bottom = padding.calculateBottomPadding() + 88.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(list, key = { it.bill.id }) { bill -> BillRow(bill, onClick = { onOpen(bill.bill.id) }) }
        }
    }
}

@Composable
private fun BillRow(item: BillWithShares, onClick: () -> Unit) {
    val b = item.bill
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(painterResource(R.drawable.ic_bill), contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    b.title.ifBlank { stringResource(b.type.labelRes) },
                    style = MaterialTheme.typography.titleMedium,
                )
                val sub = listOfNotNull(DateUtils.formatMonth(b.month), item.propertyName)
                Text(sub.joinToString(" · "), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    stringResource(R.string.bill_split_between, item.shares.size),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            MoneyText(b.totalAmount, style = MaterialTheme.typography.titleSmall)
        }
    }
}

@Composable
fun BillEditScreen(
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BillEditViewModel = hiltViewModel(),
) {
    val form by viewModel.form.collectAsStateWithLifecycle()
    val properties by viewModel.properties.collectAsStateWithLifecycle()
    var confirmDelete by remember { mutableStateOf(false) }
    val money = LocalMoneyDisplay.current
    LaunchedEffect(form.done) { if (form.done) onDone() }
    if (!form.loaded) return

    FormScaffold(
        title = stringResource(if (viewModel.isEdit) R.string.bill_edit else R.string.bill_add),
        onBack = onDone,
        onSave = viewModel::save,
        modifier = modifier,
    ) {
        ChoiceChips(
            options = BillType.entries,
            selected = form.type,
            label = { stringResource(it.labelRes) },
            onSelect = viewModel::onType,
            title = stringResource(R.string.bill_type),
        )
        TextInput(
            value = form.title,
            onValueChange = viewModel::onTitle,
            label = stringResource(R.string.bill_title_field),
            supportingText = stringResource(R.string.bill_title_hint),
        )
        val all = stringResource(R.string.bill_all_properties)
        PickerField(
            label = stringResource(R.string.bill_property),
            valueText = properties.firstOrNull { it.id == form.propertyId }?.name ?: all,
            options = listOf(PickerOption<Long?>(null, all)) + properties.map { PickerOption<Long?>(it.id, it.name) },
            onSelect = viewModel::onProperty,
        )
        Text(stringResource(R.string.bill_month), style = MaterialTheme.typography.labelLarge)
        MonthSwitcher(
            month = form.month,
            onPrevious = { viewModel.onMonth(-1) },
            onNext = { viewModel.onMonth(1) },
            canGoNext = true,
        )
        DateField(stringResource(R.string.rent_entry_due_date), form.dueDate, viewModel::onDueDate)
        ChoiceChips(
            options = SplitMode.entries,
            selected = form.splitMode,
            label = { stringResource(it.labelRes) },
            onSelect = viewModel::onSplitMode,
            title = stringResource(R.string.bill_split),
        )
        if (form.splitMode == SplitMode.EQUAL) {
            MoneyField(
                value = form.total,
                onValueChange = viewModel::onTotal,
                label = stringResource(R.string.bill_total),
                isError = form.totalError,
                supportingText = if (form.totalError) stringResource(R.string.error_amount) else null,
            )
        }

        SectionTitle(stringResource(R.string.bill_who_pays))
        val rows = form.visibleRows
        if (rows.isEmpty()) {
            Text(
                stringResource(R.string.bill_no_tenants),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        val shares = form.computedShares()
        var shareIndex = 0
        OutlinedCard(Modifier.fillMaxWidth()) {
            rows.forEach { row ->
                val share = if (row.selected) shares?.getOrNull(shareIndex++) else null
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(checked = row.selected, onCheckedChange = { viewModel.onToggle(row.tenant.tenant.id) })
                    Column(Modifier.weight(1f)) {
                        Text(row.tenant.tenant.name, style = MaterialTheme.typography.bodyLarge)
                        val place = listOfNotNull(row.tenant.propertyName, row.tenant.unitName).joinToString(" · ")
                        if (place.isNotEmpty()) {
                            Text(place, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    if (form.splitMode == SplitMode.CUSTOM && row.selected) {
                        OutlinedTextField(
                            value = row.amount,
                            onValueChange = { viewModel.onShareAmount(row.tenant.tenant.id, it) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier
                                .widthIn(max = 120.dp)
                                .padding(end = 8.dp),
                        )
                    } else if (share != null) {
                        Text(money.formatPlain(share), Modifier.padding(end = 12.dp), style = MaterialTheme.typography.titleSmall)
                    }
                }
            }
        }
        if (form.splitMode == SplitMode.CUSTOM) {
            val sum = shares?.sum() ?: form.visibleRows.filter { it.selected }.sumOf { Money.parse(it.amount.ifBlank { "0" }) ?: 0L }
            Text(
                stringResource(R.string.bill_custom_total, money.formatPlain(sum)),
                style = MaterialTheme.typography.titleSmall,
            )
        }
        if (form.sharesError) {
            Text(stringResource(R.string.bill_shares_error), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
        TextInput(form.note, viewModel::onNote, stringResource(R.string.field_notes), singleLine = false)
        if (viewModel.isEdit) {
            TextButton(onClick = { confirmDelete = true }) {
                Text(stringResource(R.string.bill_delete), color = MaterialTheme.colorScheme.error)
            }
        }
    }

    if (confirmDelete) {
        ConfirmDialog(
            title = stringResource(R.string.bill_delete),
            message = stringResource(R.string.bill_delete_message),
            confirmLabel = stringResource(R.string.action_delete),
            onConfirm = viewModel::delete,
            onDismiss = { confirmDelete = false },
        )
    }
}
