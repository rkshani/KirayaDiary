package com.kiraya.diary.ui.tenants

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kiraya.diary.R
import com.kiraya.diary.core.date.DateUtils
import com.kiraya.diary.domain.model.TenantWithUnit
import com.kiraya.diary.ui.components.ConfirmDialog
import com.kiraya.diary.ui.lease.LeaseCard
import com.kiraya.diary.ui.payments.PaymentsSection
import com.kiraya.diary.ui.rent.ChargesSection
import com.kiraya.diary.ui.components.KirayaTopBar
import com.kiraya.diary.ui.components.LocalMoneyDisplay
import com.kiraya.diary.ui.components.MoneyText
import com.kiraya.diary.ui.components.PhotoThumb
import com.kiraya.diary.ui.components.StatusPill
import com.kiraya.diary.ui.theme.KirayaColors
import com.kiraya.diary.ui.util.Intents

/** Actions on the tenant detail screen. Features add their own as they land. */
data class TenantDetailActions(
    val onBack: () -> Unit,
    val onEdit: (Long) -> Unit,
    val onEditLease: (Long) -> Unit,
    val onEditRentEntry: (Long) -> Unit,
    val onOpenBill: (Long) -> Unit,
    val onRecordPayment: (Long) -> Unit,
    val onEditPayment: (Long) -> Unit,
    val onReceipt: (Long) -> Unit,
)

@Composable
fun TenantDetailScreen(
    actions: TenantDetailActions,
    modifier: Modifier = Modifier,
    viewModel: TenantDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val tenant = state.tenant
    var menuOpen by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Scaffold(
        modifier = modifier,
        topBar = {
            KirayaTopBar(
                title = tenant?.tenant?.name.orEmpty(),
                onBack = actions.onBack,
                actions = {
                    if (tenant != null) {
                        if (tenant.tenant.phone.isNotBlank()) {
                            IconButton(onClick = { Intents.dial(context, tenant.tenant.phone) }) {
                                Icon(painterResource(R.drawable.ic_phone), stringResource(R.string.tenant_call))
                            }
                        }
                        IconButton(onClick = { actions.onEdit(tenant.tenant.id) }) {
                            Icon(painterResource(R.drawable.ic_edit), stringResource(R.string.action_edit))
                        }
                        IconButton(onClick = { menuOpen = true }) {
                            Icon(painterResource(R.drawable.ic_more), contentDescription = null)
                        }
                        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.tenant_delete)) },
                                onClick = {
                                    menuOpen = false
                                    confirmDelete = true
                                },
                            )
                        }
                    }
                },
            )
        },
    ) { padding ->
        if (tenant == null) return@Scaffold
        LazyColumn(
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = padding.calculateTopPadding() + 8.dp,
                bottom = padding.calculateBottomPadding() + 24.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            tenantSections(state, tenant, actions)
        }
    }

    if (confirmDelete) {
        ConfirmDialog(
            title = stringResource(R.string.tenant_delete),
            message = stringResource(R.string.tenant_delete_message),
            confirmLabel = stringResource(R.string.action_delete),
            onConfirm = { viewModel.delete(actions.onBack) },
            onDismiss = { confirmDelete = false },
        )
    }
}

private fun LazyListScope.tenantSections(
    state: TenantDetailState,
    tenant: TenantWithUnit,
    actions: TenantDetailActions,
) {
    item { BalanceCard(state, onRecordPayment = { actions.onRecordPayment(tenant.tenant.id) }) }
    item { InfoCard(tenant) }
    item {
        ChargesSection(
            charges = state.ledger.charges,
            onRentClick = actions.onEditRentEntry,
            onBillClick = actions.onOpenBill,
        )
    }
    item {
        PaymentsSection(
            payments = state.ledger.payments,
            onPaymentClick = actions.onEditPayment,
            onReceiptClick = actions.onReceipt,
        )
    }
    if (tenant.tenant.isActive || state.lease != null) {
        item { LeaseCard(state.lease, onEdit = { actions.onEditLease(tenant.tenant.id) }) }
    }
    tenant.tenant.cnicPhoto?.let { photo ->
        item { PhotoThumb(photo, stringResource(R.string.tenant_cnic_photo)) }
    }
    if (tenant.tenant.notes.isNotBlank()) {
        item {
            OutlinedCard(Modifier.fillMaxWidth()) {
                Text(tenant.tenant.notes, Modifier.padding(16.dp), style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun BalanceCard(state: TenantDetailState, onRecordPayment: () -> Unit) {
    val ledger = state.ledger
    val tenant = state.tenant?.tenant ?: return
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
    ) {
        Column(Modifier.padding(20.dp)) {
            if (!tenant.isActive) {
                StatusPill(
                    stringResource(R.string.tenant_moved_out_on, DateUtils.formatDate(tenant.moveOutDate ?: 0)),
                    MaterialTheme.colorScheme.error,
                )
            }
            when {
                ledger.pending > 0 -> {
                    Text(stringResource(R.string.tenant_balance_due), style = MaterialTheme.typography.bodyMedium)
                    MoneyText(ledger.pending, style = MaterialTheme.typography.headlineMedium, color = KirayaColors.status.unpaid)
                }
                ledger.credit > 0 -> {
                    Text(stringResource(R.string.tenant_balance_credit), style = MaterialTheme.typography.bodyMedium)
                    MoneyText(ledger.credit, style = MaterialTheme.typography.headlineMedium, color = KirayaColors.status.paid)
                }
                else -> {
                    Text(stringResource(R.string.tenant_balance_clear), style = MaterialTheme.typography.titleMedium)
                }
            }
            Row(Modifier.fillMaxWidth().padding(top = 12.dp)) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.tenant_total_charged), style = MaterialTheme.typography.bodySmall)
                    MoneyText(ledger.totalCharged, style = MaterialTheme.typography.titleSmall)
                }
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.tenant_total_paid), style = MaterialTheme.typography.bodySmall)
                    MoneyText(ledger.totalPaid, style = MaterialTheme.typography.titleSmall)
                }
            }
            Button(onClick = onRecordPayment, modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
                Icon(painterResource(R.drawable.ic_payment), contentDescription = null)
                Text(stringResource(R.string.payment_add), Modifier.padding(start = 8.dp))
            }
        }
    }
}

@Composable
private fun InfoCard(t: TenantWithUnit) {
    val money = LocalMoneyDisplay.current
    OutlinedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            val place = listOfNotNull(t.propertyName, t.unitName).joinToString(" · ")
            InfoRow(stringResource(R.string.tenant_unit), place.ifEmpty { stringResource(R.string.tenant_no_unit) })
            InfoRow(stringResource(R.string.tenant_rent), money.format(t.tenant.rentAmount))
            InfoRow(stringResource(R.string.tenant_deposit), money.format(t.tenant.securityDeposit))
            InfoRow(stringResource(R.string.tenant_start_date), DateUtils.formatDate(t.tenant.startDate))
            InfoRow(stringResource(R.string.tenant_due_day), t.tenant.dueDay.toString())
            if (t.tenant.phone.isNotBlank()) InfoRow(stringResource(R.string.tenant_phone), t.tenant.phone)
            if (t.tenant.cnic.isNotBlank()) InfoRow(stringResource(R.string.tenant_cnic), t.tenant.cnic, last = true)
        }
    }
}

@Composable
fun InfoRow(label: String, value: String, last: Boolean = false) {
    Row(Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1.4f),
        )
    }
    if (!last) HorizontalDivider()
}
