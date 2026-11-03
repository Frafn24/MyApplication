package com.example.myapplication.data.repository

import com.example.myapplication.data.local.AppDatabase
import com.example.myapplication.data.local.BalanceEntity
import com.example.myapplication.data.local.ExpenseEntity
import com.example.myapplication.data.local.GroupEntity
import com.example.myapplication.data.local.GroupMemberEntity
import com.example.myapplication.data.local.NotificationEntity
import com.example.myapplication.data.local.UserEntity
import com.example.myapplication.data.local.SyncStatus
import com.example.myapplication.data.model.AppNotification
import com.example.myapplication.data.model.Balance
import com.example.myapplication.data.model.Expense
import com.example.myapplication.data.model.GroupMember
import com.example.myapplication.data.model.GroupSummary
import com.example.myapplication.data.model.User
import com.example.myapplication.data.sync.NoOpSyncScheduler
import com.example.myapplication.data.sync.SyncScheduler

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
    splitBetween = splitBetween.split("|").filter(String::isNotBlank),
)

private fun Expense.toEntity() = ExpenseEntity(
    id = id,
    groupId = groupId,
    title = title,
    amountInCents = amountInCents,
    paidBy = paidBy,
    splitBetween = splitBetween.joinToString("|"),
)

private fun BalanceEntity.toModel() = Balance(groupId, groupName, description, amountDkk, isOwedToCurrentUser)
private fun NotificationEntity.toModel() = AppNotification(id, title, description, timeLabel)

class RoomGroupRepository(database: AppDatabase) : GroupRepository {
    private val dao = database.groupDao()

    override suspend fun getGroups(): Result<List<GroupSummary>> = runCatching {
        dao.getAll().map(GroupEntity::toModel)
    }

    override suspend fun getMembers(groupId: String): Result<List<GroupMember>> = runCatching {
        dao.getMembers(groupId).map(GroupMemberEntity::toModel)
    }

    override suspend fun createGroup(title: String, description: String): Result<GroupSummary> = runCatching {
        val group = GroupEntity(
            id = "group-${System.currentTimeMillis()}",
            name = title,
            description = description,
            memberCount = 1,
            balanceText = "No balance yet",
        )
        dao.insert(group)
        dao.insertMember(GroupMemberEntity(group.id, "current-user", "You", true))
        group.toModel()
    }

    override suspend fun addMember(groupId: String, name: String): Result<GroupMember> = runCatching {
        val member = GroupMemberEntity(groupId, "member-${System.currentTimeMillis()}", name, false)
        dao.insertMember(member)
        member.toModel()
    }
}

class RoomExpenseRepository(
    database: AppDatabase,
    private val syncScheduler: SyncScheduler = NoOpSyncScheduler(),
) : ExpenseRepository {
    private val dao = database.expenseDao()

    override suspend fun getExpenses(groupId: String): Result<List<Expense>> = runCatching {
        dao.getForGroup(groupId).map(ExpenseEntity::toModel)
    }

    override suspend fun addExpense(expense: Expense): Result<Expense> = runCatching {
        dao.insert(expense.toEntity().copy(syncStatus = SyncStatus.PENDING))
        syncScheduler.enqueueExpenseSync()
        expense
    }

    override suspend fun updateExpense(expense: Expense): Result<Expense> = runCatching {
        dao.update(expense.toEntity().copy(syncStatus = SyncStatus.PENDING))
        syncScheduler.enqueueExpenseSync()
        expense
    }

    override fun observeExpenses(groupId: String) = dao.observeForGroup(groupId)

    override suspend fun addExpense(
        groupId: String,
        description: String,
        amountInCents: Long,
        payerId: String,
    ): Result<Expense> = runCatching {
        val expense = Expense(
            id = "expense-${System.currentTimeMillis()}",
            groupId = groupId,
            title = description,
            amountInCents = amountInCents,
            paidBy = payerId,
            splitBetween = listOf(payerId),
        )
        dao.insert(expense.toEntity().copy(syncStatus = SyncStatus.PENDING))
        syncScheduler.enqueueExpenseSync()
        expense
    }

    override suspend fun retryExpense(expenseId: String): Result<Unit> = runCatching {
        dao.updateSyncStatus(expenseId, SyncStatus.PENDING)
        syncScheduler.enqueueExpenseSync()
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

class RoomProfileRepository(database: AppDatabase) : ProfileRepository {
    private val dao = database.userDao()

    override suspend fun getProfile(): Result<User> = runCatching {
        dao.getById("current-user")?.toModel()
            ?: error("Profile has not been created yet")
    }

    override suspend fun updateProfile(user: User): Result<User> = runCatching {
        dao.insert(user.toEntity())
        user
    }
}
