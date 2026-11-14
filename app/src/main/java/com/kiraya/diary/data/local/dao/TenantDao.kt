package com.kiraya.diary.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import com.kiraya.diary.data.local.entity.LeaseEntity
import com.kiraya.diary.data.local.entity.TenantEntity
import com.kiraya.diary.data.local.model.LeaseTenantRow
import com.kiraya.diary.data.local.model.TenantUnitRow
import kotlinx.coroutines.flow.Flow

@Dao
interface TenantDao {
    @Query(
        """
        SELECT t.*, u.name AS unitName, p.id AS propertyId, p.name AS propertyName
        FROM tenants t
        LEFT JOIN units u ON u.id = t.unitId
        LEFT JOIN properties p ON p.id = u.propertyId
        WHERE t.isActive = 1
        ORDER BY t.name COLLATE NOCASE
        """,
    )
    fun observeActiveWithUnit(): Flow<List<TenantUnitRow>>

    @Query(
        """
        SELECT t.*, u.name AS unitName, p.id AS propertyId, p.name AS propertyName
        FROM tenants t
        LEFT JOIN units u ON u.id = t.unitId
        LEFT JOIN properties p ON p.id = u.propertyId
        ORDER BY t.isActive DESC, t.name COLLATE NOCASE
        """,
    )
    fun observeAllWithUnit(): Flow<List<TenantUnitRow>>

    @Query(
        """
        SELECT t.*, u.name AS unitName, p.id AS propertyId, p.name AS propertyName
        FROM tenants t
        LEFT JOIN units u ON u.id = t.unitId
        LEFT JOIN properties p ON p.id = u.propertyId
        WHERE t.id = :id
        """,
    )
    fun observeWithUnit(id: Long): Flow<TenantUnitRow?>

    @Query("SELECT * FROM tenants WHERE isActive = 1")
    suspend fun getActive(): List<TenantEntity>

    @Query("SELECT * FROM tenants")
    fun observeAll(): Flow<List<TenantEntity>>

    @Query("SELECT * FROM tenants WHERE id = :id")
    suspend fun get(id: Long): TenantEntity?

    @Insert
    suspend fun insert(tenant: TenantEntity): Long

    @Update
    suspend fun update(tenant: TenantEntity)

    @Delete
    suspend fun delete(tenant: TenantEntity)
}

@Dao
interface LeaseDao {
    @Query("SELECT * FROM leases WHERE tenantId = :tenantId")
    fun observeByTenant(tenantId: Long): Flow<LeaseEntity?>

    @Query("SELECT * FROM leases WHERE tenantId = :tenantId")
    suspend fun getByTenant(tenantId: Long): LeaseEntity?

    @Query(
        """
        SELECT l.*, t.name AS tenantName, t.phone AS tenantPhone
        FROM leases l JOIN tenants t ON t.id = l.tenantId
        WHERE t.isActive = 1 AND l.endDate IS NOT NULL AND l.endDate < :before
        ORDER BY l.endDate
        """,
    )
    fun observeEndingBefore(before: Long): Flow<List<LeaseTenantRow>>

    @Query(
        """
        SELECT l.*, t.name AS tenantName, t.phone AS tenantPhone
        FROM leases l JOIN tenants t ON t.id = l.tenantId
        WHERE t.isActive = 1 AND l.endDate IS NOT NULL AND l.endDate < :before
        ORDER BY l.endDate
        """,
    )
    suspend fun getEndingBefore(before: Long): List<LeaseTenantRow>

    @Upsert
    suspend fun upsert(lease: LeaseEntity): Long

    @Delete
    suspend fun delete(lease: LeaseEntity)
}
