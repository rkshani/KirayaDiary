package com.kiraya.diary.domain.usecase

import com.kiraya.diary.core.date.DateUtils
import com.kiraya.diary.domain.ledger.TenantLedger
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

data class ReportPropertyRow(
    val propertyName: String?,
    /** Rent and bills charged for months in the period. */
    val charged: Long,
    /** Payments received in the period. */
    val collected: Long,
    val expenses: Long,
) {
    val profit: Long get() = collected - expenses
}

data class ReportMonthRow(
    val month: Int,
    val charged: Long,
    val collected: Long,
    val expenses: Long,
)

data class PendingRow(
    val tenantName: String,
    val place: String,
    val phone: String,
    val amount: Long,
    val oldestMonth: Int?,
)

data class Report(
    val period: Period,
    val charged: Long,
    val collected: Long,
    val expenses: Long,
    val properties: List<ReportPropertyRow>,
    /** One row per month, only for a year report. */
    val months: List<ReportMonthRow>,
    /** Everyone who owes money now (not limited to the period). */
    val pending: List<PendingRow>,
) {
    val profit: Long get() = collected - expenses
    val totalPending: Long get() = pending.sumOf { it.amount }
}

object ReportBuilder {
    fun build(
        period: Period,
        tenants: List<TenantWithUnit>,
        ledgers: Map<Long, TenantLedger>,
        payments: List<Payment>,
        expenses: List<ExpenseWithProperty>,
        properties: List<Property>,
    ): Report {
        val names = properties.associate { it.id to it.name }
        val tenantProperty = tenants.associate { it.tenant.id to it.propertyId }
        val firstMonth = DateUtils.monthKey(period.from)
        val lastMonth = DateUtils.monthKey(period.to - 1)
        fun inPeriod(month: Int) = month in firstMonth..lastMonth

        data class Line(val propertyId: Long?, val month: Int, val charged: Long, val collected: Long, val expense: Long)

        val lines = buildList {
            ledgers.forEach { (tenantId, ledger) ->
                ledger.charges.filter { inPeriod(it.month) }.forEach {
                    add(Line(tenantProperty[tenantId], it.month, it.amount, 0, 0))
                }
            }
            payments.filter { it.date >= period.from && it.date < period.to }.forEach {
                add(Line(tenantProperty[it.tenantId], DateUtils.monthKey(it.date), 0, it.amount, 0))
            }
            expenses.filter { it.expense.date >= period.from && it.expense.date < period.to }.forEach {
                add(Line(it.expense.propertyId, DateUtils.monthKey(it.expense.date), 0, 0, it.expense.amount))
            }
        }

        val byProperty = lines.groupBy { it.propertyId }.map { (id, l) ->
            ReportPropertyRow(id?.let { names[it] }, l.sumOf { it.charged }, l.sumOf { it.collected }, l.sumOf { it.expense })
        }.sortedWith(compareBy<ReportPropertyRow>({ it.propertyName == null }, { it.propertyName.orEmpty().lowercase() }))

        val months = if (period.isYear) {
            (1..12).map { m ->
                val key = period.year * 100 + m
                val l = lines.filter { it.month == key }
                ReportMonthRow(key, l.sumOf { it.charged }, l.sumOf { it.collected }, l.sumOf { it.expense })
            }
        } else {
            emptyList()
        }

        val pending = tenants.filter { it.tenant.isActive }.mapNotNull { t ->
            val ledger = ledgers[t.tenant.id] ?: return@mapNotNull null
            if (ledger.pending <= 0) return@mapNotNull null
            PendingRow(
                tenantName = t.tenant.name,
                place = listOfNotNull(t.propertyName, t.unitName).joinToString(" · "),
                phone = t.tenant.phone,
                amount = ledger.pending,
                oldestMonth = ledger.charges.firstOrNull { it.remaining > 0 }?.month,
            )
        }.sortedByDescending { it.amount }

        return Report(
            period = period,
            charged = lines.sumOf { it.charged },
            collected = lines.sumOf { it.collected },
            expenses = lines.sumOf { it.expense },
            properties = byProperty,
            months = months,
            pending = pending,
        )
    }
}

class ObserveReportUseCase @Inject constructor(
    private val tenantRepository: TenantRepository,
    private val observeLedgers: ObserveLedgersUseCase,
    private val rentRepository: RentRepository,
    private val expenseRepository: ExpenseRepository,
    private val propertyRepository: PropertyRepository,
) {
    operator fun invoke(period: Flow<Period>): Flow<Report> {
        val data = combine(
            tenantRepository.observeAllTenants(),
            observeLedgers(),
            rentRepository.observeAllPayments(),
            expenseRepository.observeExpenses(),
            propertyRepository.observeProperties(),
        ) { t, l, p, e, pr -> ReportInputs(t, l, p, e, pr) }
        return combine(period, data) { p, d -> ReportBuilder.build(p, d.tenants, d.ledgers, d.payments, d.expenses, d.properties) }
    }

    private data class ReportInputs(
        val tenants: List<TenantWithUnit>,
        val ledgers: Map<Long, TenantLedger>,
        val payments: List<Payment>,
        val expenses: List<ExpenseWithProperty>,
        val properties: List<Property>,
    )
}
