package com.kiraya.diary.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.kiraya.diary.data.local.entity.PropertyEntity
import com.kiraya.diary.data.local.entity.UnitEntity
import com.kiraya.diary.data.local.model.PropertyCountsRow
import com.kiraya.diary.data.local.model.UnitStatusRow
import kotlinx.coroutines.flow.Flow

@Dao
interface PropertyDao {
    @Query("SELECT * FROM properties ORDER BY name COLLATE NOCASE")
    fun observeAll(): Flow<List<PropertyEntity>>

    @Query(
        """
        SELECT p.*,
          (SELECT COUNT(*) FROM units u WHERE u.propertyId = p.id) AS unitCount,
          (SELECT COUNT(DISTINCT t.unitId) FROM tenants t JOIN units u ON u.id = t.unitId
             WHERE u.propertyId = p.id AND t.isActive = 1) AS occupiedCount
        FROM properties p
        ORDER BY p.name COLLATE NOCASE
        """,
    )
    fun observeAllWithCounts(): Flow<List<PropertyCountsRow>>

    @Query("SELECT * FROM properties WHERE id = :id")
    fun observe(id: Long): Flow<PropertyEntity?>

    @Query("SELECT * FROM properties WHERE id = :id")
    suspend fun get(id: Long): PropertyEntity?

    @Insert
    suspend fun insert(property: PropertyEntity): Long

    @Update
    suspend fun update(property: PropertyEntity)

    @Delete
    suspend fun delete(property: PropertyEntity)
}

@Dao
interface UnitDao {
    @Query(
        """
        SELECT u.*, p.name AS propertyName, t.id AS tenantId, t.name AS tenantName
        FROM units u
        JOIN properties p ON p.id = u.propertyId
        LEFT JOIN tenants t ON t.unitId = u.id AND t.isActive = 1
        WHERE u.propertyId = :propertyId
        GROUP BY u.id
        ORDER BY u.name COLLATE NOCASE
        """,
    )
    fun observeByProperty(propertyId: Long): Flow<List<UnitStatusRow>>

    @Query(
        """
        SELECT u.*, p.name AS propertyName, t.id AS tenantId, t.name AS tenantName
        FROM units u
        JOIN properties p ON p.id = u.propertyId
        LEFT JOIN tenants t ON t.unitId = u.id AND t.isActive = 1
        GROUP BY u.id
        ORDER BY p.name COLLATE NOCASE, u.name COLLATE NOCASE
        """,
    )
    fun observeAllWithStatus(): Flow<List<UnitStatusRow>>

    @Query("SELECT * FROM units WHERE id = :id")
    suspend fun get(id: Long): UnitEntity?

    @Insert
    suspend fun insert(unit: UnitEntity): Long

    @Update
    suspend fun update(unit: UnitEntity)

    @Delete
    suspend fun delete(unit: UnitEntity)
}
