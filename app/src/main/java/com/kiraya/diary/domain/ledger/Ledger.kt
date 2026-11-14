package com.kiraya.diary.domain.ledger

import com.kiraya.diary.core.date.DateUtils
import com.kiraya.diary.domain.model.BillCharge
import com.kiraya.diary.domain.model.BillType
import com.kiraya.diary.domain.model.Payment
import com.kiraya.diary.domain.model.RentEntry
import com.kiraya.diary.domain.model.RentStatus

enum class ChargeKind { RENT, BILL }

/** Something the tenant owes: a month's rent or a share of a bill. */
data class LedgerCharge(
    val kind: ChargeKind,
    /** RentEntry id or BillShare id. */
    val refId: Long,
    /** Bill id for BILL charges. */
    val billId: Long? = null,
    val month: Int,
    val amount: Long,
    val dueDate: Long,
    val billType: BillType? = null,
    val billTitle: String = "",
    /** How much of this charge is covered by payments (oldest charges are paid first). */
    val paid: Long,
) {
    val remaining: Long get() = (amount - paid).coerceAtLeast(0)

    val status: RentStatus
        get() = when {
            paid >= amount -> RentStatus.PAID
            paid > 0 -> RentStatus.PARTIAL
            else -> RentStatus.UNPAID
        }

    fun isOverdue(now: Long): Boolean = remaining > 0 && dueDate < DateUtils.startOfDay(now)
}

data class TenantLedger(
    val tenantId: Long,
    val charges: List<LedgerCharge>,
    val payments: List<Payment>,
) {
    val totalCharged: Long get() = charges.sumOf { it.amount }
    val totalPaid: Long get() = payments.sumOf { it.amount }

    /** Positive: tenant owes this much. Negative: tenant has paid in advance. */
    val balance: Long get() = totalCharged - totalPaid
    val pending: Long get() = balance.coerceAtLeast(0)
    val credit: Long get() = (-balance).coerceAtLeast(0)

    fun overdueCharges(now: Long): List<LedgerCharge> = charges.filter { it.isOverdue(now) }

    fun overdueAmount(now: Long): Long = overdueCharges(now).sumOf { it.remaining }

    fun chargesForMonth(month: Int): List<LedgerCharge> = charges.filter { it.month == month }

    companion object {
        val EMPTY = TenantLedger(0, emptyList(), emptyList())
    }
}

object LedgerCalculator {
    /**
     * Builds a tenant's ledger. Payments are pooled and applied to the oldest charges first,
     * so partial payments always clear old dues before new ones.
     */
    fun build(
        tenantId: Long,
        rentEntries: List<RentEntry>,
        billCharges: List<BillCharge>,
        payments: List<Payment>,
    ): TenantLedger {
        data class Raw(
            val kind: ChargeKind,
            val refId: Long,
            val billId: Long?,
            val month: Int,
            val amount: Long,
            val dueDate: Long,
            val billType: BillType?,
            val billTitle: String,
        )

        val raw = rentEntries.map {
            Raw(ChargeKind.RENT, it.id, null, it.month, it.amount, it.dueDate, null, "")
        } + billCharges.map {
            Raw(
                ChargeKind.BILL, it.share.id, it.bill.id, it.bill.month, it.share.amount,
                it.bill.dueDate, it.bill.type, it.bill.title,
            )
        }

        var pool = payments.sumOf { it.amount }
        val charges = raw
            .sortedWith(compareBy<Raw>({ it.dueDate }, { it.month }, { it.kind.ordinal }, { it.refId }))
            .map { r ->
                val paid = minOf(r.amount, pool.coerceAtLeast(0))
                pool -= paid
                LedgerCharge(r.kind, r.refId, r.billId, r.month, r.amount, r.dueDate, r.billType, r.billTitle, paid)
            }
        return TenantLedger(tenantId, charges, payments.sortedByDescending { it.date })
    }
}
