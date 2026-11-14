package com.kiraya.diary.domain.usecase

import com.kiraya.diary.core.date.DateUtils
import com.kiraya.diary.domain.model.ExpenseWithProperty
import com.kiraya.diary.domain.model.Payment
import com.kiraya.diary.domain.model.Property
import com.kiraya.diary.domain.model.TenantWithUnit
import com.kiraya.diary.domain.repository.ExpenseRepository
import com.kiraya.diary.domain.repository.PropertyRepository
import com.kiraya.diary.domain.repository.RentRepository
import com.kiraya.diary.domain.repository.TenantRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

/** A time range: one month or one year. */
data class Period(val isYear: Boolean, val year: Int, val month: Int = 1) {
    val from: Long get() = if (isYear) DateUtils.yearStart(year) else DateUtils.monthStart(year * 100 + month)
    val to: Long get() = if (isYear) DateUtils.yearEnd(year) else DateUtils.monthEnd(year * 100 + month)
    val monthKey: Int get() = year * 100 + month

    fun previous(): Period = if (isYear) copy(year = year - 1) else fromMonthKey(DateUtils.addMonths(monthKey, -1))
    fun next(): Period = if (isYear) copy(year = year + 1) else fromMonthKey(DateUtils.addMonths(monthKey, 1))
    fun asYear(): Period = copy(isYear = true)
    fun asMonth(): Period = copy(isYear = false)

    /** True if this period is in the future (nothing to show yet). */
    fun isAfterNow(): Boolean = from > DateUtils.now()

    companion object {
        fun currentMonth(): Period = fromMonthKey(DateUtils.currentMonthKey())
        fun fromMonthKey(key: Int) = Period(false, DateUtils.yearOf(key), DateUtils.monthOf(key))
    }
}

data class PropertyProfit(
    val propertyId: Long?,
    val propertyName: String?,
    val income: Long,
    val expenses: Long,
) {
    val profit: Long get() = income - expenses
}

data class ProfitSummary(
    val income: Long,
    val expenses: Long,
    val byProperty: List<PropertyProfit>,
    val expenseList: List<ExpenseWithProperty>,
) {
    val profit: Long get() = income - expenses
}

object ProfitCalculator {
    fun compute(
        period: Period,
        payments: List<Payment>,
        expenses: List<ExpenseWithProperty>,
        tenants: List<TenantWithUnit>,
        properties: List<Property>,
    ): ProfitSummary {
        val from = period.from
        val to = period.to
        val tenantProperty = tenants.associate { it.tenant.id to it.propertyId }
        val inPayments = payments.filter { it.date in from until to }
        val inExpenses = expenses.filter { it.expense.date in from until to }
        val incomeBy = inPayments.groupBy { tenantProperty[it.tenantId] }.mapValues { e -> e.value.sumOf { it.amount } }
        val expenseBy = inExpenses.groupBy { it.expense.propertyId }.mapValues { e -> e.value.sumOf { it.expense.amount } }
        val names = properties.associate { it.id to it.name }
        val ids = (incomeBy.keys + expenseBy.keys)
        val rows = ids.map { id ->
            PropertyProfit(id, id?.let { names[it] }, incomeBy[id] ?: 0, expenseBy[id] ?: 0)
        }.sortedWith(compareBy<PropertyProfit>({ it.propertyId == null }, { it.propertyName.orEmpty().lowercase() }))
        return ProfitSummary(
            income = inPayments.sumOf { it.amount },
            expenses = inExpenses.sumOf { it.expense.amount },
            byProperty = rows,
            expenseList = inExpenses,
        )
    }
}

class ObserveProfitUseCase @Inject constructor(
    private val rentRepository: RentRepository,
    private val expenseRepository: ExpenseRepository,
    private val tenantRepository: TenantRepository,
    private val propertyRepository: PropertyRepository,
) {
    operator fun invoke(period: Flow<Period>): Flow<Pair<Period, ProfitSummary>> = combine(
        period,
        rentRepository.observeAllPayments(),
        expenseRepository.observeExpenses(),
        tenantRepository.observeAllTenants(),
        propertyRepository.observeProperties(),
    ) { p, payments, expenses, tenants, properties ->
        p to ProfitCalculator.compute(p, payments, expenses, tenants, properties)
    }
}
