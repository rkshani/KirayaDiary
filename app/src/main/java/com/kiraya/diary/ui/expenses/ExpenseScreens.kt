package com.kiraya.diary.ui.expenses

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
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
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kiraya.diary.R
import com.kiraya.diary.core.date.DateUtils
import com.kiraya.diary.domain.model.ExpenseCategory
import com.kiraya.diary.domain.usecase.ProfitSummary
import com.kiraya.diary.ui.components.ChoiceChips
import com.kiraya.diary.ui.components.ConfirmDialog
import com.kiraya.diary.ui.components.DateField
import com.kiraya.diary.ui.components.FormScaffold
import com.kiraya.diary.ui.components.KirayaTopBar
import com.kiraya.diary.ui.components.MoneyField
import com.kiraya.diary.ui.components.MoneyText
import com.kiraya.diary.ui.components.PeriodSwitcher
import com.kiraya.diary.ui.components.PickerField
import com.kiraya.diary.ui.components.PickerOption
import com.kiraya.diary.ui.components.SectionTitle
import com.kiraya.diary.ui.components.TextInput
import com.kiraya.diary.ui.components.labelRes
import com.kiraya.diary.ui.theme.KirayaColors

@Composable
fun ExpensesScreen(
    onBack: () -> Unit,
    onAdd: () -> Unit,
    onOpen: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ExpensesViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    Scaffold(
        modifier = modifier,
        topBar = { KirayaTopBar(title = stringResource(R.string.expenses_title), onBack = onBack) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAdd,
                icon = { Icon(painterResource(R.drawable.ic_add), contentDescription = null) },
                text = { Text(stringResource(R.string.expense_add)) },
            )
        },
    ) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = padding.calculateTopPadding() + 8.dp,
                bottom = padding.calculateBottomPadding() + 88.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                PeriodSwitcher(
                    period = state.period,
                    onPrevious = viewModel::previous,
                    onNext = viewModel::next,
                    onYearMode = viewModel::showYear,
                )
            }
            item { ProfitCard(state.summary) }
            if (state.summary.byProperty.size > 1) {
                item { SectionTitle(stringResource(R.string.profit_by_property)) }
                item { ByPropertyCard(state.summary) }
            }
            item { SectionTitle(stringResource(R.string.expenses_list)) }
            if (state.loaded && state.summary.expenseList.isEmpty()) {
                item {
                    Text(
                        stringResource(R.string.expenses_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            items(state.summary.expenseList, key = { it.expense.id }) { item ->
                val e = item.expense
                Card(onClick = { onOpen(e.id) }, modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(e.category.labelRes), style = MaterialTheme.typography.titleSmall)
                            val sub = listOfNotNull(DateUtils.formatDate(e.date), item.propertyName, e.note.ifBlank { null })
                            Text(
                                sub.joinToString(" · "),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        MoneyText(e.amount, style = MaterialTheme.typography.titleSmall, color = KirayaColors.status.unpaid)
                    }
                }
            }
        }
    }
}

@Composable
fun ProfitCard(summary: ProfitSummary, modifier: Modifier = Modifier) {
    ElevatedCard(
        modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
    ) {
        Column(Modifier.padding(20.dp)) {
            Text(stringResource(R.string.profit_net), style = MaterialTheme.typography.bodyMedium)
            MoneyText(
                summary.profit,
                style = MaterialTheme.typography.headlineMedium,
                color = if (summary.profit < 0) KirayaColors.status.unpaid else MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Row(Modifier.fillMaxWidth().padding(top = 12.dp)) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.profit_income), style = MaterialTheme.typography.bodySmall)
                    MoneyText(summary.income, style = MaterialTheme.typography.titleSmall)
                }
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.profit_expenses), style = MaterialTheme.typography.bodySmall)
                    MoneyText(summary.expenses, style = MaterialTheme.typography.titleSmall)
                }
            }
        }
    }
}

@Composable
fun ByPropertyCard(summary: ProfitSummary, modifier: Modifier = Modifier) {
    OutlinedCard(modifier.fillMaxWidth()) {
        summary.byProperty.forEachIndexed { i, row ->
            if (i > 0) HorizontalDivider(Modifier.padding(horizontal = 16.dp))
            Row(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(row.propertyName ?: stringResource(R.string.profit_no_property), style = MaterialTheme.typography.titleSmall)
                    Row {
                        Text(stringResource(R.string.profit_income) + ": ", style = MaterialTheme.typography.bodySmall)
                        MoneyText(row.income, style = MaterialTheme.typography.bodySmall)
                        Text("  " + stringResource(R.string.profit_expenses) + ": ", style = MaterialTheme.typography.bodySmall)
                        MoneyText(row.expenses, style = MaterialTheme.typography.bodySmall)
                    }
                }
                MoneyText(
                    row.profit,
                    style = MaterialTheme.typography.titleSmall,
                    color = if (row.profit < 0) KirayaColors.status.unpaid else KirayaColors.status.paid,
                )
            }
        }
    }
}

@Composable
fun ExpenseEditScreen(
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ExpenseEditViewModel = hiltViewModel(),
) {
    val form by viewModel.form.collectAsStateWithLifecycle()
    val properties by viewModel.properties.collectAsStateWithLifecycle()
    var confirmDelete by remember { mutableStateOf(false) }
    LaunchedEffect(form.done) { if (form.done) onDone() }
    if (!form.loaded) return

    FormScaffold(
        title = stringResource(if (viewModel.isEdit) R.string.expense_edit else R.string.expense_add),
        onBack = onDone,
        onSave = viewModel::save,
        modifier = modifier,
    ) {
        ChoiceChips(
            options = ExpenseCategory.entries,
            selected = form.category,
            label = { stringResource(it.labelRes) },
            onSelect = viewModel::onCategory,
            title = stringResource(R.string.expense_category),
        )
        MoneyField(
            value = form.amount,
            onValueChange = viewModel::onAmount,
            label = stringResource(R.string.expense_amount),
            isError = form.amountError,
            supportingText = if (form.amountError) stringResource(R.string.error_amount) else null,
        )
        DateField(stringResource(R.string.expense_date), form.date, viewModel::onDate)
        val none = stringResource(R.string.profit_no_property)
        PickerField(
            label = stringResource(R.string.bill_property),
            valueText = properties.firstOrNull { it.id == form.propertyId }?.name ?: none,
            options = listOf(PickerOption<Long?>(null, none)) + properties.map { PickerOption<Long?>(it.id, it.name) },
            onSelect = viewModel::onProperty,
        )
        TextInput(
            value = form.note,
            onValueChange = viewModel::onNote,
            label = stringResource(R.string.field_notes),
            supportingText = stringResource(R.string.expense_note_hint),
            singleLine = false,
        )
        if (viewModel.isEdit) {
            TextButton(onClick = { confirmDelete = true }) {
                Text(stringResource(R.string.expense_delete), color = MaterialTheme.colorScheme.error)
            }
        }
    }

    if (confirmDelete) {
        ConfirmDialog(
            title = stringResource(R.string.expense_delete),
            message = stringResource(R.string.expense_delete_message),
            confirmLabel = stringResource(R.string.action_delete),
            onConfirm = viewModel::delete,
            onDismiss = { confirmDelete = false },
        )
    }
}
