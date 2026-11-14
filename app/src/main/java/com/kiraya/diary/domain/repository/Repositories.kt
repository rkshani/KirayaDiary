package com.kiraya.diary.domain.repository

import com.kiraya.diary.domain.model.Bill
import com.kiraya.diary.domain.model.BillCharge
import com.kiraya.diary.domain.model.BillShare
import com.kiraya.diary.domain.model.BillWithShares
import com.kiraya.diary.domain.model.Expense
import com.kiraya.diary.domain.model.ExpenseWithProperty
import com.kiraya.diary.domain.model.Lease
import com.kiraya.diary.domain.model.LeaseAlert
import com.kiraya.diary.domain.model.Payment
import com.kiraya.diary.domain.model.PaymentWithTenant
import com.kiraya.diary.domain.model.Property
import com.kiraya.diary.domain.model.PropertyWithCounts
import com.kiraya.diary.domain.model.RentEntry
import com.kiraya.diary.domain.model.RentalUnit
import com.kiraya.diary.domain.model.Tenant
import com.kiraya.diary.domain.model.TenantWithUnit
import com.kiraya.diary.domain.model.UnitWithStatus
import kotlinx.coroutines.flow.Flow

interface PropertyRepository {
    fun observePropertiesWithCounts(): Flow<List<PropertyWithCounts>>
    fun observeProperties(): Flow<List<Property>>
    fun observeProperty(id: Long): Flow<Property?>
    suspend fun getProperty(id: Long): Property?

    /** Inserts when id is 0, otherwise updates. Returns the id. */
    suspend fun saveProperty(property: Property): Long
    suspend fun deleteProperty(id: Long)

    fun observeUnits(propertyId: Long): Flow<List<UnitWithStatus>>
    fun observeAllUnits(): Flow<List<UnitWithStatus>>
    suspend fun getUnit(id: Long): RentalUnit?
    suspend fun saveUnit(unit: RentalUnit): Long
    suspend fun deleteUnit(id: Long)
}

interface TenantRepository {
    fun observeActiveTenants(): Flow<List<TenantWithUnit>>
    fun observeAllTenants(): Flow<List<TenantWithUnit>>
    fun observeTenant(id: Long): Flow<TenantWithUnit?>
    suspend fun getTenant(id: Long): Tenant?
    suspend fun getActiveTenants(): List<Tenant>
    suspend fun saveTenant(tenant: Tenant): Long
    suspend fun deleteTenant(id: Long)

    fun observeLease(tenantId: Long): Flow<Lease?>
    suspend fun getLease(tenantId: Long): Lease?
    suspend fun saveLease(lease: Lease): Long
    suspend fun deleteLease(tenantId: Long)
    fun observeLeasesEndingBefore(before: Long): Flow<List<LeaseAlert>>
    suspend fun getLeasesEndingBefore(before: Long): List<LeaseAlert>
}

interface RentRepository {
    fun observeRentEntries(tenantId: Long): Flow<List<RentEntry>>
    fun observeAllRentEntries(): Flow<List<RentEntry>>
    suspend fun getRentEntry(id: Long): RentEntry?

    /** Returns false if the tenant already has an entry for that month. */
    suspend fun addRentEntry(entry: RentEntry): Boolean
    suspend fun updateRentEntry(entry: RentEntry)
    suspend fun deleteRentEntry(id: Long)
    suspend fun lastRentMonth(tenantId: Long): Int?

    /** Removes a tenant's rent entries for months after [month] (used at move-out). */
    suspend fun deleteRentEntriesAfter(tenantId: Long, month: Int)

    fun observePayments(tenantId: Long): Flow<List<Payment>>
    fun observeAllPayments(): Flow<List<Payment>>
    fun observePaymentsBetween(from: Long, to: Long): Flow<List<PaymentWithTenant>>
    suspend fun getPayment(id: Long): Payment?
    suspend fun savePayment(payment: Payment): Long
    suspend fun deletePayment(id: Long)
}

interface BillRepository {
    fun observeBills(): Flow<List<BillWithShares>>
    fun observeBillCharges(): Flow<List<BillCharge>>
    suspend fun getBill(id: Long): BillWithShares?
    suspend fun saveBill(bill: Bill, shares: List<BillShare>): Long
    suspend fun deleteBill(id: Long)
}

interface ExpenseRepository {
    fun observeExpenses(): Flow<List<ExpenseWithProperty>>
    suspend fun getExpense(id: Long): Expense?
    suspend fun saveExpense(expense: Expense): Long
    suspend fun deleteExpense(id: Long)
}
