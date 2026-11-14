package com.kiraya.diary.ui.reminders

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kiraya.diary.R
import com.kiraya.diary.core.date.DateUtils
import com.kiraya.diary.domain.usecase.DueTenant
import com.kiraya.diary.ui.components.ChoiceChips
import com.kiraya.diary.ui.components.KirayaTopBar
import com.kiraya.diary.ui.components.MoneyText
import com.kiraya.diary.ui.components.SectionTitle
import com.kiraya.diary.ui.theme.KirayaColors

@Composable
fun RemindersScreen(
    onBack: () -> Unit,
    onTenantClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RemindersViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var sendTo by remember { mutableStateOf<DueTenant?>(null) }
    var permissionGranted by remember {
        mutableStateOf(
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED,
        )
    }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        permissionGranted = it
    }
    val s = state.settings

    Scaffold(
        modifier = modifier,
        topBar = { KirayaTopBar(title = stringResource(R.string.reminders_title), onBack = onBack) },
    ) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = padding.calculateTopPadding() + 8.dp,
                bottom = padding.calculateBottomPadding() + 24.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                OutlinedCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(stringResource(R.string.reminders_enable), style = MaterialTheme.typography.titleSmall)
                                Text(
                                    stringResource(R.string.reminders_enable_hint),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Switch(checked = s.remindersEnabled, onCheckedChange = viewModel::setEnabled)
                        }
                        if (s.remindersEnabled && !permissionGranted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            Button(onClick = { permission.launch(Manifest.permission.POST_NOTIFICATIONS) }) {
                                Text(stringResource(R.string.reminders_allow))
                            }
                        }
                        ChoiceChips(
                            options = listOf(0, 1, 2, 3, 5),
                            selected = s.reminderDaysBefore,
                            label = { if (it == 0) stringResource(R.string.reminders_on_due_day) else stringResource(R.string.reminders_days_before, it) },
                            onSelect = viewModel::setDaysBefore,
                            title = stringResource(R.string.reminders_when),
                        )
                        ChoiceChips(
                            options = listOf(15, 30, 60, 90),
                            selected = s.leaseAlertDays,
                            label = { stringResource(R.string.reminders_days, it) },
                            onSelect = viewModel::setLeaseAlertDays,
                            title = stringResource(R.string.reminders_lease_alert),
                        )
                        ChoiceChips(
                            options = listOf("ur", "en"),
                            selected = s.messageLanguage,
                            label = { stringResource(if (it == "ur") R.string.language_urdu else R.string.language_english) },
                            onSelect = viewModel::setMessageLanguage,
                            title = stringResource(R.string.reminders_message_language),
                        )
                        OutlinedButton(onClick = viewModel::testNow, enabled = s.remindersEnabled && permissionGranted) {
                            Text(stringResource(R.string.reminders_test))
                        }
                    }
                }
            }

            item { SectionTitle(stringResource(R.string.reminders_due_title)) }
            if (state.loaded && state.due.isEmpty()) {
                item {
                    Text(
                        stringResource(R.string.reminders_none_due),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            items(state.due, key = { it.tenant.tenant.id }) { due ->
                Card(onClick = { onTenantClick(due.tenant.tenant.id) }, modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(due.tenant.tenant.name, style = MaterialTheme.typography.titleSmall)
                            Text(
                                if (due.overdue) {
                                    stringResource(R.string.reminders_overdue_since, DateUtils.formatDate(due.oldestDueDate))
                                } else {
                                    stringResource(R.string.charge_due_on, DateUtils.formatDate(due.oldestDueDate))
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = if (due.overdue) KirayaColors.status.unpaid else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            MoneyText(due.amount, style = MaterialTheme.typography.bodyMedium)
                        }
                        IconButton(onClick = { sendTo = due }) {
                            Icon(painterResource(R.drawable.ic_message), stringResource(R.string.reminder_send_title))
                        }
                    }
                }
            }

            item { SectionTitle(stringResource(R.string.reminders_leases_title)) }
            if (state.loaded && state.leases.isEmpty()) {
                item {
                    Text(
                        stringResource(R.string.reminders_no_leases),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            items(state.leases, key = { "lease_" + it.lease.id }) { alert ->
                val end = alert.lease.endDate ?: 0L
                val days = DateUtils.daysBetween(DateUtils.now(), end)
                Card(onClick = { onTenantClick(alert.lease.tenantId) }, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text(alert.tenantName, style = MaterialTheme.typography.titleSmall)
                        Text(
                            if (days < 0) stringResource(R.string.lease_ended) else stringResource(R.string.lease_ends_in, days),
                            style = MaterialTheme.typography.bodySmall,
                            color = if (days < 0) KirayaColors.status.unpaid else KirayaColors.status.partial,
                        )
                        Text(DateUtils.formatDate(end), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }

    sendTo?.let { due ->
        SendReminderDialog(
            phone = due.tenant.tenant.phone,
            defaultLanguage = s.messageLanguage,
            buildMessage = { lang -> viewModel.message(due, lang) },
            onDismiss = { sendTo = null },
        )
    }
}
