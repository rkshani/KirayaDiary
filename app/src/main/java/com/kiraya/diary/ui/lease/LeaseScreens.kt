package com.kiraya.diary.ui.lease

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
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
import com.kiraya.diary.domain.model.Lease
import com.kiraya.diary.domain.usecase.LeaseInfo
import com.kiraya.diary.ui.components.ConfirmDialog
import com.kiraya.diary.ui.components.DateField
import com.kiraya.diary.ui.components.FormScaffold
import com.kiraya.diary.ui.components.LocalMoneyDisplay
import com.kiraya.diary.ui.components.MoneyField
import com.kiraya.diary.ui.components.PhotoField
import com.kiraya.diary.ui.components.PhotoThumb
import com.kiraya.diary.ui.components.StatusPill
import com.kiraya.diary.ui.components.TextInput
import com.kiraya.diary.ui.tenants.InfoRow
import com.kiraya.diary.ui.theme.KirayaColors

@Composable
fun LeaseEditScreen(
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LeaseEditViewModel = hiltViewModel(),
) {
    val form by viewModel.form.collectAsStateWithLifecycle()
    var confirmDelete by remember { mutableStateOf(false) }
    LaunchedEffect(form.done) { if (form.done) onDone() }
    if (!form.loaded) return

    FormScaffold(
        title = stringResource(if (form.exists) R.string.lease_edit else R.string.lease_add),
        onBack = onDone,
        onSave = viewModel::save,
        modifier = modifier,
    ) {
        Text(form.tenantName, style = MaterialTheme.typography.titleMedium)
        DateField(stringResource(R.string.lease_start), form.startDate, viewModel::onStart)
        DateField(
            label = stringResource(R.string.lease_end),
            millis = form.endDate,
            onDateChange = viewModel::onEnd,
            placeholder = stringResource(R.string.lease_no_end),
            trailing = if (form.endDate != null) {
                {
                    IconButton(onClick = { viewModel.onEnd(null) }) {
                        Icon(painterResource(R.drawable.ic_close), stringResource(R.string.lease_clear_end))
                    }
                }
            } else {
                null
            },
        )
        if (form.endError) {
            Text(
                stringResource(R.string.lease_end_error),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(1, 2, 3).forEach { years ->
                AssistChip(
                    onClick = { viewModel.onEndInYears(years) },
                    label = { Text(stringResource(R.string.lease_years, years)) },
                )
            }
        }
        MoneyField(
            value = form.baseRent,
            onValueChange = viewModel::onRent,
            label = stringResource(R.string.lease_base_rent),
            isError = form.rentError,
            supportingText = if (form.rentError) {
                stringResource(R.string.error_amount)
            } else {
                stringResource(R.string.lease_base_rent_hint)
            },
        )
        OutlinedTextField(
            value = form.increasePercent,
            onValueChange = viewModel::onPercent,
            label = { Text(stringResource(R.string.lease_increase)) },
            suffix = { Text("%") },
            singleLine = true,
            isError = form.percentError,
            supportingText = { Text(stringResource(R.string.lease_increase_hint)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(),
        )
        PhotoField(
            label = stringResource(R.string.lease_agreement_photo),
            fileName = form.agreementPhoto,
            onPicked = viewModel::onPhotoPicked,
            onRemove = viewModel::onPhotoRemoved,
        )
        TextInput(form.notes, viewModel::onNotes, stringResource(R.string.field_notes), singleLine = false)
        if (form.exists) {
            TextButton(onClick = { confirmDelete = true }) {
                Text(stringResource(R.string.lease_delete), color = MaterialTheme.colorScheme.error)
            }
        }
    }

    if (confirmDelete) {
        ConfirmDialog(
            title = stringResource(R.string.lease_delete),
            message = stringResource(R.string.lease_delete_message),
            confirmLabel = stringResource(R.string.action_delete),
            onConfirm = viewModel::delete,
            onDismiss = { confirmDelete = false },
        )
    }
}

/** Lease summary on the tenant detail screen. */
@Composable
fun LeaseCard(lease: Lease?, onEdit: () -> Unit, modifier: Modifier = Modifier) {
    OutlinedCard(modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.lease_title),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                if (lease != null) {
                    TextButton(onClick = onEdit) { Text(stringResource(R.string.action_edit)) }
                }
            }
            if (lease == null) {
                Text(
                    stringResource(R.string.lease_none),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedButton(onClick = onEdit, modifier = Modifier.padding(top = 8.dp)) {
                    Text(stringResource(R.string.lease_add))
                }
                return@Column
            }
            val info = LeaseInfo.from(lease, DateUtils.now())
            val money = LocalMoneyDisplay.current
            info.daysToEnd?.let { days ->
                when {
                    days < 0 -> StatusPill(stringResource(R.string.lease_ended), KirayaColors.status.unpaid)
                    days <= 30 -> StatusPill(stringResource(R.string.lease_ends_in, days), KirayaColors.status.partial)
                    else -> Unit
                }
            }
            InfoRow(stringResource(R.string.lease_start), DateUtils.formatDate(lease.startDate))
            InfoRow(
                stringResource(R.string.lease_end),
                lease.endDate?.let { DateUtils.formatDate(it) } ?: stringResource(R.string.lease_no_end),
            )
            InfoRow(stringResource(R.string.lease_base_rent), money.format(lease.baseRent))
            if (lease.yearlyIncreasePercent > 0) {
                InfoRow(stringResource(R.string.lease_increase), "${lease.yearlyIncreasePercent}%")
                InfoRow(stringResource(R.string.lease_current_rent), money.format(info.currentRent))
                if (info.nextIncreaseDate != null && info.nextRent != null) {
                    InfoRow(
                        stringResource(R.string.lease_next_increase),
                        "${DateUtils.formatDate(info.nextIncreaseDate)} → ${money.format(info.nextRent)}",
                        last = lease.agreementPhoto == null,
                    )
                }
            }
            lease.agreementPhoto?.let {
                PhotoThumb(it, stringResource(R.string.lease_agreement_photo), Modifier.padding(top = 12.dp))
            }
        }
    }
}
