package com.kiraya.diary.ui.rent

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kiraya.diary.core.date.DateUtils
import com.kiraya.diary.core.money.Money
import com.kiraya.diary.domain.ledger.ChargeKind
import com.kiraya.diary.domain.model.RentEntry
import com.kiraya.diary.domain.model.RentStatus
import com.kiraya.diary.domain.model.TenantWithUnit
import com.kiraya.diary.domain.repository.RentRepository
import com.kiraya.diary.domain.repository.TenantRepository
import com.kiraya.diary.domain.usecase.GenerateRentEntriesUseCase
import com.kiraya.diary.domain.usecase.ObserveLedgersUseCase
import com.kiraya.diary.ui.navigation.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MonthRow(
    val tenant: TenantWithUnit,
    val rent: Long,
    val bills: Long,
    val paid: Long,
    val status: RentStatus,
) {
    val total: Long get() = rent + bills
    val remaining: Long get() = (total - paid).coerceAtLeast(0)
}

data class MonthlyRentState(
    val loaded: Boolean = false,
    val month: Int = DateUtils.currentMonthKey(),
    val rows: List<MonthRow> = emptyList(),
    val generating: Boolean = false,
    val lastCreated: Int? = null,
) {
    val total: Long get() = rows.sumOf { it.total }
    val paid: Long get() = rows.sumOf { it.paid }
    val remaining: Long get() = rows.sumOf { it.remaining }
    val canGoNext: Boolean get() = month < DateUtils.currentMonthKey()
}

@HiltViewModel
class MonthlyRentViewModel @Inject constructor(
    tenantRepository: TenantRepository,
    observeLedgers: ObserveLedgersUseCase,
    private val generateRentEntries: GenerateRentEntriesUseCase,
) : ViewModel() {
    private val month = MutableStateFlow(DateUtils.currentMonthKey())
    private val generating = MutableStateFlow<Pair<Boolean, Int?>>(false to null)

    val state: StateFlow<MonthlyRentState> = combine(
        tenantRepository.observeAllTenants(),
        observeLedgers(),
        month,
        generating,
    ) { tenants, ledgers, m, gen ->
        val rows = tenants.mapNotNull { t ->
            val charges = ledgers[t.tenant.id]?.chargesForMonth(m).orEmpty()
            if (charges.isEmpty()) return@mapNotNull null
            val total = charges.sumOf { it.amount }
            val paid = charges.sumOf { it.paid }
            MonthRow(
                tenant = t,
                rent = charges.filter { it.kind == ChargeKind.RENT }.sumOf { it.amount },
                bills = charges.filter { it.kind == ChargeKind.BILL }.sumOf { it.amount },
                paid = paid,
                status = when {
                    paid >= total -> RentStatus.PAID
                    paid > 0 -> RentStatus.PARTIAL
                    else -> RentStatus.UNPAID
                },
            )
        }.sortedWith(compareBy<MonthRow>({ it.status == RentStatus.PAID }, { it.tenant.tenant.name.lowercase() }))
        MonthlyRentState(true, m, rows, gen.first, gen.second)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MonthlyRentState())

    fun previousMonth() = month.update { DateUtils.addMonths(it, -1) }

    fun nextMonth() = month.update { m ->
        val next = DateUtils.addMonths(m, 1)
        if (next <= DateUtils.currentMonthKey()) next else m
    }

    fun generateNow() {
        viewModelScope.launch {
            generating.value = true to null
            val created = runCatching { generateRentEntries() }.getOrDefault(0)
            generating.value = false to created
        }
    }

    fun clearMessage() = generating.update { it.first to null }
}

data class RentEntryForm(
    val loaded: Boolean = false,
    val tenantName: String = "",
    val month: Int = 0,
    val amount: String = "",
    val dueDate: Long = 0,
    val note: String = "",
    val amountError: Boolean = false,
    val done: Boolean = false,
)

@HiltViewModel
class RentEntryEditViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val rentRepository: RentRepository,
    private val tenantRepository: TenantRepository,
) : ViewModel() {
    private val entryId: Long = checkNotNull(savedStateHandle[Routes.ARG_ENTRY_ID])
    private var entry: RentEntry? = null

    private val _form = MutableStateFlow(RentEntryForm())
    val form: StateFlow<RentEntryForm> = _form.asStateFlow()

    init {
        viewModelScope.launch {
            val e = rentRepository.getRentEntry(entryId) ?: run {
                _form.update { it.copy(done = true) }
                return@launch
            }
            entry = e
            val name = tenantRepository.getTenant(e.tenantId)?.name.orEmpty()
            _form.value = RentEntryForm(true, name, e.month, Money.toInput(e.amount), e.dueDate, e.note)
        }
    }

    fun onAmount(v: String) = _form.update { it.copy(amount = v, amountError = false) }
    fun onDueDate(v: Long) = _form.update { it.copy(dueDate = v) }
    fun onNote(v: String) = _form.update { it.copy(note = v) }

    fun save() {
        val e = entry ?: return
        val f = _form.value
        val amount = Money.parse(f.amount)
        if (amount == null || amount < 0) {
            _form.update { it.copy(amountError = true) }
            return
        }
        viewModelScope.launch {
            rentRepository.updateRentEntry(e.copy(amount = amount, dueDate = f.dueDate, note = f.note))
            _form.update { it.copy(done = true) }
        }
    }

    /** Sets this month's rent to zero. The entry stays so it is not created again. */
    fun waive() {
        val e = entry ?: return
        viewModelScope.launch {
            rentRepository.updateRentEntry(e.copy(amount = 0, note = _form.value.note))
            _form.update { it.copy(done = true) }
        }
    }
}
