package com.kiraya.diary.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kiraya.diary.core.date.DateUtils
import com.kiraya.diary.domain.ledger.TenantLedger
import com.kiraya.diary.domain.model.TenantWithUnit
import com.kiraya.diary.domain.repository.PropertyRepository
import com.kiraya.diary.domain.repository.RentRepository
import com.kiraya.diary.domain.repository.TenantRepository
import com.kiraya.diary.domain.usecase.ObserveLedgersUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class OverdueTenant(
    val tenant: TenantWithUnit,
    val overdueAmount: Long,
    val oldestDueDate: Long,
)

data class HomeUiState(
    val loading: Boolean = true,
    val month: Int = DateUtils.currentMonthKey(),
    val dueThisMonth: Long = 0,
    val collectedThisMonth: Long = 0,
    val pendingThisMonth: Long = 0,
    val totalPending: Long = 0,
    val activeTenants: Int = 0,
    val unitCount: Int = 0,
    val vacantUnits: Int = 0,
    val overdue: List<OverdueTenant> = emptyList(),
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    tenantRepository: TenantRepository,
    rentRepository: RentRepository,
    propertyRepository: PropertyRepository,
    observeLedgers: ObserveLedgersUseCase,
) : ViewModel() {
    private val month = DateUtils.currentMonthKey()

    val state: StateFlow<HomeUiState> = combine(
        tenantRepository.observeActiveTenants(),
        observeLedgers(),
        rentRepository.observePaymentsBetween(DateUtils.monthStart(month), DateUtils.monthEnd(month)),
        propertyRepository.observeAllUnits(),
    ) { tenants, ledgers, payments, units ->
        val now = DateUtils.now()
        val activeLedgers = tenants.map { ledgers[it.tenant.id] ?: TenantLedger.EMPTY }
        val monthCharges = activeLedgers.flatMap { it.chargesForMonth(month) }
        HomeUiState(
            loading = false,
            month = month,
            dueThisMonth = monthCharges.sumOf { it.amount },
            collectedThisMonth = payments.sumOf { it.payment.amount },
            pendingThisMonth = monthCharges.sumOf { it.remaining },
            totalPending = activeLedgers.sumOf { it.pending },
            activeTenants = tenants.size,
            unitCount = units.size,
            vacantUnits = units.count { !it.isOccupied },
            overdue = tenants.mapNotNull { t ->
                val ledger = ledgers[t.tenant.id] ?: return@mapNotNull null
                val overdue = ledger.overdueCharges(now)
                if (overdue.isEmpty()) {
                    null
                } else {
                    OverdueTenant(t, overdue.sumOf { it.remaining }, overdue.minOf { it.dueDate })
                }
            }.sortedBy { it.oldestDueDate },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())
}
