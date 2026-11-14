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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kiraya.diary.R
import com.kiraya.diary.domain.model.PropertyWithCounts
import com.kiraya.diary.ui.components.EmptyState
import com.kiraya.diary.ui.components.KirayaTopBar
import com.kiraya.diary.ui.components.StatusPill
import com.kiraya.diary.ui.theme.KirayaColors

@Composable
fun PropertiesScreen(
    onBack: () -> Unit,
    onAdd: () -> Unit,
    onOpen: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PropertiesViewModel = hiltViewModel(),
) {
    val properties by viewModel.properties.collectAsStateWithLifecycle()
    Scaffold(
        modifier = modifier,
        topBar = { KirayaTopBar(title = stringResource(R.string.properties_title), onBack = onBack) },
        floatingActionButton = {
            if (!properties.isNullOrEmpty()) {
                ExtendedFloatingActionButton(
                    onClick = onAdd,
                    icon = { Icon(painterResource(R.drawable.ic_add), contentDescription = null) },
                    text = { Text(stringResource(R.string.property_add)) },
                )
            }
        },
    ) { padding ->
        val list = properties
        when {
            list == null -> Unit
            list.isEmpty() -> EmptyState(
                iconRes = R.drawable.ic_property,
                title = stringResource(R.string.properties_empty_title),
                message = stringResource(R.string.properties_empty_message),
                modifier = Modifier.padding(padding),
                action = { Button(onClick = onAdd) { Text(stringResource(R.string.property_add)) } },
            )
            else -> LazyColumn(
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = padding.calculateTopPadding() + 8.dp,
                    bottom = padding.calculateBottomPadding() + 88.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(list, key = { it.property.id }) { item ->
                    PropertyCard(item, onClick = { onOpen(item.property.id) })
                }
            }
        }
    }
}

@Composable
private fun PropertyCard(item: PropertyWithCounts, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painterResource(R.drawable.ic_property),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(item.property.name, style = MaterialTheme.typography.titleMedium)
                if (item.property.address.isNotBlank()) {
                    Text(
                        item.property.address,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.padding(top = 6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatusPill(
                        stringResource(R.string.units_count, item.unitCount),
                        MaterialTheme.colorScheme.primary,
                    )
                    if (item.vacantCount > 0) {
                        StatusPill(
                            stringResource(R.string.vacant_count, item.vacantCount),
                            KirayaColors.status.partial,
                        )
                    }
                }
            }
            Icon(painterResource(R.drawable.ic_chevron_right), contentDescription = null)
        }
    }
}
