package com.kiraya.diary.ui.tenants

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.background
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kiraya.diary.R
import com.kiraya.diary.ui.components.EmptyState
import com.kiraya.diary.ui.components.KirayaTopBar
import com.kiraya.diary.ui.components.MoneyText
import com.kiraya.diary.ui.components.StatusPill
import com.kiraya.diary.ui.theme.KirayaColors

@Composable
fun TenantsScreen(
    onAdd: () -> Unit,
    onOpen: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TenantsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    Scaffold(
        modifier = modifier,
        topBar = { KirayaTopBar(title = stringResource(R.string.nav_tenants)) },
        floatingActionButton = {
            if (state.totalCount > 0) {
                ExtendedFloatingActionButton(
                    onClick = onAdd,
                    icon = { Icon(painterResource(R.drawable.ic_add), contentDescription = null) },
                    text = { Text(stringResource(R.string.tenant_add)) },
                )
            }
        },
    ) { padding ->
        if (!state.loaded) return@Scaffold
        if (state.totalCount == 0) {
            EmptyState(
                iconRes = R.drawable.ic_tenants,
                title = stringResource(R.string.tenants_empty_title),
                message = stringResource(R.string.tenants_empty_message),
                modifier = Modifier.padding(padding),
                action = { Button(onClick = onAdd) { Text(stringResource(R.string.tenant_add)) } },
            )
            return@Scaffold
        }
        Column(Modifier.padding(top = padding.calculateTopPadding())) {
            PrimaryTabRow(selectedTabIndex = state.filter.ordinal) {
                Tab(
                    selected = state.filter == TenantFilter.ACTIVE,
                    onClick = { viewModel.onFilter(TenantFilter.ACTIVE) },
                    text = { Text(stringResource(R.string.tenants_active)) },
                )
                Tab(
                    selected = state.filter == TenantFilter.MOVED_OUT,
                    onClick = { viewModel.onFilter(TenantFilter.MOVED_OUT) },
                    text = { Text(stringResource(R.string.tenants_moved_out)) },
                )
            }
            OutlinedTextField(
                value = state.query,
                onValueChange = viewModel::onQuery,
                placeholder = { Text(stringResource(R.string.tenants_search)) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            )
            LazyColumn(
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 4.dp,
                    bottom = padding.calculateBottomPadding() + 88.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (state.items.isEmpty()) {
                    item {
                        Text(
                            stringResource(R.string.tenants_none_found),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(16.dp),
                        )
                    }
                }
                items(state.items, key = { it.tenant.tenant.id }) { item ->
                    TenantRow(item, onClick = { onOpen(item.tenant.tenant.id) })
                }
            }
        }
    }
}

@Composable
private fun TenantRow(item: TenantListItem, onClick: () -> Unit) {
    val t = item.tenant
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    t.tenant.name.trim().take(1).uppercase(),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(t.tenant.name, style = MaterialTheme.typography.titleMedium)
                val place = listOfNotNull(t.propertyName, t.unitName).joinToString(" · ")
                Text(
                    place.ifEmpty { stringResource(R.string.tenant_no_unit) },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                MoneyText(t.tenant.rentAmount, style = MaterialTheme.typography.bodySmall)
            }
            Column(horizontalAlignment = Alignment.End) {
                if (item.pending > 0) {
                    MoneyText(
                        item.pending,
                        style = MaterialTheme.typography.titleSmall,
                        color = if (item.overdue) KirayaColors.status.unpaid else KirayaColors.status.partial,
                    )
                    Text(stringResource(R.string.home_pending), style = MaterialTheme.typography.labelSmall)
                } else if (t.tenant.isActive) {
                    StatusPill(stringResource(R.string.tenant_clear), KirayaColors.status.paid)
                }
            }
        }
    }
}
