package com.kiraya.diary.domain.usecase

import com.kiraya.diary.core.date.DateUtils
import com.kiraya.diary.domain.model.Lease
import com.kiraya.diary.domain.repository.TenantRepository
import javax.inject.Inject

/** Saves the lease and keeps the tenant's current rent in step with the yearly increase. */
class SaveLeaseUseCase @Inject constructor(
    private val tenantRepository: TenantRepository,
) {
    suspend operator fun invoke(lease: Lease) {
        tenantRepository.saveLease(lease)
        val tenant = tenantRepository.getTenant(lease.tenantId) ?: return
        val rentNow = RentCalculator.rentForMonth(tenant, lease, DateUtils.currentMonthKey())
        val rent = if (lease.yearlyIncreasePercent > 0) rentNow else lease.baseRent
        if (rent != tenant.rentAmount) tenantRepository.saveTenant(tenant.copy(rentAmount = rent))
    }
}

/** Details shown on the lease card. */
data class LeaseInfo(
    val lease: Lease,
    val currentRent: Long,
    /** Date of the next yearly increase, or null if there is no increase. */
    val nextIncreaseDate: Long?,
    val nextRent: Long?,
    /** Days until the lease ends (negative when it has ended), or null if no end date. */
    val daysToEnd: Int?,
) {
    companion object {
        fun from(lease: Lease, now: Long): LeaseInfo {
            val month = DateUtils.monthKey(now)
            val years = RentCalculator.fullYearsSince(lease.startDate, month)
            val dummy = com.kiraya.diary.domain.model.Tenant(name = "", rentAmount = lease.baseRent, startDate = lease.startDate)
            val current = RentCalculator.rentForMonth(dummy, lease, month)
            val hasIncrease = lease.yearlyIncreasePercent > 0
            val nextDate = if (hasIncrease) DateUtils.addYears(lease.startDate, years + 1) else null
            val nextRent = nextDate?.let { RentCalculator.rentForMonth(dummy, lease, DateUtils.monthKey(it)) }
            return LeaseInfo(
                lease = lease,
                currentRent = current,
                nextIncreaseDate = nextDate,
                nextRent = nextRent,
                daysToEnd = lease.endDate?.let { DateUtils.daysBetween(now, it) },
            )
        }
    }
}
