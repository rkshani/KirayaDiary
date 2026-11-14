package com.kiraya.diary.data.repository

import com.kiraya.diary.core.date.DateUtils
import com.kiraya.diary.data.local.dao.BillDao
import com.kiraya.diary.data.local.dao.ExpenseDao
import com.kiraya.diary.data.local.dao.LeaseDao
import com.kiraya.diary.data.local.dao.PaymentDao
import com.kiraya.diary.data.local.dao.PropertyDao
import com.kiraya.diary.data.local.dao.RentEntryDao
import com.kiraya.diary.data.local.dao.TenantDao
import com.kiraya.diary.data.local.dao.UnitDao
import com.kiraya.diary.data.local.model.LeaseTenantRow
import com.kiraya.diary.data.mapper.toDomain
import com.kiraya.diary.data.mapper.toEntity
import com.kiraya.diary.domain.model.Bill
import com.kiraya.diary.domain.model.BillCharge
import com.kiraya.diary.domain.model.BillShare
import com.kiraya.diary.domain.model.BillShareWithTenant
import com.kiraya.diary.domain.model.BillWithShares
import com.kiraya.diary.domain.model.Expense
import com.kiraya.diary.domain.model.ExpenseWithProperty
import com.kiraya.diary.domain.model.Lease
import com.kiraya.diary.domain.model.LeaseAlert
import com.kiraya.diary.domain.model.Payment
import com.kiraya.diary.domain.model.PaymentWithTenant
import com.kiraya.diary.domain.model.Property
import com.kiraya.diary.domain.model.RentEntry
import com.kiraya.diary.domain.model.RentalUnit
import com.kiraya.diary.domain.model.Tenant
import com.kiraya.diary.domain.repository.BillRepository
import com.kiraya.diary.domain.repository.ExpenseRepository
import com.kiraya.diary.domain.repository.PropertyRepository
import com.kiraya.diary.domain.repository.RentRepository
import com.kiraya.diary.domain.repository.TenantRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PropertyRepositoryImpl @Inject constructor(
    private val propertyDao: PropertyDao,
    private val unitDao: UnitDao,
) : PropertyRepository {
    override fun observePropertiesWithCounts() =
        propertyDao.observeAllWithCounts().map { rows -> rows.map { it.toDomain() } }

    override fun observeProperties() = propertyDao.observeAll().map { rows -> rows.map { it.toDomain() } }

    override fun observeProperty(id: Long) = propertyDao.observe(id).map { it?.toDomain() }

    override suspend fun getProperty(id: Long) = propertyDao.get(id)?.toDomain()

    override suspend fun saveProperty(property: Property): Long =
        if (property.id == 0L) {
            propertyDao.insert(property.copy(createdAt = DateUtils.now()).toEntity())
        } else {
            propertyDao.update(property.toEntity())
            property.id
        }

    override suspend fun deleteProperty(id: Long) {
        propertyDao.get(id)?.let { propertyDao.delete(it) }
    }

    override fun observeUnits(propertyId: Long) =
        unitDao.observeByProperty(propertyId).map { rows -> rows.map { it.toDomain() } }

    override fun observeAllUnits() = unitDao.observeAllWithStatus().map { rows -> rows.map { it.toDomain() } }

    override suspend fun getUnit(id: Long) = unitDao.get(id)?.toDomain()

    override suspend fun saveUnit(unit: RentalUnit): Long =
        if (unit.id == 0L) {
            unitDao.insert(unit.copy(createdAt = DateUtils.now()).toEntity())
        } else {
            unitDao.update(unit.toEntity())
            unit.id
        }

    override suspend fun deleteUnit(id: Long) {
        unitDao.get(id)?.let { unitDao.delete(it) }
    }
}

@Singleton
class TenantRepositoryImpl @Inject constructor(
    private val tenantDao: TenantDao,
    private val leaseDao: LeaseDao,
) : TenantRepository {
    override fun observeActiveTenants() =
        tenantDao.observeActiveWithUnit().map { rows -> rows.map { it.toDomain() } }

    override fun observeAllTenants() = tenantDao.observeAllWithUnit().map { rows -> rows.map { it.toDomain() } }

    override fun observeTenant(id: Long) = tenantDao.observeWithUnit(id).map { it?.toDomain() }

    override suspend fun getTenant(id: Long) = tenantDao.get(id)?.toDomain()

    override suspend fun getActiveTenants() = tenantDao.getActive().map { it.toDomain() }

    override suspend fun saveTenant(tenant: Tenant): Long =
        if (tenant.id == 0L) {
            tenantDao.insert(tenant.copy(createdAt = DateUtils.now()).toEntity())
        } else {
            tenantDao.update(tenant.toEntity())
            tenant.id
        }

    override suspend fun deleteTenant(id: Long) {
        tenantDao.get(id)?.let { tenantDao.delete(it) }
    }

    override fun observeLease(tenantId: Long) = leaseDao.observeByTenant(tenantId).map { it?.toDomain() }

    override suspend fun getLease(tenantId: Long) = leaseDao.getByTenant(tenantId)?.toDomain()

    override suspend fun saveLease(lease: Lease): Long {
        // One lease per tenant: reuse the existing row id.
        val existing = leaseDao.getByTenant(lease.tenantId)
        val toSave = lease.copy(
            id = existing?.id ?: 0,
            createdAt = existing?.createdAt ?: DateUtils.now(),
        )
        val result = leaseDao.upsert(toSave.toEntity())
        return if (result == -1L) toSave.id else result
    }

    override suspend fun deleteLease(tenantId: Long) {
        leaseDao.getByTenant(tenantId)?.let { leaseDao.delete(it) }
    }

    override fun observeLeasesEndingBefore(before: Long) =
        leaseDao.observeEndingBefore(before).map { rows -> rows.map { it.toAlert() } }

    override suspend fun getLeasesEndingBefore(before: Long) = leaseDao.getEndingBefore(before).map { it.toAlert() }

    private fun LeaseTenantRow.toAlert() = LeaseAlert(lease.toDomain(), tenantName, tenantPhone)
}

@Singleton
class RentRepositoryImpl @Inject constructor(
    private val rentEntryDao: RentEntryDao,
    private val paymentDao: PaymentDao,
) : RentRepository {
    override fun observeRentEntries(tenantId: Long) =
        rentEntryDao.observeByTenant(tenantId).map { rows -> rows.map { it.toDomain() } }

    override fun observeAllRentEntries() = rentEntryDao.observeAll().map { rows -> rows.map { it.toDomain() } }

    override suspend fun getRentEntry(id: Long) = rentEntryDao.get(id)?.toDomain()

    override suspend fun addRentEntry(entry: RentEntry): Boolean =
        rentEntryDao.insert(entry.copy(id = 0, createdAt = DateUtils.now()).toEntity()) != -1L

    override suspend fun updateRentEntry(entry: RentEntry) = rentEntryDao.update(entry.toEntity())

    override suspend fun deleteRentEntry(id: Long) {
        rentEntryDao.get(id)?.let { rentEntryDao.delete(it) }
    }

    override suspend fun lastRentMonth(tenantId: Long) = rentEntryDao.lastMonth(tenantId)

    override suspend fun deleteRentEntriesAfter(tenantId: Long, month: Int) = rentEntryDao.deleteAfter(tenantId, month)

    override fun observePayments(tenantId: Long) =
        paymentDao.observeByTenant(tenantId).map { rows -> rows.map { it.toDomain() } }

    override fun observeAllPayments() = paymentDao.observeAll().map { rows -> rows.map { it.toDomain() } }

    override fun observePaymentsBetween(from: Long, to: Long): Flow<List<PaymentWithTenant>> =
        paymentDao.observeBetween(from, to).map { rows ->
            rows.map { PaymentWithTenant(it.payment.toDomain(), it.tenantName) }
        }

    override suspend fun getPayment(id: Long) = paymentDao.get(id)?.toDomain()

    override suspend fun savePayment(payment: Payment): Long =
        if (payment.id == 0L) {
            paymentDao.insert(payment.copy(createdAt = DateUtils.now()).toEntity())
        } else {
            paymentDao.update(payment.toEntity())
            payment.id
        }

    override suspend fun deletePayment(id: Long) {
        paymentDao.get(id)?.let { paymentDao.delete(it) }
    }
}

@Singleton
class BillRepositoryImpl @Inject constructor(
    private val billDao: BillDao,
    private val propertyDao: PropertyDao,
    private val tenantDao: TenantDao,
) : BillRepository {
    override fun observeBills(): Flow<List<BillWithShares>> = combine(
        billDao.observeBills(),
        billDao.observeShares(),
        propertyDao.observeAll(),
        tenantDao.observeAll(),
    ) { bills, shares, properties, tenants ->
        val propertyNames = properties.associate { it.id to it.name }
        val tenantNames = tenants.associate { it.id to it.name }
        val sharesByBill = shares.groupBy { it.billId }
        bills.map { bill ->
            BillWithShares(
                bill = bill.toDomain(),
                propertyName = bill.propertyId?.let { propertyNames[it] },
                shares = sharesByBill[bill.id].orEmpty().map {
                    BillShareWithTenant(it.toDomain(), tenantNames[it.tenantId].orEmpty())
                },
            )
        }
    }

    override fun observeBillCharges(): Flow<List<BillCharge>> =
        combine(billDao.observeBills(), billDao.observeShares()) { bills, shares ->
            val byId = bills.associateBy { it.id }
            shares.mapNotNull { share ->
                byId[share.billId]?.let { BillCharge(share.toDomain(), it.toDomain()) }
            }
        }

    override suspend fun getBill(id: Long): BillWithShares? {
        val bill = billDao.getBill(id) ?: return null
        val propertyName = bill.propertyId?.let { propertyDao.get(it)?.name }
        val shares = billDao.getShares(id).map {
            BillShareWithTenant(it.toDomain(), tenantDao.get(it.tenantId)?.name.orEmpty())
        }
        return BillWithShares(bill.toDomain(), propertyName, shares)
    }

    override suspend fun saveBill(bill: Bill, shares: List<BillShare>): Long {
        val shareEntities = shares.map { it.toEntity() }
        return if (bill.id == 0L) {
            billDao.insertWithShares(bill.copy(createdAt = DateUtils.now()).toEntity(), shareEntities)
        } else {
            billDao.updateWithShares(bill.toEntity(), shareEntities)
            bill.id
        }
    }

    override suspend fun deleteBill(id: Long) = billDao.deleteBill(id)
}

@Singleton
class ExpenseRepositoryImpl @Inject constructor(
    private val expenseDao: ExpenseDao,
    private val propertyDao: PropertyDao,
) : ExpenseRepository {
    override fun observeExpenses(): Flow<List<ExpenseWithProperty>> =
        combine(expenseDao.observeAll(), propertyDao.observeAll()) { expenses, properties ->
            val names = properties.associate { it.id to it.name }
            expenses.map { ExpenseWithProperty(it.toDomain(), it.propertyId?.let { id -> names[id] }) }
        }

    override suspend fun getExpense(id: Long): Expense? = expenseDao.get(id)?.toDomain()

    override suspend fun saveExpense(expense: Expense): Long =
        if (expense.id == 0L) {
            expenseDao.insert(expense.copy(createdAt = DateUtils.now()).toEntity())
        } else {
            expenseDao.update(expense.toEntity())
            expense.id
        }

    override suspend fun deleteExpense(id: Long) {
        expenseDao.get(id)?.let { expenseDao.delete(it) }
    }
}
