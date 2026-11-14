package com.kiraya.diary.ui.moveout

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.kiraya.diary.R
import com.kiraya.diary.core.date.DateUtils
import com.kiraya.diary.core.money.Money
import com.kiraya.diary.domain.repository.TenantRepository
import com.kiraya.diary.domain.usecase.MoveOutUseCase
import com.kiraya.diary.domain.usecase.Settlement
import com.kiraya.diary.ui.components.ConfirmDialog
import com.kiraya.diary.ui.components.DateField
import com.kiraya.diary.ui.components.FormScaffold
import com.kiraya.diary.ui.components.LocalMoneyDisplay
import com.kiraya.diary.ui.components.MoneyField
import com.kiraya.diary.ui.components.TextInput
import com.kiraya.diary.ui.navigation.Routes
import com.kiraya.diary.ui.tenants.InfoRow
import com.kiraya.diary.ui.theme.KirayaColors
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MoveOutForm(
    val loaded: Boolean = false,
    val tenantName: String = "",
    val date: Long = DateUtils.startOfDay(DateUtils.now()),
    val pending: Long = 0,
    val deposit: Long = 0,
    val fromDeposit: String = "",
    val otherDeductions: String = "",
    val finalPayment: String = "",
    val note: String = "",
    val error: Boolean = false,
    val done: Boolean = false,
) {
    fun settlement(): Settlement? {
        val dues = if (fromDeposit.isBlank()) 0L else Money.parse(fromDeposit) ?: return null
        val other = if (otherDeductions.isBlank()) 0L else Money.parse(otherDeductions) ?: return null
        val paid = if (finalPayment.isBlank()) 0L else Money.parse(finalPayment) ?: return null
        if (dues < 0 || other < 0 || paid < 0) return null
        if (dues > pending || dues + other > deposit) return null
        return Settlement(pending, deposit, dues, other, paid)
    }
}

@HiltViewModel
class MoveOutViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val tenantRepository: TenantRepository,
    private val moveOut: MoveOutUseCase,
) : ViewModel() {
    private val tenantId: Long = checkNotNull(savedStateHandle[Routes.ARG_TENANT_ID])
    private val _form = MutableStateFlow(MoveOutForm())
    val form: StateFlow<MoveOutForm> = _form.asStateFlow()

    init {
        viewModelScope.launch {
            val t = tenantRepository.getTenant(tenantId)
            if (t == null) {
                _form.update { it.copy(done = true) }
                return@launch
            }
            val pending = moveOut.pendingAt(tenantId, _form.value.date)
            val s = Settlement.suggest(pending, t.securityDeposit)
            _form.value = MoveOutForm(
                loaded = true,
                tenantName = t.name,
                pending = pending,
                deposit = t.securityDeposit,
                fromDeposit = Money.toInput(s.duesFromDeposit),
            )
        }
    }

    fun onDate(v: Long) {
        _form.update { it.copy(date = v) }
        viewModelScope.launch {
            val pending = moveOut.pendingAt(tenantId, v)
            _form.update { f ->
                val s = Settlement.suggest(pending, f.deposit)
                f.copy(pending = pending, fromDeposit = Money.toInput(s.duesFromDeposit), error = false)
            }
        }
    }

    fun onFromDeposit(v: String) = _form.update { it.copy(fromDeposit = v, error = false) }
    fun onOther(v: String) = _form.update { it.copy(otherDeductions = v, error = false) }
    fun onFinalPayment(v: String) = _form.update { it.copy(finalPayment = v, error = false) }
    fun onNote(v: String) = _form.update { it.copy(note = v) }

    fun validate(): Boolean {
        val ok = _form.value.settlement() != null
        if (!ok) _form.update { it.copy(error = true) }
        return ok
    }

    fun confirm() {
        val f = _form.value
        val s = f.settlement() ?: return
        viewModelScope.launch {
            moveOut(tenantId, f.date, s, f.note)
            _form.update { it.copy(done = true) }
        }
    }
}

@Composable
fun MoveOutScreen(
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MoveOutViewModel = hiltViewModel(),
) {
    val form by viewModel.form.collectAsStateWithLifecycle()
    var confirm by remember { mutableStateOf(false) }
    val money = LocalMoneyDisplay.current
    LaunchedEffect(form.done) { if (form.done) onDone() }
    if (!form.loaded) return

    FormScaffold(
        title = stringResource(R.string.move_out_title),
        onBack = onDone,
        onSave = { if (viewModel.validate()) confirm = true },
        saveLabel = stringResource(R.string.move_out_confirm),
        modifier = modifier,
    ) {
        Text(form.tenantName, style = MaterialTheme.typography.titleMedium)
        DateField(stringResource(R.string.move_out_date), form.date, viewModel::onDate)
        Text(
            stringResource(R.string.move_out_date_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                InfoRow(stringResource(R.string.move_out_pending), money.formatPlain(form.pending))
                InfoRow(stringResource(R.string.tenant_deposit), money.formatPlain(form.deposit), last = true)
            }
        }
        MoneyField(
            value = form.fromDeposit,
            onValueChange = viewModel::onFromDeposit,
            label = stringResource(R.string.move_out_from_deposit),
            supportingText = stringResource(R.string.move_out_from_deposit_hint),
        )
        MoneyField(
            value = form.otherDeductions,
            onValueChange = viewModel::onOther,
            label = stringResource(R.string.move_out_other),
            supportingText = stringResource(R.string.move_out_other_hint),
        )
        MoneyField(
            value = form.finalPayment,
            onValueChange = viewModel::onFinalPayment,
            label = stringResource(R.string.move_out_final_payment),
        )
        TextInput(form.note, viewModel::onNote, stringResource(R.string.move_out_note), singleLine = false)

        val s = form.settlement()
        if (form.error || s == null) {
            Text(
                stringResource(R.string.move_out_error),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        if (s != null) {
            ElevatedCard(
                Modifier.fillMaxWidth(),
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
            ) {
                Column(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                    InfoRow(stringResource(R.string.move_out_total_deducted), money.formatPlain(s.totalDeducted))
                    InfoRow(stringResource(R.string.move_out_return), money.formatPlain(s.depositReturn), last = s.stillOwed == 0L)
                    if (s.stillOwed > 0) {
                        InfoRow(stringResource(R.string.move_out_still_owed), money.formatPlain(s.stillOwed), last = true)
                    }
                }
            }
            if (s.stillOwed > 0) {
                Text(
                    stringResource(R.string.move_out_still_owed_hint),
                    color = KirayaColors.status.unpaid,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }

    if (confirm) {
        ConfirmDialog(
            title = stringResource(R.string.move_out_title),
            message = stringResource(R.string.move_out_confirm_message, form.tenantName),
            confirmLabel = stringResource(R.string.move_out_confirm),
            onConfirm = viewModel::confirm,
            onDismiss = { confirm = false },
        )
    }
}
