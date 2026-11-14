package com.kiraya.diary.ui.properties

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kiraya.diary.R
import com.kiraya.diary.domain.model.UnitWithStatus
import com.kiraya.diary.ui.components.ConfirmDialog
import com.kiraya.diary.ui.components.KirayaTopBar
import com.kiraya.diary.ui.components.MoneyText
import com.kiraya.diary.ui.components.SectionTitle
import com.kiraya.diary.ui.components.StatusPill
import com.kiraya.diary.ui.components.labelRes
import com.kiraya.diary.ui.theme.KirayaColors

@Composable
fun PropertyDetailScreen(
    onBack: () -> Unit,
    onEdit: (Long) -> Unit,
    onAddUnit: (Long) -> Unit,
    onEditUnit: (propertyId: Long, unitId: Long) -> Unit,
    onTenantClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PropertyDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val property = state.property
    var menuOpen by remember { mutableStateOf(false) }
    var confirmDeleteProperty by remember { mutableStateOf(false) }
    var unitToDelete by remember { mutableStateOf<UnitWithStatus?>(null) }

    Scaffold(
        modifier = modifier,
        topBar = {
            KirayaTopBar(
                title = property?.name ?: "",
                onBack = onBack,
                actions = {
                    if (property != null) {
                        IconButton(onClick = { onEdit(property.id) }) {
                            Icon(painterResource(R.drawable.ic_edit), stringResource(R.string.action_edit))
                        }
                        IconButton(onClick = { menuOpen = true }) {
                            Icon(painterResource(R.drawable.ic_more), contentDescription = null)
                        }
                        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.property_delete)) },
                                onClick = {
                                    menuOpen = false
                                    confirmDeleteProperty = true
                                },
                            )
                        }
                    }
                },
            )
        },
        floatingActionButton = {
            if (property != null) {
                ExtendedFloatingActionButton(
                    onClick = { onAddUnit(property.id) },
                    icon = { Icon(painterResource(R.drawable.ic_add), contentDescription = null) },
                    text = { Text(stringResource(R.string.unit_add)) },
                )
            }
        },
    ) { padding ->
        if (property == null) return@Scaffold
        LazyColumn(
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = padding.calculateTopPadding() + 8.dp,
                bottom = padding.calculateBottomPadding() + 88.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (property.address.isNotBlank() || property.notes.isNotBlank()) {
                item {
                    OutlinedCard(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp)) {
                            if (property.address.isNotBlank()) {
                                Text(property.address, style = MaterialTheme.typography.bodyMedium)
                            }
                            if (property.notes.isNotBlank()) {
                                Text(
                                    property.notes,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
            item {
                val occupied = state.units.count { it.isOccupied }
                SectionTitle(
                    stringResource(R.string.units_summary, state.units.size, occupied, state.units.size - occupied),
                )
            }
            if (state.units.isEmpty()) {
                item {
                    Text(
                        stringResource(R.string.units_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            items(state.units, key = { it.unit.id }) { unit ->
                UnitRow(
                    unit = unit,
                    onEdit = { onEditUnit(property.id, unit.unit.id) },
                    onDelete = { unitToDelete = unit },
                    onTenantClick = onTenantClick,
                )
            }
        }
    }

    if (confirmDeleteProperty) {
        ConfirmDialog(
            title = stringResource(R.string.property_delete),
            message = stringResource(R.string.property_delete_message),
            confirmLabel = stringResource(R.string.action_delete),
            onConfirm = { viewModel.deleteProperty(onBack) },
            onDismiss = { confirmDeleteProperty = false },
        )
    }
    unitToDelete?.let { unit ->
        ConfirmDialog(
            title = stringResource(R.string.unit_delete),
            message = if (unit.isOccupied) {
                stringResource(R.string.unit_delete_occupied_message, unit.tenantName.orEmpty())
            } else {
                stringResource(R.string.unit_delete_message)
            },
            confirmLabel = stringResource(R.string.action_delete),
            onConfirm = { viewModel.deleteUnit(unit.unit.id) },
            onDismiss = { unitToDelete = null },
        )
    }
}

@Composable
private fun UnitRow(
    unit: UnitWithStatus,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onTenantClick: (Long) -> Unit,
) {
    Card(onClick = onEdit, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(start = 16.dp, top = 12.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(unit.unit.name, style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        stringResource(unit.unit.type.labelRes),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (unit.unit.defaultRent > 0) {
                    MoneyText(unit.unit.defaultRent, style = MaterialTheme.typography.bodyMedium)
                }
                Spacer(Modifier.padding(top = 4.dp))
                if (unit.isOccupied) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        StatusPill(stringResource(R.string.unit_occupied), KirayaColors.status.paid)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            unit.tenantName.orEmpty(),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(4.dp),
                        )
                        IconButton(onClick = { unit.tenantId?.let(onTenantClick) }) {
                            Icon(painterResource(R.drawable.ic_chevron_right), contentDescription = null)
                        }
                    }
                } else {
                    StatusPill(stringResource(R.string.unit_vacant), KirayaColors.status.partial)
                }
            }
            IconButton(onClick = onDelete) {
                Icon(painterResource(R.drawable.ic_delete), stringResource(R.string.action_delete))
            }
        }
    }
}
