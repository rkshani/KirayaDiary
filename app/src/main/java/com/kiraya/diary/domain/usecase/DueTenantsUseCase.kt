package com.kiraya.diary.domain.usecase

import com.kiraya.diary.core.date.DateUtils
import com.kiraya.diary.domain.ledger.TenantLedger
import com.kiraya.diary.domain.model.TenantWithUnit
import com.kiraya.diary.domain.repository.TenantRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/** A tenant with dues that are overdue or due within the reminder window. */
data class DueTenant(
    val tenant: TenantWithUnit,
    /** Everything the tenant owes now. */
    val amount: Long,
    /** Months with unpaid charges, oldest first (yyyyMM). */
    val months: List<Int>,
    val oldestDueDate: Long,
    val overdue: Boolean,
)

class DueTenantsUseCase @Inject constructor(
    private val tenantRepository: TenantRepository,
    private val observeLedgers: ObserveLedgersUseCase,
) {
    fun observe(daysBefore: Int, now: () -> Long = DateUtils::now): Flow<List<DueTenant>> =
        combine(tenantRepository.observeActiveTenants(), observeLedgers()) { tenants, ledgers ->
            compute(tenants, ledgers, daysBefore, now())
        }

    suspend fun get(daysBefore: Int, now: Long = DateUtils.now()): List<DueTenant> =
        compute(tenantRepository.observeActiveTenants().first(), observeLedgers().first(), daysBefore, now)

    private fun compute(
        tenants: List<TenantWithUnit>,
        ledgers: Map<Long, TenantLedger>,
        daysBefore: Int,
        now: Long,
    ): List<DueTenant> {
        val today = DateUtils.startOfDay(now)
        val windowEnd = today + (daysBefore + 1) * DateUtils.DAY_MILLIS
        return tenants.mapNotNull { t ->
            val ledger = ledgers[t.tenant.id] ?: return@mapNotNull null
            val open = ledger.charges.filter { it.remaining > 0 }
            val dueSoon = open.filter { it.dueDate < windowEnd }
            if (dueSoon.isEmpty()) return@mapNotNull null
            DueTenant(
                tenant = t,
                amount = ledger.pending,
                months = open.map { it.month }.distinct().sorted(),
                oldestDueDate = dueSoon.minOf { it.dueDate },
                overdue = dueSoon.any { it.dueDate < today },
            )
        }.sortedBy { it.oldestDueDate }
    }
}
