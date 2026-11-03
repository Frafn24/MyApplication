package com.example.myapplication.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE id = :userId LIMIT 1")
    suspend fun getById(userId: String): UserEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(user: UserEntity)

    @Update
    suspend fun update(user: UserEntity)
}

@Dao
interface GroupDao {
    @Query("SELECT * FROM groups ORDER BY name")
    suspend fun getAll(): List<GroupEntity>

    @Query("SELECT * FROM groups WHERE id = :groupId LIMIT 1")
    suspend fun getById(groupId: String): GroupEntity?

    @Query("SELECT * FROM group_members WHERE groupId = :groupId ORDER BY name")
    suspend fun getMembers(groupId: String): List<GroupMemberEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(group: GroupEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMember(member: GroupMemberEntity)
}

@Dao
interface ExpenseDao {
    @Query("SELECT * FROM expenses WHERE groupId = :groupId ORDER BY id DESC")
    fun observeForGroup(groupId: String): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM expenses WHERE groupId = :groupId ORDER BY title")
    suspend fun getForGroup(groupId: String): List<ExpenseEntity>

    @Query("SELECT * FROM expenses WHERE id = :expenseId LIMIT 1")
    suspend fun getById(expenseId: String): ExpenseEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(expense: ExpenseEntity)

    @Update
    suspend fun update(expense: ExpenseEntity)

    @Query("SELECT * FROM expenses WHERE syncStatus = 'PENDING'")
    suspend fun getPending(): List<ExpenseEntity>

    @Query("UPDATE expenses SET syncStatus = :status WHERE id = :expenseId")
    suspend fun updateSyncStatus(expenseId: String, status: String)
}

@Dao
interface BalanceDao {
    @Query("SELECT * FROM balances ORDER BY groupName")
    suspend fun getAll(): List<BalanceEntity>
}

@Dao
interface NotificationDao {
    @Query("SELECT * FROM notifications ORDER BY id DESC")
    suspend fun getAll(): List<NotificationEntity>
}

@Dao
interface SyncMetadataDao {
    @Query("SELECT * FROM sync_metadata WHERE state != 'synced'")
    suspend fun getPending(): List<SyncMetadataEntity>

    @Query("SELECT * FROM sync_metadata WHERE entityType = :entityType AND entityId = :entityId LIMIT 1")
    suspend fun get(entityType: String, entityId: String): SyncMetadataEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(metadata: SyncMetadataEntity)
}
