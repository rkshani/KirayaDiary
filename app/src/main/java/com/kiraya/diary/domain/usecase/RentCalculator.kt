package com.kiraya.diary.domain.usecase

import com.kiraya.diary.core.date.DateUtils
import com.kiraya.diary.core.money.Money
import com.kiraya.diary.domain.model.Lease
import com.kiraya.diary.domain.model.Tenant
import kotlin.math.pow

object RentCalculator {
    /**
     * Rent for a month. With a lease that has a yearly increase, the base rent goes up
     * by the percent once for every full year since the lease start (compounded).
     * Without one, the tenant's current rent is used.
     */
    fun rentForMonth(tenant: Tenant, lease: Lease?, month: Int): Long {
        if (lease == null || lease.yearlyIncreasePercent <= 0.0) return tenant.rentAmount
        val years = fullYearsSince(lease.startDate, month)
        if (years <= 0) return lease.baseRent
        val factor = (1 + lease.yearlyIncreasePercent / 100.0).pow(years)
        return Money.percentOf(lease.baseRent, factor * 100.0)
    }

    /** Full lease years completed by the start of the given month. */
    fun fullYearsSince(startDate: Long, month: Int): Int {
        val startMonth = DateUtils.monthKey(startDate)
        val months = (DateUtils.yearOf(month) - DateUtils.yearOf(startMonth)) * 12 +
            (DateUtils.monthOf(month) - DateUtils.monthOf(startMonth))
        return if (months < 0) 0 else months / 12
    }

    /** Due date for a tenant's rent in a month. */
    fun dueDate(tenant: Tenant, month: Int): Long = DateUtils.dayInMonth(month, tenant.dueDay)
}
