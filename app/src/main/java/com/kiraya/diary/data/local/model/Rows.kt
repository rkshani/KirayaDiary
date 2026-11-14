package com.kiraya.diary.data.local.model

import androidx.room.Embedded
import com.kiraya.diary.data.local.entity.LeaseEntity
import com.kiraya.diary.data.local.entity.PaymentEntity
import com.kiraya.diary.data.local.entity.PropertyEntity
import com.kiraya.diary.data.local.entity.TenantEntity
import com.kiraya.diary.data.local.entity.UnitEntity

/** Query results that join tables. */

data class PropertyCountsRow(
    @Embedded val property: PropertyEntity,
    val unitCount: Int,
    val occupiedCount: Int,
)

data class UnitStatusRow(
    @Embedded val unit: UnitEntity,
    val propertyName: String,
    val tenantId: Long?,
    val tenantName: String?,
)

data class TenantUnitRow(
    @Embedded val tenant: TenantEntity,
    val unitName: String?,
    val propertyId: Long?,
    val propertyName: String?,
)

data class LeaseTenantRow(
    @Embedded val lease: LeaseEntity,
    val tenantName: String,
    val tenantPhone: String,
)

data class PaymentTenantRow(
    @Embedded val payment: PaymentEntity,
    val tenantName: String,
)
