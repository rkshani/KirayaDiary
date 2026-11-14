package com.kiraya.diary.ui.reports

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kiraya.diary.R
import com.kiraya.diary.core.date.DateUtils
import com.kiraya.diary.domain.usecase.Report
import com.kiraya.diary.ui.components.KirayaTopBar
import com.kiraya.diary.ui.components.LocalMoneyDisplay
import com.kiraya.diary.ui.components.MoneyText
import com.kiraya.diary.ui.components.PeriodSwitcher
import com.kiraya.diary.ui.components.SectionTitle
import com.kiraya.diary.ui.theme.KirayaColors
import com.kiraya.diary.ui.util.Share

@Composable
fun ReportsScreen(
    modifier: Modifier = Modifier,
    viewModel: ReportsViewModel = hiltViewModel(),
) {
    val report by viewModel.report.collectAsStateWithLifecycle()
    val period by viewModel.periodState.collectAsStateWithLifecycle()
    val exporting by viewModel.exporting.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val chooser = stringResource(R.string.report_share_title)

    Scaffold(
        modifier = modifier,
        topBar = { KirayaTopBar(title = stringResource(R.string.nav_reports)) },
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
                PeriodSwitcher(period, viewModel::previous, viewModel::next, viewModel::showYear)
            }
            val r = report ?: return@LazyColumn
            item { SummaryCard(r) }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    listOf(ExportType.PDF to R.string.report_export_pdf, ExportType.CSV to R.string.report_export_csv).forEach { (type, label) ->
                        OutlinedButton(
                            onClick = {
                                viewModel.export(type, context) { file, mime -> Share.shareFile(context, file, mime, chooser) }
                            },
                            enabled = !exporting,
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(painterResource(R.drawable.ic_download), contentDescription = null)
                            Text(stringResource(label), Modifier.padding(start = 8.dp))
                        }
                    }
                }
            }
            if (r.properties.isNotEmpty()) {
                item { SectionTitle(stringResource(R.string.report_by_property)) }
                item { PropertyTable(r) }
            }
            if (r.months.isNotEmpty()) {
                item { SectionTitle(stringResource(R.string.report_by_month)) }
                item { MonthTable(r) }
            }
            item { SectionTitle(stringResource(R.string.report_pending)) }
            if (r.pending.isEmpty()) {
                item {
                    Text(
                        stringResource(R.string.report_no_pending),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            items(r.pending) { p ->
                OutlinedCard(Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(p.tenantName, style = MaterialTheme.typography.titleSmall)
                            val sub = listOfNotNull(
                                p.place.ifBlank { null },
                                p.oldestMonth?.let { stringResource(R.string.report_since_month, DateUtils.formatMonthShort(it)) },
                            )
                            if (sub.isNotEmpty()) {
                                Text(sub.joinToString(" · "), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        MoneyText(p.amount, style = MaterialTheme.typography.titleSmall, color = KirayaColors.status.unpaid)
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryCard(r: Report) {
    ElevatedCard(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row {
                Stat(R.string.report_charged, r.charged, Modifier.weight(1f))
                Stat(R.string.home_collected, r.collected, Modifier.weight(1f))
            }
            Row {
                Stat(R.string.profit_expenses, r.expenses, Modifier.weight(1f))
                Stat(R.string.profit_net, r.profit, Modifier.weight(1f))
            }
            Stat(R.string.report_pending_total, r.totalPending)
        }
    }
}

@Composable
private fun Stat(label: Int, amount: Long, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(stringResource(label), style = MaterialTheme.typography.bodySmall)
        MoneyText(amount, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun PropertyTable(r: Report) {
    val money = LocalMoneyDisplay.current
    OutlinedCard(Modifier.fillMaxWidth()) {
        r.properties.forEachIndexed { i, row ->
            if (i > 0) HorizontalDivider(Modifier.padding(horizontal = 16.dp))
            Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        row.propertyName ?: stringResource(R.string.profit_no_property),
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.weight(1f),
                    )
                    MoneyText(
                        row.profit,
                        style = MaterialTheme.typography.titleSmall,
                        color = if (row.profit < 0) KirayaColors.status.unpaid else KirayaColors.status.paid,
                    )
                }
                Text(
                    stringResource(R.string.report_property_line, money.format(row.charged), money.format(row.collected), money.format(row.expenses)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun MonthTable(r: Report) {
    val money = LocalMoneyDisplay.current
    OutlinedCard(Modifier.fillMaxWidth()) {
        r.months.filter { it.charged > 0 || it.collected > 0 || it.expenses > 0 }
            .forEachIndexed { i, m ->
                if (i > 0) HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                Row(Modifier.padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(DateUtils.formatMonthShort(m.month), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                    Column(horizontalAlignment = Alignment.End) {
                        MoneyText(m.collected, style = MaterialTheme.typography.titleSmall)
                        Text(
                            stringResource(R.string.report_month_line, money.format(m.charged), money.format(m.expenses)),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
    }
}
