package com.kiraya.diary.domain.ledger

import com.kiraya.diary.domain.model.Payment
import com.kiraya.diary.domain.model.PaymentMethod
import com.kiraya.diary.domain.model.RentEntry
import com.kiraya.diary.domain.model.RentStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class LedgerCalculatorTest {
    private fun entry(id: Long, month: Int, amount: Long, due: Long) =
        RentEntry(id = id, tenantId = 1, month = month, amount = amount, dueDate = due)

    private fun pay(amount: Long) =
        Payment(tenantId = 1, amount = amount, date = 0, method = PaymentMethod.CASH)

    @Test
    fun partialPaymentClearsOldestFirst() {
        val ledger = LedgerCalculator.build(
            tenantId = 1,
            rentEntries = listOf(entry(2, 202602, 10_000_00, 2_000), entry(1, 202601, 10_000_00, 1_000)),
            billCharges = emptyList(),
            payments = listOf(pay(15_000_00)),
        )
        assertEquals(RentStatus.PAID, ledger.charges[0].status)
        assertEquals(202601, ledger.charges[0].month)
        assertEquals(RentStatus.PARTIAL, ledger.charges[1].status)
        assertEquals(5_000_00L, ledger.charges[1].remaining)
        assertEquals(5_000_00L, ledger.balance)
    }

    @Test
    fun overpaymentBecomesCredit() {
        val ledger = LedgerCalculator.build(
            tenantId = 1,
            rentEntries = listOf(entry(1, 202601, 1_000_00, 1_000)),
            billCharges = emptyList(),
            payments = listOf(pay(1_500_00)),
        )
        assertEquals(0L, ledger.pending)
        assertEquals(500_00L, ledger.credit)
    }
}
