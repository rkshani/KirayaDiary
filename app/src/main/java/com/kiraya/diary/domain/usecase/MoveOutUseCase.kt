package com.kiraya.diary.domain.usecase

import com.kiraya.diary.core.date.DateUtils
import com.kiraya.diary.domain.ledger.LedgerCalculator
import com.kiraya.diary.domain.model.Payment
import com.kiraya.diary.domain.model.PaymentMethod
import com.kiraya.diary.domain.repository.BillRepository
import com.kiraya.diary.domain.repository.RentRepository
import com.kiraya.diary.domain.repository.TenantRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/** Money for the final settlement. All amounts are paisa. */
data class Settlement(
    /** Dues left after removing rent for months after the move-out. */
    val pendingDues: Long,
    val deposit: Long,
    /** Part of the deposit kept to clear dues. */
    val duesFromDeposit: Long,
    /** Extra deductions (damage, cleaning...). */
    val otherDeductions: Long,
    /** Cash received from the tenant at move-out. */
    val finalPayment: Long,
) {
    val totalDeducted: Long get() = duesFromDeposit + otherDeductions
    val depositReturn: Long get() = (deposit - totalDeducted).coerceAtLeast(0)

    /** What the tenant still owes after deposit and final payment (0 if settled). */
    val stillOwed: Long get() = (pendingDues - duesFromDeposit - finalPayment).coerceAtLeast(0)

    companion object {
        /** Default: use the deposit for dues as far as it goes. */
        fun suggest(pendingDues: Long, deposit: Long) =
            Settlement(pendingDues, deposit, minOf(pendingDues, deposit), 0, 0)
    }
}

class MoveOutUseCase @Inject constructor(
    private val tenantRepository: TenantRepository,
    private val rentRepository: RentRepository,
    private val billRepository: BillRepository,
) {
    /** Dues the tenant would have after moving out on [moveOutDate]. */
    suspend fun pendingAt(tenantId: Long, moveOutDate: Long): Long {
        val month = DateUtils.monthKey(moveOutDate)
        val entries = rentRepository.observeRentEntries(tenantId).first().filter { it.month <= month }
        val bills = billRepository.observeBillCharges().first().filter { it.share.tenantId == tenantId }
        val payments = rentRepository.observePayments(tenantId).first()
        return LedgerCalculator.build(tenantId, entries, bills, payments).pending
    }

    suspend operator fun invoke(tenantId: Long, moveOutDate: Long, settlement: Settlement, note: String) {
        val tenant = tenantRepository.getTenant(tenantId) ?: return
        // No rent after the move-out month.
        rentRepository.deleteRentEntriesAfter(tenantId, DateUtils.monthKey(moveOutDate))
        if (settlement.duesFromDeposit > 0) {
            rentRepository.savePayment(
                Payment(
                    tenantId = tenantId,
                    amount = settlement.duesFromDeposit,
                    date = moveOutDate,
                    method = PaymentMethod.DEPOSIT,
                    note = note,
                ),
            )
        }
        if (settlement.finalPayment > 0) {
            rentRepository.savePayment(
                Payment(tenantId = tenantId, amount = settlement.finalPayment, date = moveOutDate, method = PaymentMethod.CASH),
            )
        }
        // Inactive tenants no longer occupy their unit, so it shows as vacant.
        tenantRepository.saveTenant(
            tenant.copy(
                isActive = false,
                moveOutDate = moveOutDate,
                depositDeducted = settlement.totalDeducted,
                depositReturned = settlement.depositReturn,
                settlementNote = note,
            ),
        )
    }
}
