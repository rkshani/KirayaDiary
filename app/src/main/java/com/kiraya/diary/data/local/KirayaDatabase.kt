package com.kiraya.diary.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.kiraya.diary.data.local.dao.BillDao
import com.kiraya.diary.data.local.dao.ExpenseDao
import com.kiraya.diary.data.local.dao.LeaseDao
import com.kiraya.diary.data.local.dao.PaymentDao
import com.kiraya.diary.data.local.dao.PropertyDao
import com.kiraya.diary.data.local.dao.RentEntryDao
import com.kiraya.diary.data.local.dao.TenantDao
import com.kiraya.diary.data.local.dao.UnitDao
import com.kiraya.diary.data.local.entity.BillEntity
import com.kiraya.diary.data.local.entity.BillShareEntity
import com.kiraya.diary.data.local.entity.ExpenseEntity
import com.kiraya.diary.data.local.entity.LeaseEntity
import com.kiraya.diary.data.local.entity.PaymentEntity
import com.kiraya.diary.data.local.entity.PropertyEntity
import com.kiraya.diary.data.local.entity.RentEntryEntity
import com.kiraya.diary.data.local.entity.TenantEntity
import com.kiraya.diary.data.local.entity.UnitEntity

@Database(
    entities = [
        PropertyEntity::class,
        UnitEntity::class,
        TenantEntity::class,
        LeaseEntity::class,
        RentEntryEntity::class,
        PaymentEntity::class,
        BillEntity::class,
        BillShareEntity::class,
        ExpenseEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class KirayaDatabase : RoomDatabase() {
    abstract fun propertyDao(): PropertyDao
    abstract fun unitDao(): UnitDao
    abstract fun tenantDao(): TenantDao
    abstract fun leaseDao(): LeaseDao
    abstract fun rentEntryDao(): RentEntryDao
    abstract fun paymentDao(): PaymentDao
    abstract fun billDao(): BillDao
    abstract fun expenseDao(): ExpenseDao

    companion object {
        const val NAME = "kiraya.db"
    }
}
