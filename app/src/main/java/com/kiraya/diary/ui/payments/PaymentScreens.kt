package com.kiraya.diary.ui.payments

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AssistChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kiraya.diary.R
import com.kiraya.diary.core.date.DateUtils
import com.kiraya.diary.domain.model.Payment
import com.kiraya.diary.domain.model.PaymentMethod
import com.kiraya.diary.ui.components.ChoiceChips
import com.kiraya.diary.ui.components.ConfirmDialog
import com.kiraya.diary.ui.components.DateField
import com.kiraya.diary.ui.components.FormScaffold
import com.kiraya.diary.ui.components.LocalMoneyDisplay
import com.kiraya.diary.ui.components.MoneyField
import com.kiraya.diary.ui.components.MoneyText
import com.kiraya.diary.ui.components.PickerField
import com.kiraya.diary.ui.components.PickerOption
import com.kiraya.diary.ui.components.TextInput
import com.kiraya.diary.ui.components.labelRes

@Composable
fun PaymentEditScreen(
    onDone: (savedPaymentId: Long?) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PaymentEditViewModel = hiltViewModel(),
) {
    val form by viewModel.form.collectAsStateWithLifecycle()
    val tenants by viewModel.tenants.collectAsStateWithLifecycle()
    var confirmDelete by remember { mutableStateOf(false) }
    val money = LocalMoneyDisplay.current
    LaunchedEffect(form.done) { if (form.done) onDone(form.savedId.takeIf { it > 0 }) }
    if (!form.loaded) return

    FormScaffold(
        title = stringResource(if (viewModel.isEdit) R.string.payment_edit else R.string.payment_add),
        onBack = { onDone(null) },
        onSave = viewModel::save,
        modifier = modifier,
    ) {
        val selected = tenants.firstOrNull { it.tenant.tenant.id == form.tenantId }
        PickerField(
            label = stringResource(R.string.payment_tenant),
            valueText = selected?.tenant?.tenant?.name.orEmpty(),
            options = tenants.map {
                val place = listOfNotNull(it.tenant.propertyName, it.tenant.unitName).joinToString(" · ")
                PickerOption(
                    value = it,
                    title = it.tenant.tenant.name,
                    subtitle = listOf(place, stringResource(R.string.payment_owes, money.format(it.pending)))
                        .filter(String::isNotBlank).joinToString(" · "),
                )
            },
            onSelect = viewModel::onTenant,
            isError = form.tenantError,
            supportingText = if (form.tenantError) stringResource(R.string.payment_pick_tenant) else null,
        )
        if (selected != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.payment_pending_now, money.format(selected.pending)),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                )
                if (selected.pending > 0) {
                    AssistChip(onClick = viewModel::payFull, label = { Text(stringResource(R.string.payment_full)) })
                }
            }
        }
        MoneyField(
            value = form.amount,
            onValueChange = viewModel::onAmount,
            label = stringResource(R.string.payment_amount),
            isError = form.amountError,
            supportingText = if (form.amountError) {
                stringResource(R.string.error_amount)
            } else {
                stringResource(R.string.payment_partial_hint)
            },
        )
        DateField(stringResource(R.string.payment_date), form.date, viewModel::onDate)
        val methods = PaymentMethod.entries.filter { it != PaymentMethod.DEPOSIT || form.method == PaymentMethod.DEPOSIT }
        ChoiceChips(
            options = methods,
            selected = form.method,
            label = { stringResource(it.labelRes) },
            onSelect = viewModel::onMethod,
            title = stringResource(R.string.payment_method),
        )
        TextInput(
            value = form.note,
            onValueChange = viewModel::onNote,
            label = stringResource(R.string.payment_note),
            supportingText = stringResource(R.string.payment_note_hint),
        )
        if (viewModel.isEdit) {
            TextButton(onClick = { confirmDelete = true }) {
                Text(stringResource(R.string.payment_delete), color = MaterialTheme.colorScheme.error)
            }
        }
    }

    if (confirmDelete) {
        ConfirmDialog(
            title = stringResource(R.string.payment_delete),
            message = stringResource(R.string.payment_delete_message),
            confirmLabel = stringResource(R.string.action_delete),
            onConfirm = viewModel::delete,
            onDismiss = { confirmDelete = false },
        )
    }
}

/** Payment history on the tenant detail screen, newest first. */
@Composable
fun PaymentsSection(
    payments: List<Payment>,
    onPaymentClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showAll by rememberSaveable { mutableStateOf(false) }
    val shown = if (showAll) payments else payments.take(COLLAPSED)
    OutlinedCard(modifier.fillMaxWidth()) {
        Column(Modifier.padding(vertical = 8.dp)) {
            Text(
                stringResource(R.string.payments_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
            if (payments.isEmpty()) {
                Text(
                    stringResource(R.string.payments_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
            shown.forEachIndexed { i, p ->
                if (i > 0) HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { onPaymentClick(p.id) }
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(DateUtils.formatDate(p.date), style = MaterialTheme.typography.bodyLarge)
                        val sub = listOf(stringResource(p.method.labelRes), p.note).filter(String::isNotBlank)
                        Text(
                            sub.joinToString(" · "),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    MoneyText(p.amount, style = MaterialTheme.typography.titleSmall)
                }
            }
            if (payments.size > COLLAPSED) {
                TextButton(onClick = { showAll = !showAll }, modifier = Modifier.padding(horizontal = 8.dp)) {
                    Text(stringResource(if (showAll) R.string.charges_show_less else R.string.charges_show_all))
                }
            }
        }
    }
}

private const val COLLAPSED = 5
