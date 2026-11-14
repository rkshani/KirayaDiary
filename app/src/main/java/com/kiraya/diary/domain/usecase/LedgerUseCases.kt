package com.kiraya.diary.domain.usecase

import com.kiraya.diary.domain.ledger.LedgerCalculator
import com.kiraya.diary.domain.ledger.TenantLedger
import com.kiraya.diary.domain.repository.BillRepository
import com.kiraya.diary.domain.repository.RentRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/** Ledgers for every tenant, keyed by tenant id. */
class ObserveLedgersUseCase @Inject constructor(
    private val rentRepository: RentRepository,
    private val billRepository: BillRepository,
) {
    operator fun invoke(): Flow<Map<Long, TenantLedger>> = combine(
        rentRepository.observeAllRentEntries(),
        billRepository.observeBillCharges(),
        rentRepository.observeAllPayments(),
    ) { entries, bills, payments ->
        val entriesBy = entries.groupBy { it.tenantId }
        val billsBy = bills.groupBy { it.share.tenantId }
        val paymentsBy = payments.groupBy { it.tenantId }
        val ids = entriesBy.keys + billsBy.keys + paymentsBy.keys
        ids.associateWith { id ->
            LedgerCalculator.build(
                id,
                entriesBy[id].orEmpty(),
                billsBy[id].orEmpty(),
                paymentsBy[id].orEmpty(),
            )
        }
    }
}

/** One tenant's ledger. */
class ObserveTenantLedgerUseCase @Inject constructor(
    private val rentRepository: RentRepository,
    private val billRepository: BillRepository,
) {
    operator fun invoke(tenantId: Long): Flow<TenantLedger> = combine(
        rentRepository.observeRentEntries(tenantId),
        billRepository.observeBillCharges().map { list -> list.filter { it.share.tenantId == tenantId } },
        rentRepository.observePayments(tenantId),
    ) { entries, bills, payments ->
        LedgerCalculator.build(tenantId, entries, bills, payments)
    }
}
