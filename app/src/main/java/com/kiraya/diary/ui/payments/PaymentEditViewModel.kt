package com.kiraya.diary.ui.payments

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kiraya.diary.core.date.DateUtils
import com.kiraya.diary.core.money.Money
import com.kiraya.diary.domain.ledger.TenantLedger
import com.kiraya.diary.domain.model.Payment
import com.kiraya.diary.domain.model.PaymentMethod
import com.kiraya.diary.domain.model.TenantWithUnit
import com.kiraya.diary.domain.repository.RentRepository
import com.kiraya.diary.domain.repository.TenantRepository
import com.kiraya.diary.domain.usecase.ObserveLedgersUseCase
import com.kiraya.diary.ui.navigation.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PaymentForm(
    val loaded: Boolean = false,
    val id: Long = 0,
    val tenantId: Long = 0,
    val amount: String = "",
    val date: Long = DateUtils.startOfDay(DateUtils.now()),
    val method: PaymentMethod = PaymentMethod.CASH,
    val note: String = "",
    val createdAt: Long = 0,
    val tenantError: Boolean = false,
    val amountError: Boolean = false,
    /** Set after saving: the payment id (for a receipt). */
    val savedId: Long = 0,
    val done: Boolean = false,
)

data class TenantChoice(
    val tenant: TenantWithUnit,
    val pending: Long,
)

@HiltViewModel
class PaymentEditViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val rentRepository: RentRepository,
    tenantRepository: TenantRepository,
    private val observeLedgers: ObserveLedgersUseCase,
) : ViewModel() {
    private val presetTenantId: Long = savedStateHandle[Routes.ARG_TENANT_ID] ?: 0L
    private val paymentId: Long = savedStateHandle[Routes.ARG_PAYMENT_ID] ?: 0L
    val isEdit: Boolean get() = paymentId > 0

    private val _form = MutableStateFlow(PaymentForm(tenantId = presetTenantId))
    val form: StateFlow<PaymentForm> = _form.asStateFlow()

    /** Tenants to pick from, with what each one owes. */
    val tenants: StateFlow<List<TenantChoice>> = combine(
        tenantRepository.observeAllTenants(),
        observeLedgers(),
    ) { list, ledgers ->
        list.filter { it.tenant.isActive || it.tenant.id == _form.value.tenantId }
            .map { TenantChoice(it, (ledgers[it.tenant.id] ?: TenantLedger.EMPTY).pending) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        viewModelScope.launch {
            if (paymentId > 0) {
                val p = rentRepository.getPayment(paymentId)
                if (p == null) {
                    _form.update { it.copy(done = true) }
                    return@launch
                }
                _form.value = PaymentForm(
                    loaded = true,
                    id = p.id,
                    tenantId = p.tenantId,
                    amount = Money.toInput(p.amount),
                    date = p.date,
                    method = p.method,
                    note = p.note,
                    createdAt = p.createdAt,
                )
            } else {
                // New payment: suggest the full pending amount.
                val pending = if (presetTenantId > 0) {
                    observeLedgers().first()[presetTenantId]?.pending ?: 0L
                } else {
                    0L
                }
                _form.update { it.copy(loaded = true, amount = Money.toInput(pending)) }
            }
        }
    }

    fun onTenant(choice: TenantChoice) = _form.update {
        it.copy(
            tenantId = choice.tenant.tenant.id,
            tenantError = false,
            amount = if (it.amount.isBlank() || !isEdit) Money.toInput(choice.pending) else it.amount,
        )
    }

    fun onAmount(v: String) = _form.update { it.copy(amount = v, amountError = false) }
    fun onDate(v: Long) = _form.update { it.copy(date = v) }
    fun onMethod(v: PaymentMethod) = _form.update { it.copy(method = v) }
    fun onNote(v: String) = _form.update { it.copy(note = v) }

    fun payFull() {
        val pending = tenants.value.firstOrNull { it.tenant.tenant.id == _form.value.tenantId }?.pending ?: return
        _form.update { it.copy(amount = Money.toInput(pending), amountError = false) }
    }

    fun save() {
        val f = _form.value
        val amount = Money.parse(f.amount)
        if (f.tenantId <= 0 || amount == null || amount <= 0) {
            _form.update { it.copy(tenantError = f.tenantId <= 0, amountError = amount == null || amount <= 0) }
            return
        }
        viewModelScope.launch {
            val id = rentRepository.savePayment(
                Payment(
                    id = f.id,
                    tenantId = f.tenantId,
                    amount = amount,
                    date = f.date,
                    method = f.method,
                    note = f.note,
                    createdAt = f.createdAt,
                ),
            )
            _form.update { it.copy(savedId = id, done = true) }
        }
    }

    fun delete() {
        viewModelScope.launch {
            rentRepository.deletePayment(paymentId)
            _form.update { it.copy(done = true) }
        }
    }
}
