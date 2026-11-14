package com.kiraya.diary.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.kiraya.diary.data.local.entity.BillEntity
import com.kiraya.diary.data.local.entity.BillShareEntity
import com.kiraya.diary.data.local.entity.ExpenseEntity
import com.kiraya.diary.data.local.entity.PaymentEntity
import com.kiraya.diary.data.local.entity.RentEntryEntity
import com.kiraya.diary.data.local.model.PaymentTenantRow
import kotlinx.coroutines.flow.Flow

@Dao
interface RentEntryDao {
    /** Returns -1 if an entry for this tenant and month already exists. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(entry: RentEntryEntity): Long

    @Update
    suspend fun update(entry: RentEntryEntity)

    @Delete
    suspend fun delete(entry: RentEntryEntity)

    @Query("SELECT * FROM rent_entries WHERE id = :id")
    suspend fun get(id: Long): RentEntryEntity?

    @Query("SELECT * FROM rent_entries WHERE tenantId = :tenantId ORDER BY month")
    fun observeByTenant(tenantId: Long): Flow<List<RentEntryEntity>>

    @Query("SELECT * FROM rent_entries ORDER BY month")
    fun observeAll(): Flow<List<RentEntryEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM rent_entries WHERE tenantId = :tenantId AND month = :month)")
    suspend fun exists(tenantId: Long, month: Int): Boolean

    @Query("SELECT MAX(month) FROM rent_entries WHERE tenantId = :tenantId")
    suspend fun lastMonth(tenantId: Long): Int?

    @Query("DELETE FROM rent_entries WHERE tenantId = :tenantId AND month > :month")
    suspend fun deleteAfter(tenantId: Long, month: Int)
}

@Dao
interface PaymentDao {
    @Insert
    suspend fun insert(payment: PaymentEntity): Long

    @Update
    suspend fun update(payment: PaymentEntity)

    @Delete
    suspend fun delete(payment: PaymentEntity)

    @Query("SELECT * FROM payments WHERE id = :id")
    suspend fun get(id: Long): PaymentEntity?

    @Query("SELECT * FROM payments WHERE tenantId = :tenantId ORDER BY date DESC, id DESC")
    fun observeByTenant(tenantId: Long): Flow<List<PaymentEntity>>

    @Query("SELECT * FROM payments ORDER BY date, id")
    fun observeAll(): Flow<List<PaymentEntity>>

    @Query(
        """
        SELECT p.*, t.name AS tenantName FROM payments p JOIN tenants t ON t.id = p.tenantId
        WHERE p.date >= :from AND p.date < :to
        ORDER BY p.date DESC, p.id DESC
        """,
    )
    fun observeBetween(from: Long, to: Long): Flow<List<PaymentTenantRow>>
}

@Dao
abstract class BillDao {
    @Insert
    abstract suspend fun insertBill(bill: BillEntity): Long

    @Update
    abstract suspend fun updateBill(bill: BillEntity)

    @Insert
    abstract suspend fun insertShares(shares: List<BillShareEntity>)

    @Query("DELETE FROM bill_shares WHERE billId = :billId")
    abstract suspend fun deleteShares(billId: Long)

    @Transaction
    open suspend fun insertWithShares(bill: BillEntity, shares: List<BillShareEntity>): Long {
        val id = insertBill(bill)
        insertShares(shares.map { it.copy(billId = id) })
        return id
    }

    @Transaction
    open suspend fun updateWithShares(bill: BillEntity, shares: List<BillShareEntity>) {
        updateBill(bill)
        deleteShares(bill.id)
        insertShares(shares.map { it.copy(id = 0, billId = bill.id) })
    }

    @Query("DELETE FROM bills WHERE id = :id")
    abstract suspend fun deleteBill(id: Long)

    @Query("SELECT * FROM bills WHERE id = :id")
    abstract suspend fun getBill(id: Long): BillEntity?

    @Query("SELECT * FROM bills ORDER BY month DESC, id DESC")
    abstract fun observeBills(): Flow<List<BillEntity>>

    @Query("SELECT * FROM bill_shares")
    abstract fun observeShares(): Flow<List<BillShareEntity>>

    @Query("SELECT * FROM bill_shares WHERE billId = :billId")
    abstract suspend fun getShares(billId: Long): List<BillShareEntity>
}

@Dao
interface ExpenseDao {
    @Insert
    suspend fun insert(expense: ExpenseEntity): Long

    @Update
    suspend fun update(expense: ExpenseEntity)

    @Delete
    suspend fun delete(expense: ExpenseEntity)

    @Query("SELECT * FROM expenses WHERE id = :id")
    suspend fun get(id: Long): ExpenseEntity?

    @Query("SELECT * FROM expenses ORDER BY date DESC, id DESC")
    fun observeAll(): Flow<List<ExpenseEntity>>
}
