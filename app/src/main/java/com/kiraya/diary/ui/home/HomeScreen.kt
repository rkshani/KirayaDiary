package com.kiraya.diary.ui.home

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kiraya.diary.R
import com.kiraya.diary.core.date.DateUtils
import com.kiraya.diary.ui.components.KirayaTopBar
import com.kiraya.diary.ui.components.MoneyText
import com.kiraya.diary.ui.theme.KirayaColors

data class QuickAction(
    @StringRes val labelRes: Int,
    @DrawableRes val iconRes: Int,
    val onClick: () -> Unit,
)

@Composable
fun HomeScreen(
    onTenantClick: (Long) -> Unit,
    quickActions: List<QuickAction>,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val hidden by viewModel.amountsHidden.collectAsStateWithLifecycle()
    Scaffold(
        modifier = modifier,
        topBar = {
            KirayaTopBar(
                title = stringResource(R.string.app_name),
                actions = {
                    IconButton(onClick = viewModel::toggleAmounts) {
                        Icon(
                            painterResource(R.drawable.ic_visibility),
                            stringResource(if (hidden) R.string.lock_show_amounts else R.string.lock_hide_amounts),
                            tint = if (hidden) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface,
                        )
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = padding.calculateTopPadding() + 8.dp,
                bottom = padding.calculateBottomPadding() + 24.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item { SummaryCard(state) }
            item { StatsRow(state) }
            if (quickActions.isNotEmpty()) {
                item { QuickActions(quickActions) }
            }
            item {
                Text(
                    stringResource(R.string.home_overdue_title),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            if (state.overdue.isEmpty() && !state.loading) {
                item {
                    OutlinedCard(Modifier.fillMaxWidth()) {
                        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                painterResource(R.drawable.ic_check),
                                contentDescription = null,
                                tint = KirayaColors.status.paid,
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(stringResource(R.string.home_no_overdue), style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
            items(state.overdue, key = { it.tenant.tenant.id }) { item ->
                OverdueRow(item, onClick = { onTenantClick(item.tenant.tenant.id) })
            }
        }
    }
}

@Composable
private fun SummaryCard(state: HomeUiState) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
    ) {
        Column(Modifier.padding(20.dp)) {
            Text(
                stringResource(R.string.home_this_month, DateUtils.formatMonth(state.month)),
                style = MaterialTheme.typography.labelLarge,
            )
            Spacer(Modifier.height(12.dp))
            Text(stringResource(R.string.home_collected), style = MaterialTheme.typography.bodyMedium)
            MoneyText(state.collectedThisMonth, style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(12.dp))
            val progress = if (state.dueThisMonth > 0) {
                (state.dueThisMonth - state.pendingThisMonth).toFloat() / state.dueThisMonth
            } else {
                0f
            }
            LinearProgressIndicator(
                progress = { progress.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(CircleShape),
            )
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth()) {
                LabeledAmount(R.string.home_due, state.dueThisMonth, Modifier.weight(1f))
                LabeledAmount(R.string.home_pending, state.pendingThisMonth, Modifier.weight(1f))
            }
            if (state.totalPending != state.pendingThisMonth) {
                Spacer(Modifier.height(12.dp))
                LabeledAmount(R.string.home_total_pending, state.totalPending)
            }
        }
    }
}

@Composable
private fun LabeledAmount(@StringRes label: Int, amount: Long, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(stringResource(label), style = MaterialTheme.typography.bodySmall)
        MoneyText(amount, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun StatsRow(state: HomeUiState) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        StatCard(R.string.home_active_tenants, state.activeTenants.toString(), Modifier.weight(1f))
        StatCard(R.string.home_units, state.unitCount.toString(), Modifier.weight(1f))
        StatCard(R.string.home_vacant, state.vacantUnits.toString(), Modifier.weight(1f))
    }
}

@Composable
private fun StatCard(@StringRes label: Int, value: String, modifier: Modifier = Modifier) {
    Card(modifier) {
        Column(Modifier.padding(12.dp)) {
            Text(value, style = MaterialTheme.typography.titleLarge)
            Text(
                stringResource(label),
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun QuickActions(actions: List<QuickAction>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.home_quick_actions), style = MaterialTheme.typography.titleMedium)
        actions.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { action ->
                    OutlinedCard(
                        onClick = action.onClick,
                        modifier = Modifier.weight(1f),
                    ) {
                        Row(
                            Modifier.padding(horizontal = 12.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                painterResource(action.iconRes),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                stringResource(action.labelRes),
                                style = MaterialTheme.typography.labelLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun OverdueRow(item: OverdueTenant, onClick: () -> Unit) {
    val days = DateUtils.daysBetween(item.oldestDueDate, DateUtils.now())
    Card(Modifier.fillMaxWidth()) {
        Row(
            Modifier
                .clickable(onClick = onClick)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painterResource(R.drawable.ic_warning),
                contentDescription = null,
                tint = KirayaColors.status.unpaid,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(item.tenant.tenant.name, style = MaterialTheme.typography.titleSmall)
                val place = listOfNotNull(item.tenant.propertyName, item.tenant.unitName).joinToString(" · ")
                if (place.isNotEmpty()) {
                    Text(place, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(
                    stringResource(R.string.home_days_overdue, days),
                    style = MaterialTheme.typography.bodySmall,
                    color = KirayaColors.status.unpaid,
                )
            }
            MoneyText(item.overdueAmount, style = MaterialTheme.typography.titleSmall, color = KirayaColors.status.unpaid)
        }
    }
}
