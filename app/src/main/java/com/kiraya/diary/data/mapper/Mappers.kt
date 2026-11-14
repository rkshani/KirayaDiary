package com.kiraya.diary.data.mapper

import com.kiraya.diary.data.local.entity.BillEntity
import com.kiraya.diary.data.local.entity.BillShareEntity
import com.kiraya.diary.data.local.entity.ExpenseEntity
import com.kiraya.diary.data.local.entity.LeaseEntity
import com.kiraya.diary.data.local.entity.PaymentEntity
import com.kiraya.diary.data.local.entity.PropertyEntity
import com.kiraya.diary.data.local.entity.RentEntryEntity
import com.kiraya.diary.data.local.entity.TenantEntity
import com.kiraya.diary.data.local.entity.UnitEntity
import com.kiraya.diary.data.local.model.PropertyCountsRow
import com.kiraya.diary.data.local.model.TenantUnitRow
import com.kiraya.diary.data.local.model.UnitStatusRow
import com.kiraya.diary.domain.model.Bill
import com.kiraya.diary.domain.model.BillShare
import com.kiraya.diary.domain.model.Expense
import com.kiraya.diary.domain.model.Lease
import com.kiraya.diary.domain.model.Payment
import com.kiraya.diary.domain.model.Property
import com.kiraya.diary.domain.model.PropertyWithCounts
import com.kiraya.diary.domain.model.RentEntry
import com.kiraya.diary.domain.model.RentalUnit
import com.kiraya.diary.domain.model.Tenant
import com.kiraya.diary.domain.model.TenantWithUnit
import com.kiraya.diary.domain.model.UnitWithStatus

fun PropertyEntity.toDomain() = Property(id, name, address, notes, createdAt)
fun Property.toEntity() = PropertyEntity(id, name.trim(), address.trim(), notes.trim(), createdAt)
fun PropertyCountsRow.toDomain() = PropertyWithCounts(property.toDomain(), unitCount, occupiedCount)

fun UnitEntity.toDomain() = RentalUnit(id, propertyId, name, type, defaultRent, notes, createdAt)
fun RentalUnit.toEntity() = UnitEntity(id, propertyId, name.trim(), type, defaultRent, notes.trim(), createdAt)
fun UnitStatusRow.toDomain() = UnitWithStatus(unit.toDomain(), propertyName, tenantId, tenantName)

fun TenantEntity.toDomain() = Tenant(
    id = id, name = name, phone = phone, cnic = cnic, cnicPhoto = cnicPhoto, unitId = unitId,
    rentAmount = rentAmount, securityDeposit = securityDeposit, startDate = startDate, dueDay = dueDay,
    isActive = isActive, notes = notes, moveOutDate = moveOutDate, depositReturned = depositReturned,
    depositDeducted = depositDeducted, settlementNote = settlementNote, createdAt = createdAt,
)

fun Tenant.toEntity() = TenantEntity(
    id = id, name = name.trim(), phone = phone.trim(), cnic = cnic.trim(), cnicPhoto = cnicPhoto,
    unitId = unitId, rentAmount = rentAmount, securityDeposit = securityDeposit, startDate = startDate,
    dueDay = dueDay, isActive = isActive, notes = notes.trim(), moveOutDate = moveOutDate,
    depositReturned = depositReturned, depositDeducted = depositDeducted,
    settlementNote = settlementNote.trim(), createdAt = createdAt,
)

fun TenantUnitRow.toDomain() = TenantWithUnit(tenant.toDomain(), unitName, propertyId, propertyName)

fun LeaseEntity.toDomain() =
    Lease(id, tenantId, startDate, endDate, baseRent, yearlyIncreasePercent, agreementPhoto, notes, createdAt)

fun Lease.toEntity() =
    LeaseEntity(id, tenantId, startDate, endDate, baseRent, yearlyIncreasePercent, agreementPhoto, notes.trim(), createdAt)

fun RentEntryEntity.toDomain() = RentEntry(id, tenantId, unitId, month, amount, dueDate, note, createdAt)
fun RentEntry.toEntity() = RentEntryEntity(id, tenantId, unitId, month, amount, dueDate, note.trim(), createdAt)

fun PaymentEntity.toDomain() = Payment(id, tenantId, amount, date, method, note, createdAt)
fun Payment.toEntity() = PaymentEntity(id, tenantId, amount, date, method, note.trim(), createdAt)

fun BillEntity.toDomain() =
    Bill(id, propertyId, type, title, totalAmount, month, dueDate, splitMode, note, createdAt)

fun Bill.toEntity() =
    BillEntity(id, propertyId, type, title.trim(), totalAmount, month, dueDate, splitMode, note.trim(), createdAt)

fun BillShareEntity.toDomain() = BillShare(id, billId, tenantId, unitId, amount)
fun BillShare.toEntity() = BillShareEntity(id, billId, tenantId, unitId, amount)

fun ExpenseEntity.toDomain() = Expense(id, propertyId, unitId, category, amount, date, note, createdAt)
fun Expense.toEntity() = ExpenseEntity(id, propertyId, unitId, category, amount, date, note.trim(), createdAt)
