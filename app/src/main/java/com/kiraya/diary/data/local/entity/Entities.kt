package com.kiraya.diary.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.kiraya.diary.domain.model.BillType
import com.kiraya.diary.domain.model.ExpenseCategory
import com.kiraya.diary.domain.model.PaymentMethod
import com.kiraya.diary.domain.model.SplitMode
import com.kiraya.diary.domain.model.UnitType

// Money columns are Long paisa. Date columns are epoch millis. Month columns are yyyyMM ints.

@Entity(tableName = "properties")
data class PropertyEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val address: String = "",
    val notes: String = "",
    val createdAt: Long,
)

@Entity(
    tableName = "units",
    foreignKeys = [
        ForeignKey(
            entity = PropertyEntity::class,
            parentColumns = ["id"],
            childColumns = ["propertyId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("propertyId")],
)
data class UnitEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val propertyId: Long,
    val name: String,
    val type: UnitType,
    val defaultRent: Long = 0,
    val notes: String = "",
    val createdAt: Long,
)

@Entity(
    tableName = "tenants",
    foreignKeys = [
        ForeignKey(
            entity = UnitEntity::class,
            parentColumns = ["id"],
            childColumns = ["unitId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [Index("unitId"), Index("isActive")],
)
data class TenantEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val phone: String = "",
    val cnic: String = "",
    /** File name inside the app's photos folder, or null. */
    val cnicPhoto: String? = null,
    val unitId: Long? = null,
    /** Current monthly rent. */
    val rentAmount: Long,
    /** Advance / security deposit held by the landlord. */
    val securityDeposit: Long = 0,
    val startDate: Long,
    /** Day of month the rent is due (1..28). */
    val dueDay: Int = 1,
    val isActive: Boolean = true,
    val notes: String = "",
    // Move-out settlement (filled when the tenant leaves).
    val moveOutDate: Long? = null,
    val depositReturned: Long = 0,
    val depositDeducted: Long = 0,
    val settlementNote: String = "",
    val createdAt: Long,
)

@Entity(
    tableName = "leases",
    foreignKeys = [
        ForeignKey(
            entity = TenantEntity::class,
            parentColumns = ["id"],
            childColumns = ["tenantId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["tenantId"], unique = true)],
)
data class LeaseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tenantId: Long,
    val startDate: Long,
    val endDate: Long? = null,
    /** Rent at the lease start. Yearly increases are applied on top of this. */
    val baseRent: Long,
    val yearlyIncreasePercent: Double = 0.0,
    /** File name inside the app's photos folder, or null. */
    val agreementPhoto: String? = null,
    val notes: String = "",
    val createdAt: Long,
)

@Entity(
    tableName = "rent_entries",
    foreignKeys = [
        ForeignKey(
            entity = TenantEntity::class,
            parentColumns = ["id"],
            childColumns = ["tenantId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["tenantId", "month"], unique = true), Index("month")],
)
data class RentEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tenantId: Long,
    val unitId: Long? = null,
    /** yyyyMM */
    val month: Int,
    val amount: Long,
    val dueDate: Long,
    val note: String = "",
    val createdAt: Long,
)

@Entity(
    tableName = "payments",
    foreignKeys = [
        ForeignKey(
            entity = TenantEntity::class,
            parentColumns = ["id"],
            childColumns = ["tenantId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("tenantId"), Index("date")],
)
data class PaymentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tenantId: Long,
    val amount: Long,
    val date: Long,
    val method: PaymentMethod,
    val note: String = "",
    val createdAt: Long,
)

@Entity(
    tableName = "bills",
    foreignKeys = [
        ForeignKey(
            entity = PropertyEntity::class,
            parentColumns = ["id"],
            childColumns = ["propertyId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("propertyId"), Index("month")],
)
data class BillEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val propertyId: Long? = null,
    val type: BillType,
    val title: String = "",
    val totalAmount: Long,
    /** yyyyMM the bill belongs to. */
    val month: Int,
    val dueDate: Long,
    val splitMode: SplitMode,
    val note: String = "",
    val createdAt: Long,
)

@Entity(
    tableName = "bill_shares",
    foreignKeys = [
        ForeignKey(
            entity = BillEntity::class,
            parentColumns = ["id"],
            childColumns = ["billId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = TenantEntity::class,
            parentColumns = ["id"],
            childColumns = ["tenantId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("billId"), Index("tenantId")],
)
data class BillShareEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val billId: Long,
    val tenantId: Long,
    val unitId: Long? = null,
    val amount: Long,
)

@Entity(
    tableName = "expenses",
    foreignKeys = [
        ForeignKey(
            entity = PropertyEntity::class,
            parentColumns = ["id"],
            childColumns = ["propertyId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("propertyId"), Index("date")],
)
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val propertyId: Long? = null,
    val unitId: Long? = null,
    val category: ExpenseCategory,
    val amount: Long,
    val date: Long,
    val note: String = "",
    val createdAt: Long,
)
