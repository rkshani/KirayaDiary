package com.kiraya.diary.ui.rent

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.kiraya.diary.R
import com.kiraya.diary.core.date.DateUtils
import com.kiraya.diary.domain.ledger.ChargeKind
import com.kiraya.diary.domain.ledger.LedgerCharge
import com.kiraya.diary.ui.components.LocalMoneyDisplay
import com.kiraya.diary.ui.components.MoneyText
import com.kiraya.diary.ui.components.RentStatusPill
import com.kiraya.diary.ui.components.labelRes

/** A tenant's monthly rent and bill charges, newest first, with paid/partial/unpaid status. */
@Composable
fun ChargesSection(
    charges: List<LedgerCharge>,
    onRentClick: (entryId: Long) -> Unit,
    onBillClick: (billId: Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showAll by rememberSaveable { mutableStateOf(false) }
    val newestFirst = charges.reversed()
    val shown = if (showAll) newestFirst else newestFirst.take(COLLAPSED)
    OutlinedCard(modifier.fillMaxWidth()) {
        Column(Modifier.padding(vertical = 8.dp)) {
            Text(
                stringResource(R.string.charges_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
            if (charges.isEmpty()) {
                Text(
                    stringResource(R.string.charges_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
            shown.forEachIndexed { index, charge ->
                if (index > 0) HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                ChargeRow(
                    charge,
                    onClick = {
                        when (charge.kind) {
                            ChargeKind.RENT -> onRentClick(charge.refId)
                            ChargeKind.BILL -> charge.billId?.let(onBillClick)
                        }
                    },
                )
            }
            if (newestFirst.size > COLLAPSED) {
                TextButton(onClick = { showAll = !showAll }, modifier = Modifier.padding(horizontal = 8.dp)) {
                    Text(stringResource(if (showAll) R.string.charges_show_less else R.string.charges_show_all))
                }
            }
        }
    }
}

@Composable
private fun ChargeRow(charge: LedgerCharge, onClick: () -> Unit) {
    val money = LocalMoneyDisplay.current
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            val what = when (charge.kind) {
                ChargeKind.RENT -> stringResource(R.string.charge_rent)
                ChargeKind.BILL -> charge.billTitle.ifBlank {
                    charge.billType?.let { stringResource(it.labelRes) } ?: stringResource(R.string.charge_bill)
                }
            }
            Text("${DateUtils.formatMonthShort(charge.month)} · $what", style = MaterialTheme.typography.bodyLarge)
            if (charge.paid in 1 until charge.amount) {
                Text(
                    stringResource(R.string.charge_paid_of, money.format(charge.paid), money.format(charge.amount)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Text(
                    stringResource(R.string.charge_due_on, DateUtils.formatDate(charge.dueDate)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            MoneyText(charge.amount, style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(4.dp))
            RentStatusPill(charge.status)
        }
    }
}

private const val COLLAPSED = 6
