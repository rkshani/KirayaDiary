package com.kiraya.diary.domain.model

// Money is Long paisa. Dates are epoch millis. Months are yyyyMM ints.

data class Property(
    val id: Long = 0,
    val name: String,
    val address: String = "",
    val notes: String = "",
    val createdAt: Long = 0,
)

/** A rentable part of a property: flat, shop, room... Named RentalUnit to avoid clashing with kotlin.Unit. */
data class RentalUnit(
    val id: Long = 0,
    val propertyId: Long,
    val name: String,
    val type: UnitType,
    val defaultRent: Long = 0,
    val notes: String = "",
    val createdAt: Long = 0,
)

/** A unit with its property name and current tenant (if occupied). */
data class UnitWithStatus(
    val unit: RentalUnit,
    val propertyName: String,
    val tenantId: Long?,
    val tenantName: String?,
) {
    val isOccupied: Boolean get() = tenantId != null
}

data class PropertyWithCounts(
    val property: Property,
    val unitCount: Int,
    val occupiedCount: Int,
) {
    val vacantCount: Int get() = unitCount - occupiedCount
}

data class Tenant(
    val id: Long = 0,
    val name: String,
    val phone: String = "",
    val cnic: String = "",
    val cnicPhoto: String? = null,
    val unitId: Long? = null,
    val rentAmount: Long,
    val securityDeposit: Long = 0,
    val startDate: Long,
    val dueDay: Int = 1,
    val isActive: Boolean = true,
    val notes: String = "",
    val moveOutDate: Long? = null,
    val depositReturned: Long = 0,
    val depositDeducted: Long = 0,
    val settlementNote: String = "",
    val createdAt: Long = 0,
)

/** A tenant with the names of its unit and property for lists. */
data class TenantWithUnit(
    val tenant: Tenant,
    val unitName: String?,
    val propertyId: Long?,
    val propertyName: String?,
)

data class Lease(
    val id: Long = 0,
    val tenantId: Long,
    val startDate: Long,
    val endDate: Long? = null,
    val baseRent: Long,
    val yearlyIncreasePercent: Double = 0.0,
    val agreementPhoto: String? = null,
    val notes: String = "",
    val createdAt: Long = 0,
)

data class RentEntry(
    val id: Long = 0,
    val tenantId: Long,
    val unitId: Long? = null,
    val month: Int,
    val amount: Long,
    val dueDate: Long,
    val note: String = "",
    val createdAt: Long = 0,
)

data class Payment(
    val id: Long = 0,
    val tenantId: Long,
    val amount: Long,
    val date: Long,
    val method: PaymentMethod,
    val note: String = "",
    val createdAt: Long = 0,
)

data class Bill(
    val id: Long = 0,
    val propertyId: Long? = null,
    val type: BillType,
    val title: String = "",
    val totalAmount: Long,
    val month: Int,
    val dueDate: Long,
    val splitMode: SplitMode,
    val note: String = "",
    val createdAt: Long = 0,
)

data class BillShare(
    val id: Long = 0,
    val billId: Long,
    val tenantId: Long,
    val unitId: Long? = null,
    val amount: Long,
)

/** A bill share with its bill, for a tenant's dues. */
data class BillCharge(
    val share: BillShare,
    val bill: Bill,
)

data class BillWithShares(
    val bill: Bill,
    val propertyName: String?,
    val shares: List<BillShareWithTenant>,
)

data class BillShareWithTenant(
    val share: BillShare,
    val tenantName: String,
)

data class Expense(
    val id: Long = 0,
    val propertyId: Long? = null,
    val unitId: Long? = null,
    val category: ExpenseCategory,
    val amount: Long,
    val date: Long,
    val note: String = "",
    val createdAt: Long = 0,
)

data class ExpenseWithProperty(
    val expense: Expense,
    val propertyName: String?,
)

data class LeaseAlert(
    val lease: Lease,
    val tenantName: String,
    val tenantPhone: String,
)

data class PaymentWithTenant(
    val payment: Payment,
    val tenantName: String,
)
