package com.kiraya.diary.ui.tenants

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kiraya.diary.R
import com.kiraya.diary.domain.model.UnitWithStatus
import com.kiraya.diary.ui.components.DateField
import com.kiraya.diary.ui.components.FormScaffold
import com.kiraya.diary.ui.components.MoneyField
import com.kiraya.diary.ui.components.PhotoField
import com.kiraya.diary.ui.components.PickerField
import com.kiraya.diary.ui.components.PickerOption
import com.kiraya.diary.ui.components.TextInput

@Composable
fun TenantEditScreen(
    onDone: (savedId: Long?) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TenantEditViewModel = hiltViewModel(),
) {
    val form by viewModel.form.collectAsStateWithLifecycle()
    val units by viewModel.units.collectAsStateWithLifecycle()
    LaunchedEffect(form.saved) { if (form.saved) onDone(form.savedId) }

    FormScaffold(
        title = stringResource(if (viewModel.isEdit) R.string.tenant_edit else R.string.tenant_add),
        onBack = { onDone(null) },
        onSave = viewModel::save,
        modifier = modifier,
    ) {
        TextInput(
            value = form.name,
            onValueChange = viewModel::onName,
            label = stringResource(R.string.tenant_name),
            isError = form.nameError,
            supportingText = if (form.nameError) stringResource(R.string.error_required) else null,
        )
        TextInput(
            form.phone,
            viewModel::onPhone,
            stringResource(R.string.tenant_phone),
            keyboardType = KeyboardType.Phone,
        )
        UnitPicker(
            units = units,
            selectedId = form.unitId,
            currentTenantId = form.id,
            onSelect = viewModel::onUnit,
        )
        MoneyField(
            value = form.rent,
            onValueChange = viewModel::onRent,
            label = stringResource(R.string.tenant_rent),
            isError = form.rentError,
            supportingText = if (form.rentError) stringResource(R.string.error_amount) else null,
        )
        MoneyField(
            value = form.deposit,
            onValueChange = viewModel::onDeposit,
            label = stringResource(R.string.tenant_deposit),
            isError = form.depositError,
            supportingText = if (form.depositError) stringResource(R.string.error_amount) else null,
        )
        DateField(
            label = stringResource(R.string.tenant_start_date),
            millis = form.startDate,
            onDateChange = viewModel::onStartDate,
        )
        TextInput(
            value = form.dueDay,
            onValueChange = viewModel::onDueDay,
            label = stringResource(R.string.tenant_due_day),
            keyboardType = KeyboardType.Number,
            isError = form.dueDayError,
            supportingText = stringResource(R.string.tenant_due_day_hint),
        )
        TextInput(
            form.cnic,
            viewModel::onCnic,
            stringResource(R.string.tenant_cnic),
            keyboardType = KeyboardType.Number,
        )
        PhotoField(
            label = stringResource(R.string.tenant_cnic_photo),
            fileName = form.cnicPhoto,
            onPicked = viewModel::onPhotoPicked,
            onRemove = viewModel::onPhotoRemoved,
        )
        TextInput(form.notes, viewModel::onNotes, stringResource(R.string.field_notes), singleLine = false)
    }
}

@Composable
private fun UnitPicker(
    units: List<UnitWithStatus>,
    selectedId: Long?,
    currentTenantId: Long,
    onSelect: (UnitWithStatus?) -> Unit,
) {
    val selected = units.firstOrNull { it.unit.id == selectedId }
    val none = stringResource(R.string.tenant_no_unit)
    val vacant = stringResource(R.string.unit_vacant)
    val options = listOf(PickerOption<UnitWithStatus?>(null, none)) + units.map { u ->
        val takenByOther = u.isOccupied && u.tenantId != currentTenantId
        PickerOption<UnitWithStatus?>(
            value = u,
            title = "${u.propertyName} · ${u.unit.name}",
            subtitle = if (takenByOther) stringResource(R.string.tenant_unit_taken, u.tenantName.orEmpty()) else vacant,
            enabled = !takenByOther,
        )
    }
    PickerField(
        label = stringResource(R.string.tenant_unit),
        valueText = selected?.let { "${it.propertyName} · ${it.unit.name}" } ?: none,
        options = options,
        onSelect = onSelect,
        supportingText = if (units.isEmpty()) stringResource(R.string.tenant_unit_none_hint) else null,
    )
}
