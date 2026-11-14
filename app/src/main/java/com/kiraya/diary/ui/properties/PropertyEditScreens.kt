package com.kiraya.diary.ui.properties

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kiraya.diary.R
import com.kiraya.diary.domain.model.UnitType
import com.kiraya.diary.ui.components.ChoiceChips
import com.kiraya.diary.ui.components.FormScaffold
import com.kiraya.diary.ui.components.MoneyField
import com.kiraya.diary.ui.components.TextInput
import com.kiraya.diary.ui.components.labelRes

@Composable
fun PropertyEditScreen(
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PropertyEditViewModel = hiltViewModel(),
) {
    val form by viewModel.form.collectAsStateWithLifecycle()
    LaunchedEffect(form.saved) { if (form.saved) onDone() }
    FormScaffold(
        title = stringResource(if (viewModel.isEdit) R.string.property_edit else R.string.property_add),
        onBack = onDone,
        onSave = viewModel::save,
        modifier = modifier,
    ) {
        TextInput(
            value = form.name,
            onValueChange = viewModel::onName,
            label = stringResource(R.string.property_name),
            isError = form.nameError,
            supportingText = if (form.nameError) stringResource(R.string.error_required) else null,
        )
        TextInput(form.address, viewModel::onAddress, stringResource(R.string.property_address))
        TextInput(form.notes, viewModel::onNotes, stringResource(R.string.field_notes), singleLine = false)
    }
}

@Composable
fun UnitEditScreen(
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: UnitEditViewModel = hiltViewModel(),
) {
    val form by viewModel.form.collectAsStateWithLifecycle()
    LaunchedEffect(form.saved) { if (form.saved) onDone() }
    FormScaffold(
        title = stringResource(if (viewModel.isEdit) R.string.unit_edit else R.string.unit_add),
        onBack = onDone,
        onSave = viewModel::save,
        modifier = modifier,
    ) {
        TextInput(
            value = form.name,
            onValueChange = viewModel::onName,
            label = stringResource(R.string.unit_name),
            isError = form.nameError,
            supportingText = if (form.nameError) stringResource(R.string.error_required) else stringResource(R.string.unit_name_hint),
        )
        ChoiceChips(
            options = UnitType.entries,
            selected = form.type,
            label = { stringResource(it.labelRes) },
            onSelect = viewModel::onType,
            title = stringResource(R.string.unit_type),
        )
        MoneyField(
            value = form.rent,
            onValueChange = viewModel::onRent,
            label = stringResource(R.string.unit_default_rent),
            isError = form.rentError,
            supportingText = if (form.rentError) stringResource(R.string.error_amount) else null,
        )
        TextInput(form.notes, viewModel::onNotes, stringResource(R.string.field_notes), singleLine = false)
    }
}
