package com.example.myapplication.data.repository

import com.example.myapplication.data.local.AppDatabase
import com.example.myapplication.data.local.BalanceEntity
import com.example.myapplication.data.local.ExpenseEntity
import com.example.myapplication.data.local.GroupEntity
import com.example.myapplication.data.local.GroupMemberEntity
import com.example.myapplication.data.local.NotificationEntity
import com.example.myapplication.data.local.SyncMetadataEntity
import com.example.myapplication.data.local.SyncOperation
import com.example.myapplication.data.local.SyncState
import com.example.myapplication.data.local.SyncStatus
import com.example.myapplication.data.local.UserEntity
import com.example.myapplication.data.model.AppNotification
import com.example.myapplication.data.model.Balance
import com.example.myapplication.data.model.Expense
import com.example.myapplication.data.model.GroupMember
import com.example.myapplication.data.model.GroupSummary
import com.example.myapplication.data.model.User
import com.example.myapplication.data.sync.NoOpSyncScheduler
import com.example.myapplication.data.sync.SyncScheduler
import com.example.myapplication.feature.groups.GroupDetails
import com.example.myapplication.feature.groups.GroupExpenseItem
import com.example.myapplication.ui.Format
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

private fun UserEntity.toModel() = User(id, name, email, phone, notificationsEnabled)
private fun User.toEntity() = UserEntity(id, name, email, phone, notificationsEnabled)

private fun GroupEntity.toModel() = GroupSummary(id, name, memberCount, balanceText)
private fun GroupMemberEntity.toModel() = GroupMember(userId, name, isAdmin)

private fun ExpenseEntity.toModel() = Expense(
    id = id,
    groupId = groupId,
    title = title,
    amountInCents = amountInCents,
    paidBy = paidBy,
    splitBetween = splitBetween.split("|").filter(String::isNotBlank)
)

private fun Expense.toEntity() = ExpenseEntity(
    id = id,
    groupId = groupId,
    title = title,
    amountInCents = amountInCents,
    paidBy = paidBy,
    splitBetween = splitBetween.joinToString("|")
)

private fun BalanceEntity.toModel() = Balance(groupId, groupName, description, amountDkk, isOwedToCurrentUser)
private fun NotificationEntity.toModel() = AppNotification(id, title, description, timeLabel)

class RoomGroupRepository(
    database: AppDatabase,
    private val currentUserId: () -> String?,
    private val syncScheduler: SyncScheduler = NoOpSyncScheduler()
) : GroupRepository {
    private val dao = database.groupDao()
    private val syncDao = database.syncMetadataDao()
    private val expenseDao = database.expenseDao()

    override suspend fun getGroups(): Result<List<GroupSummary>> = runCatching {
        dao.getAll().map(GroupEntity::toModel)
    }

    override suspend fun getDetails(groupId: String): Result<GroupDetails> = runCatching {
        val group = dao.getById(groupId)
            ?: error("This group is not available offline yet.")

        val me = currentUserId()
        val namesByUserId = dao.getMembers(groupId).associate { it.userId to it.name }

        val expenses = expenseDao.getForGroup(groupId).map { expense ->
            val payerName = when {
                expense.paidBy == me -> "You"
                expense.paidByName.isNotBlank() -> expense.paidByName
                else -> namesByUserId[expense.paidBy] ?: "Unknown"
            }

            val split = expense.splitBetween.split("|").filter(String::isNotBlank)
            val myShareInCents = expense.myShareInCents
                ?: if (me != null && me in split) expense.amountInCents / split.size else 0L
            val shareText = if (myShareInCents > 0) {
                "your share ${Format.dkk(myShareInCents / 100.0)}"
            } else {
                "not in the split"
            }

            GroupExpenseItem(
                id = expense.id,
                title = expense.title,
                amountText = Format.dkk(expense.amountInCents / 100.0),
                detailText = "$payerName paid · $shareText"
            )
        }

        GroupDetails(
            id = group.id,
            name = group.name,
            description = group.description,
            balanceText = group.balanceText,
            expenses = expenses
        )
    }

    override suspend fun getMembers(groupId: String): Result<List<GroupMember>> = runCatching {
        dao.getMembers(groupId).map(GroupMemberEntity::toModel)
    }

    override suspend fun createGroup(
        title: String,
        description: String,
        invitedEmails: List<String>
    ): Result<GroupSummary> = runCatching {
        val creatorId = currentUserId() ?: error("You are not signed in.")
        val now = System.currentTimeMillis()

        val group = GroupEntity(
            id = "group-$now",
            name = title,
            description = description,
            memberCount = 1 + invitedEmails.size,
            balanceText = "No balance yet"
        )
        dao.insert(group)
        dao.insertMember(GroupMemberEntity(group.id, creatorId, "You", true))

        syncDao.upsert(
            SyncMetadataEntity(
                entityType = "group",
                entityId = group.id,
                state = SyncState.PENDING_UPLOAD,
                operation = SyncOperation.CREATE,
                localUpdatedAt = now
            )
        )

        invitedEmails
            .map(String::trim)
            .filter(String::isNotBlank)
            .distinct()
            .forEach { email ->
                syncDao.upsert(
                    SyncMetadataEntity(
                        entityType = "member_invite",
                        entityId = "${group.id}|$email",
                        state = SyncState.PENDING_UPLOAD,
                        operation = SyncOperation.CREATE,
                        localUpdatedAt = now
                    )
                )
            }
        syncScheduler.enqueueSync()
        group.toModel()
    }

    override suspend fun inviteMember(groupId: String, email: String): Result<Unit> = runCatching {
        val normalized = email.trim()
        require(normalized.isNotBlank()) { "Enter an email address" }

        syncDao.upsert(
            SyncMetadataEntity(
                entityType = "member_invite",
                entityId = "$groupId|$normalized",
                state = SyncState.PENDING_UPLOAD,
                operation = SyncOperation.CREATE,
                localUpdatedAt = System.currentTimeMillis()
            )
        )
        syncScheduler.enqueueSync()
    }

    override suspend fun removeMember(groupId: String, userId: String): Result<Unit> = runCatching {
        dao.deleteMember(groupId, userId)
        syncDao.upsert(
            SyncMetadataEntity(
                entityType = "member_remove",
                entityId = "$groupId|$userId",
                state = SyncState.PENDING_UPLOAD,
                operation = SyncOperation.DELETE,
                localUpdatedAt = System.currentTimeMillis()
            )
        )
        syncScheduler.enqueueSync()
    }

    override fun observeMembers(groupId: String): Flow<List<GroupMember>> = combine(
        dao.observeMembers(groupId),
        syncDao.observePendingInvites("$groupId|")
    ) { entities, invites ->
        entities.map(GroupMemberEntity::toModel) +
            invites.map { metadata ->
                GroupMember(
                    id = "pending:${metadata.entityId}",
                    name = metadata.entityId.substringAfter("|"),
                    isPending = true,
                    isFailed = metadata.state == SyncState.FAILED
                )
            }
    }

    override suspend fun cancelInvite(groupId: String, email: String): Result<Unit> = runCatching {
        syncDao.delete("member_invite", "$groupId|${email.trim()}")
    }
}

class RoomExpenseRepository(database: AppDatabase, private val syncScheduler: SyncScheduler = NoOpSyncScheduler()) :
    ExpenseRepository {
    private val dao = database.expenseDao()
    private val syncDao = database.syncMetadataDao()

    override suspend fun getExpenses(groupId: String): Result<List<Expense>> = runCatching {
        dao.getForGroup(groupId).map(ExpenseEntity::toModel)
    }

    override suspend fun addExpense(expense: Expense): Result<Expense> = runCatching {
        dao.insert(expense.toEntity().copy(syncStatus = SyncStatus.PENDING))
        syncDao.upsert(
            SyncMetadataEntity(
                entityType = "expense",
                entityId = expense.id,
                state = SyncState.PENDING_UPLOAD,
                operation = SyncOperation.CREATE,
                localUpdatedAt = System.currentTimeMillis()
            )
        )
        syncScheduler.enqueueSync()
        expense
    }

    override suspend fun updateExpense(expense: Expense): Result<Expense> = runCatching {
        dao.update(expense.toEntity().copy(syncStatus = SyncStatus.PENDING))
        syncDao.upsert(
            SyncMetadataEntity(
                entityType = "expense",
                entityId = expense.id,
                state = SyncState.PENDING_UPLOAD,
                operation = SyncOperation.UPDATE,
                localUpdatedAt = System.currentTimeMillis()
            )
        )
        syncScheduler.enqueueSync()
        expense
    }

    override fun observeExpenses(groupId: String) = dao.observeForGroup(groupId)

    override suspend fun addExpense(
        groupId: String,
        description: String,
        amountInCents: Long,
        payerId: String
    ): Result<Expense> = runCatching {
        val expense = Expense(
            id = "expense-${System.currentTimeMillis()}",
            groupId = groupId,
            title = description,
            amountInCents = amountInCents,
            paidBy = payerId,
            splitBetween = listOf(payerId)
        )
        dao.insert(expense.toEntity().copy(syncStatus = SyncStatus.PENDING))
        syncDao.upsert(
            SyncMetadataEntity(
                entityType = "expense",
                entityId = expense.id,
                state = SyncState.PENDING_UPLOAD,
                operation = SyncOperation.CREATE,
                localUpdatedAt = System.currentTimeMillis()
            )
        )
        syncScheduler.enqueueSync()
        expense
    }

    override suspend fun retryExpense(expenseId: String): Result<Unit> = runCatching {
        dao.updateSyncStatus(expenseId, SyncStatus.PENDING)
        syncScheduler.enqueueSync()
    }

    override suspend fun getPendingExpenses() = dao.getPending()
}

class RoomBalanceRepository(database: AppDatabase) : BalanceRepository {
    private val dao = database.balanceDao()

    override suspend fun getBalances(): Result<List<Balance>> = runCatching {
        dao.getAll().map(BalanceEntity::toModel)
    }
}

class RoomNotificationRepository(database: AppDatabase) : NotificationRepository {
    private val dao = database.notificationDao()

    override suspend fun getNotifications(): Result<List<AppNotification>> = runCatching {
        dao.getAll().map(NotificationEntity::toModel)
    }
}

class RoomProfileRepository(database: AppDatabase, private val currentUserId: () -> String?) : ProfileRepository {
    private val dao = database.userDao()
    private val syncDao = database.syncMetadataDao()

    override suspend fun getProfile(): Result<User> = runCatching {
        val id = currentUserId() ?: error("You are not signed in.")
        dao.getById(id)?.toModel()
            ?: error("Profile has not been created yet")
    }

    override suspend fun updateProfile(user: User): Result<User> = runCatching {
        val id = currentUserId() ?: error("You are not signed in.")
        val stored = user.copy(id = id)
        dao.insert(stored.toEntity())
        syncDao.upsert(
            SyncMetadataEntity(
                entityType = "profile",
                entityId = id,
                state = SyncState.PENDING_UPLOAD,
                operation = SyncOperation.UPDATE,
                localUpdatedAt = System.currentTimeMillis()
            )
        )
        stored
    }
}
