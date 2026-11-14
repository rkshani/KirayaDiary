package com.kiraya.diary.domain.usecase

import com.kiraya.diary.core.date.DateUtils
import com.kiraya.diary.domain.model.RentEntry
import com.kiraya.diary.domain.model.Tenant
import com.kiraya.diary.domain.repository.RentRepository
import com.kiraya.diary.domain.repository.TenantRepository
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Creates the monthly rent entry for every active tenant, from their start month (or the
 * month after their last entry) up to the current month. Safe to run many times: a tenant
 * never gets two entries for the same month.
 */
@Singleton
class GenerateRentEntriesUseCase @Inject constructor(
    private val tenantRepository: TenantRepository,
    private val rentRepository: RentRepository,
) {
    private val mutex = Mutex()

    /** Returns how many entries were created. */
    suspend operator fun invoke(tenantId: Long? = null, now: Long = DateUtils.now()): Int = mutex.withLock {
        val currentMonth = DateUtils.monthKey(now)
        val tenants = if (tenantId != null) {
            listOfNotNull(tenantRepository.getTenant(tenantId)?.takeIf { it.isActive })
        } else {
            tenantRepository.getActiveTenants()
        }
        var created = 0
        for (tenant in tenants) created += generateFor(tenant, currentMonth)
        created
    }

    private suspend fun generateFor(tenant: Tenant, currentMonth: Int): Int {
        val lease = tenantRepository.getLease(tenant.id)
        val startMonth = DateUtils.monthKey(tenant.startDate)
        val last = rentRepository.lastRentMonth(tenant.id)
        var month = if (last != null) DateUtils.addMonths(last, 1) else startMonth
        // Never go back more than MAX_CATCH_UP months in one run.
        val earliest = DateUtils.addMonths(currentMonth, -MAX_CATCH_UP)
        if (month < earliest) month = earliest
        if (month < startMonth) month = startMonth

        var created = 0
        while (month <= currentMonth) {
            val amount = RentCalculator.rentForMonth(tenant, lease, month)
            val added = rentRepository.addRentEntry(
                RentEntry(
                    tenantId = tenant.id,
                    unitId = tenant.unitId,
                    month = month,
                    amount = amount,
                    dueDate = RentCalculator.dueDate(tenant, month),
                ),
            )
            if (added) created++
            month = DateUtils.addMonths(month, 1)
        }

        // Keep the tenant's shown rent equal to this month's rent after a yearly increase.
        if (lease != null && lease.yearlyIncreasePercent > 0) {
            val rentNow = RentCalculator.rentForMonth(tenant, lease, currentMonth)
            if (rentNow != tenant.rentAmount) tenantRepository.saveTenant(tenant.copy(rentAmount = rentNow))
        }
        return created
    }

    companion object {
        const val MAX_CATCH_UP = 36
    }
}
