package com.example.myapplication.data.local

import androidx.room.Dao
import androidx.room.Delete
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

    @Query("DELETE FROM users")
    suspend fun clear()
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

    @Query(
        """
    DELETE FROM groups
    WHERE id LIKE 'group-%'
    AND id NOT IN (SELECT entityId FROM sync_metadata WHERE entityType = 'group')
    """
    )
    suspend fun deleteUnlinkedLocalGroups()

    @Query("DELETE FROM group_members WHERE groupId = :groupId")
    suspend fun deleteMembersForGroup(groupId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMember(member: GroupMemberEntity)

    @Query("DELETE FROM group_members WHERE groupId = :groupId AND userId = :userId")
    suspend fun deleteMember(groupId: String, userId: String)

    @Query("DELETE FROM groups")
    suspend fun clear()

    @Query("DELETE FROM group_members")
    suspend fun clearMembers()

    @Query("DELETE FROM groups WHERE id = :groupId")
    suspend fun delete(groupId: String)

    @Query("SELECT * FROM group_members WHERE groupId = :groupId ORDER BY name")
    fun observeMembers(groupId: String): Flow<List<GroupMemberEntity>>

    @Query("SELECT * FROM groups WHERE id = :groupId LIMIT 1")
    fun observeById(groupId: String): Flow<GroupEntity?>
}

@Dao
interface ExpenseDao {
    @Query("SELECT * FROM expenses WHERE groupId = :groupId AND syncStatus != 'DELETED' ORDER BY id DESC")
    fun observeForGroup(groupId: String): Flow<List<ExpenseEntity>>

    /** Includes rows waiting to be deleted, so the balance stays correct while they are queued. */
    @Query("SELECT * FROM expenses WHERE groupId = :groupId ORDER BY id DESC")
    fun observeAllForGroup(groupId: String): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM expenses WHERE groupId = :groupId AND syncStatus != 'DELETED' ORDER BY rowid DESC")
    suspend fun getForGroup(groupId: String): List<ExpenseEntity>

    @Query("SELECT * FROM expenses WHERE id = :expenseId LIMIT 1")
    suspend fun getById(expenseId: String): ExpenseEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(expense: ExpenseEntity)

    @Update
    suspend fun update(expense: ExpenseEntity)

    @Query("UPDATE expenses SET syncStatus = :status WHERE id = :expenseId")
    suspend fun updateSyncStatus(expenseId: String, status: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(expenses: List<ExpenseEntity>)

    @Delete
    suspend fun delete(expense: ExpenseEntity)

    @Query("DELETE FROM expenses")
    suspend fun clear()

    @Query("DELETE FROM expenses WHERE groupId = :groupId")
    suspend fun deleteForGroup(groupId: String)

    @Query("SELECT * FROM expenses WHERE syncStatus NOT IN ('SYNCED', 'DELETED')")
    suspend fun getPending(): List<ExpenseEntity>

    @Query("SELECT * FROM expenses WHERE syncStatus = 'DELETED'")
    suspend fun getDeleted(): List<ExpenseEntity>

    @Query("SELECT * FROM expenses WHERE groupId = :groupId")
    suspend fun getAllForGroup(groupId: String): List<ExpenseEntity>

    @Query("SELECT * FROM expenses")
    suspend fun getAll(): List<ExpenseEntity>
}

@Dao
interface BalanceDao {
    @Query("SELECT * FROM balances ORDER BY groupName, personName")
    suspend fun getAll(): List<BalanceEntity>

    @Query("SELECT * FROM balances WHERE groupId = :groupId ORDER BY personName")
    suspend fun getForGroup(groupId: String): List<BalanceEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(balances: List<BalanceEntity>)

    @Query("DELETE FROM balances")
    suspend fun clear()

    @Query("DELETE FROM balances WHERE groupId = :groupId")
    suspend fun deleteForGroup(groupId: String)
}

@Dao
interface NotificationDao {
    @Query("SELECT * FROM notifications ORDER BY createdAtEpochMs DESC")
    suspend fun getAll(): List<NotificationEntity>

    @Query("SELECT * FROM notifications ORDER BY createdAtEpochMs DESC")
    fun observeAll(): Flow<List<NotificationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(notifications: List<NotificationEntity>)

    @Query("UPDATE notifications SET isUnread = 0 WHERE id = :id")
    suspend fun markRead(id: String)

    @Query("DELETE FROM notifications")
    suspend fun clear()

    @Query("DELETE FROM notifications WHERE id = :id")
    suspend fun delete(id: String)
}

@Dao
interface SyncMetadataDao {
    @Query("SELECT * FROM sync_metadata WHERE state != 'synced'")
    suspend fun getPending(): List<SyncMetadataEntity>

    @Query("SELECT * FROM sync_metadata WHERE entityType = :entityType AND entityId = :entityId LIMIT 1")
    suspend fun get(entityType: String, entityId: String): SyncMetadataEntity?

    @Query("SELECT * FROM sync_metadata WHERE entityType = :entityType AND remoteId = :remoteId LIMIT 1")
    suspend fun getByRemoteId(entityType: String, remoteId: String): SyncMetadataEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(metadata: SyncMetadataEntity)

    @Query("DELETE FROM sync_metadata WHERE entityType = :entityType AND entityId = :entityId")
    suspend fun delete(entityType: String, entityId: String)

    @Query("DELETE FROM sync_metadata")
    suspend fun clear()

    @Query(
        """SELECT * FROM sync_metadata WHERE entityType = 'member_invite' AND state != 'synced' AND substr(entityId, 1, length(:prefix)) = :prefix"""
    )
    fun observePendingInvites(prefix: String): Flow<List<SyncMetadataEntity>>
}
