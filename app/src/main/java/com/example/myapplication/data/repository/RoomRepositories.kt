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
import com.example.myapplication.data.model.BalanceDirection
import com.example.myapplication.data.model.BalanceEntry
import com.example.myapplication.data.model.BalancesSummary
import com.example.myapplication.data.model.Expense
import com.example.myapplication.data.model.GroupBalances
import com.example.myapplication.data.model.GroupMember
import com.example.myapplication.data.model.GroupSummary
import com.example.myapplication.data.model.User
import com.example.myapplication.data.sync.NoOpSyncScheduler
import com.example.myapplication.data.sync.SyncScheduler
import com.example.myapplication.data.sync.decodeSplits
import com.example.myapplication.data.sync.encodeSplits
import com.example.myapplication.data.sync.equalSplit
import com.example.myapplication.feature.expenses.ExpenseDetails
import com.example.myapplication.feature.expenses.ExpenseDraft
import com.example.myapplication.feature.expenses.ExpenseEdit
import com.example.myapplication.feature.expenses.ExpenseMember
import com.example.myapplication.feature.expenses.ExpenseSplit
import com.example.myapplication.feature.expenses.SharedExpenses
import com.example.myapplication.feature.groups.GroupDetails
import com.example.myapplication.feature.groups.GroupExpenseItem
import com.example.myapplication.ui.Format
import java.math.BigDecimal
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

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

/**
 * What the expense moves for [me]: what they paid, minus their own share.
 * Mirrors the server's balance calculation so offline changes show up right away.
 */
private fun ExpenseEntity.netInCents(me: String?): Long {
    if (me == null) return 0L

    val people = splitBetween.split("|").filter(String::isNotBlank)
    val share = decodeSplits(splitAmounts)[me]
        ?: myShareInCents
        ?: if (me in people) amountInCents / people.size else 0L

    return (if (paidBy == me) amountInCents else 0L) - share
}

/** Rows queued for deletion no longer count. */
private fun List<ExpenseEntity>.netBalanceInCents(me: String?): Long =
    filter { it.syncStatus != SyncStatus.DELETED }.sumOf { it.netInCents(me) }

/** Only recompute locally once this phone has changes the server does not know about. */
private fun List<ExpenseEntity>.hasLocalChanges(): Boolean = any { it.syncStatus != SyncStatus.SYNCED }

private fun NotificationEntity.toModel() = AppNotification(
    id = id,
    title = title,
    message = message,
    timeLabel = if (createdAtEpochMs > 0) {
        Format.relativeTime(
            kotlin.time.Instant.fromEpochMilliseconds(createdAtEpochMs),
            kotlin.time.Clock.System.now()
        )
    } else {
        // Rows cached before creation times were stored.
        "Earlier"
    },
    isUnread = isUnread,
    groupId = groupId,
    expenseId = expenseId
)

class RoomGroupRepository(
    database: AppDatabase,
    private val currentUserId: () -> String? = { null },
    private val syncScheduler: SyncScheduler = NoOpSyncScheduler()
) : GroupRepository {
    private val dao = database.groupDao()
    private val syncDao = database.syncMetadataDao()
    private val expenseDao = database.expenseDao()

    override suspend fun getGroups(): Result<List<GroupSummary>> = runCatching {
        val me = currentUserId()
        val expensesByGroup = expenseDao.getAll().groupBy { it.groupId }

        dao.getAll().map { group ->
            val expenses = expensesByGroup[group.id].orEmpty()

            GroupSummary(
                id = group.id,
                name = group.name,
                memberCount = group.memberCount,
                balanceText = if (expenses.hasLocalChanges()) {
                    Format.groupBalance(expenses.netBalanceInCents(me) / 100.0)
                } else {
                    group.balanceText
                }
            )
        }
    }

    override suspend fun getDetails(groupId: String): Result<GroupDetails> = runCatching {
        val group = dao.getById(groupId)
            ?: error("This group is not available offline yet.")

        buildDetails(group, dao.getMembers(groupId), expenseDao.getAllForGroup(groupId))
    }

    override fun observeDetails(groupId: String): Flow<GroupDetails?> = combine(
        dao.observeById(groupId),
        dao.observeMembers(groupId),
        expenseDao.observeAllForGroup(groupId)
    ) { group, members, expenses ->
        group?.let { buildDetails(it, members, expenses) }
    }

    private fun buildDetails(
        group: GroupEntity,
        members: List<GroupMemberEntity>,
        expenses: List<ExpenseEntity>
    ): GroupDetails {
        val me = currentUserId()
        val namesByUserId = members.associate { it.userId to it.name }

        // Rows queued for deletion are hidden, but still counted above.
        val items = expenses.filter { it.syncStatus != SyncStatus.DELETED }.map { expense ->
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
                detailText = "$payerName paid · $shareText",
                isPending = expense.syncStatus != SyncStatus.SYNCED,
                syncFailed = expense.syncStatus == SyncStatus.FAILED
            )
        }

        return GroupDetails(
            id = group.id,
            name = group.name,
            description = group.description,
            balanceText = if (expenses.hasLocalChanges()) {
                Format.groupBalance(expenses.netBalanceInCents(me) / 100.0)
            } else {
                group.balanceText
            },
            expenses = items
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

class RoomExpenseRepository(
    database: AppDatabase,
    private val currentUserId: () -> String? = { null },
    private val syncScheduler: SyncScheduler = NoOpSyncScheduler()
) : ExpenseRepository {
    private val dao = database.expenseDao()
    private val groupDao = database.groupDao()
    private val syncDao = database.syncMetadataDao()

    override suspend fun getExpenses(groupId: String): Result<List<Expense>> = runCatching {
        dao.getForGroup(groupId).map(ExpenseEntity::toModel)
    }

    override suspend fun getExpenseDetails(expenseId: String): Result<ExpenseDetails> = runCatching {
        val expense = dao.getById(expenseId)
            ?: error("This expense is not available offline yet.")

        val me = currentUserId()
        val names = groupDao.getMembers(expense.groupId).associate { it.userId to it.name }

        // Rows stored before splits were cached fall back to an equal split.
        val amounts = decodeSplits(expense.splitAmounts).ifEmpty {
            equalSplit(
                expense.amountInCents,
                expense.splitBetween.split("|").filter(String::isNotBlank)
            )
        }

        ExpenseDetails(
            title = expense.title,
            amount = BigDecimal.valueOf(expense.amountInCents, 2),
            paidBy = when {
                expense.paidBy == me -> "You"
                expense.paidByName.isNotBlank() -> expense.paidByName
                else -> names[expense.paidBy] ?: "Unknown"
            },
            splits = amounts.map { (userId, cents) ->
                ExpenseSplit(
                    memberName = if (userId == me) "You" else names[userId] ?: "Unknown",
                    amount = BigDecimal.valueOf(cents, 2)
                )
            },
            canEdit = if (expense.createdById.isNotBlank()) {
                expense.createdById == me
            } else {
                // Locally created rows have no server value yet.
                expense.syncStatus == SyncStatus.PENDING
            },
            canDelete = expense.paidBy == me,
            isPending = expense.syncStatus != SyncStatus.SYNCED,
            syncFailed = expense.syncStatus == SyncStatus.FAILED
        )
    }

    override suspend fun getExpenseEdit(expenseId: String): Result<ExpenseEdit> = runCatching {
        val expense = dao.getById(expenseId)
            ?: error("This expense is not available offline yet.")

        val me = currentUserId()
        val members = groupDao.getMembers(expense.groupId)
        val memberIds = members.map { it.userId }.toSet()

        ExpenseEdit(
            groupId = expense.groupId,
            draft = ExpenseDraft(
                title = expense.title,
                amount = BigDecimal.valueOf(expense.amountInCents, 2),
                paidBy = expense.paidBy,
                // People who have left the group can no longer be in the split.
                splitBetween = expense.splitBetween
                    .split("|")
                    .filter { it in memberIds }
            ),
            members = members
                .sortedByDescending { it.userId == me }
                .map { ExpenseMember(it.userId, if (it.userId == me) "You" else it.name) }
        )
    }

    override suspend fun addExpense(expense: Expense): Result<Expense> = runCatching {
        dao.insert(
            expense.toEntity().copy(
                createdById = currentUserId().orEmpty(),
                splitAmounts = encodeSplits(equalSplit(expense.amountInCents, expense.splitBetween)),
                syncStatus = SyncStatus.PENDING
            )
        )
        syncScheduler.enqueueSync()
        expense
    }

    override suspend fun updateExpense(expense: Expense): Result<Expense> = runCatching {
        val existing = dao.getById(expense.id)
        val metadata = syncDao.get("expense", expense.id)

        dao.update(
            expense.toEntity().copy(
                createdById = existing?.createdById ?: currentUserId().orEmpty(),
                splitAmounts = encodeSplits(equalSplit(expense.amountInCents, expense.splitBetween)),
                syncStatus = SyncStatus.PENDING
            )
        )
        syncDao.upsert(
            SyncMetadataEntity(
                entityType = "expense",
                entityId = expense.id,
                state = SyncState.PENDING_UPLOAD,
                // Not uploaded yet: keep it as a create, not an update.
                operation = if (metadata?.remoteId == null) {
                    SyncOperation.CREATE
                } else {
                    SyncOperation.UPDATE
                },
                localUpdatedAt = System.currentTimeMillis(),
                remoteId = metadata?.remoteId
            )
        )
        syncScheduler.enqueueSync()
        expense
    }

    override fun observeExpenses(groupId: String) = dao.observeForGroup(groupId)

    /**
     * The expenses behind one balance entry: everything between the signed-in
     * user and [userId] in that group, computed from the local cache. The group
     * id may be the server's (as stored in the balances table) or a local one.
     */
    override fun observeSharedExpenses(groupId: String, userId: String): Flow<SharedExpenses?> = flow {
        // Balances store the server's group id; groups made offline live under a local one.
        val localGroupId = syncDao.getByRemoteId("group", groupId)?.entityId ?: groupId

        emitAll(
            combine(
                groupDao.observeById(localGroupId),
                groupDao.observeMembers(localGroupId),
                dao.observeAllForGroup(localGroupId)
            ) { group, members, expenses ->
                if (group == null) return@combine null

                val me = currentUserId()
                val personName = members.firstOrNull { it.userId == userId }?.name ?: "Unknown"

                var totalCents = 0L
                val items = expenses
                    .filter { it.syncStatus != SyncStatus.DELETED }
                    .mapNotNull { expense ->
                        val people = expense.splitBetween.split("|").filter(String::isNotBlank)
                        val splits = decodeSplits(expense.splitAmounts).ifEmpty {
                            equalSplit(expense.amountInCents, people)
                        }

                        val share = when (expense.paidBy) {
                            me -> splits[userId]
                            userId -> splits[me]
                            else -> null
                        } ?: return@mapNotNull null

                        totalCents += if (expense.paidBy == me) share else -share

                        GroupExpenseItem(
                            id = expense.id,
                            title = expense.title,
                            amountText = Format.dkk(expense.amountInCents / 100.0),
                            detailText = if (expense.paidBy == me) {
                                "You paid · $personName's share ${Format.dkk(share / 100.0)}"
                            } else {
                                "$personName paid · your share ${Format.dkk(share / 100.0)}"
                            }
                        )
                    }

                SharedExpenses(
                    title = "You and $personName · ${group.name}",
                    totalText = when {
                        totalCents > 0 -> "$personName owes you ${Format.dkk(totalCents / 100.0)}"
                        totalCents < 0 -> "You owe $personName ${Format.dkk(-totalCents / 100.0)}"
                        else -> "You are settled up"
                    },
                    expenses = items
                )
            }
        )
    }

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
        dao.insert(
            expense.toEntity().copy(
                createdById = currentUserId().orEmpty(),
                splitAmounts = encodeSplits(equalSplit(amountInCents, expense.splitBetween)),
                syncStatus = SyncStatus.PENDING
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

    override suspend fun deleteExpense(expenseId: String): Result<Unit> = runCatching {
        val expense = dao.getById(expenseId) ?: return@runCatching
        val metadata = syncDao.get("expense", expenseId)

        if (metadata?.remoteId == null) {
            // Never reached the server, so there is nothing to upload.
            dao.delete(expense)
            syncDao.delete("expense", expenseId)
        } else {
            dao.update(expense.copy(syncStatus = SyncStatus.DELETED))
            syncDao.upsert(
                metadata.copy(
                    state = SyncState.PENDING_DELETE,
                    operation = SyncOperation.DELETE,
                    localUpdatedAt = System.currentTimeMillis()
                )
            )
            syncScheduler.enqueueSync()
        }
    }
}

class RoomBalanceRepository(database: AppDatabase, private val syncScheduler: SyncScheduler = NoOpSyncScheduler()) :
    BalanceRepository {
    private val dao = database.balanceDao()
    private val syncDao = database.syncMetadataDao()

    /** Marks entries that have a reminder waiting to be sent. */
    private fun List<BalanceEntity>.toSummary(
        pendingReminders: List<SyncMetadataEntity> = emptyList()
    ): BalancesSummary {
        val groups = groupBy { it.groupId }.map { (groupId, entries) ->
            val groupName = entries.first().groupName
            val balanceEntries = entries.map { e ->
                val matching = pendingReminders.filter { it.entityId.startsWith("${e.groupId}|${e.userId}|") }
                BalanceEntry(
                    id = "${e.groupId}/${e.userId}",
                    personName = e.personName,
                    amountText = Format.dkk(e.amountCents / 100.0),
                    direction = if (e.owedToMe) BalanceDirection.OwedToYou else BalanceDirection.YouOwe,
                    isPending = matching.isNotEmpty(),
                    isFailed = matching.any { it.state == SyncState.FAILED }
                )
            }
            GroupBalances(groupName, balanceEntries)
        }
        val totalCents = sumOf { if (it.owedToMe) it.amountCents else -it.amountCents }
        return BalancesSummary(Format.signedDkk(totalCents / 100.0), groups)
    }

    override suspend fun getBalances(): Result<BalancesSummary> = runCatching {
        dao.getAll().toSummary(syncDao.getPending().filter { it.entityType == "payment_reminder" })
    }

    override fun observeBalances(): Flow<BalancesSummary> = kotlinx.coroutines.flow.combine(
        dao.observeAll(),
        syncDao.observePendingByType("payment_reminder")
    ) { balances, pending -> balances.toSummary(pending) }

    override suspend fun sendPaymentReminder(entry: BalanceEntry): Result<Unit> = runCatching {
        val (groupId, userId) = entry.id.split("/")
        syncDao.upsert(
            SyncMetadataEntity(
                entityType = "payment_reminder",
                entityId = "$groupId|$userId|${System.currentTimeMillis()}",
                state = SyncState.PENDING_UPLOAD,
                operation = SyncOperation.CREATE,
                localUpdatedAt = System.currentTimeMillis()
            )
        )
        syncScheduler.enqueueSync()
    }
}

class RoomNotificationRepository(
    database: AppDatabase,
    private val syncScheduler: SyncScheduler = NoOpSyncScheduler()
) : NotificationRepository {
    private val dao = database.notificationDao()
    private val syncDao = database.syncMetadataDao()

    override suspend fun getNotifications(): Result<List<AppNotification>> = runCatching {
        dao.getAll().map(NotificationEntity::toModel)
    }

    override fun observeNotifications(): Flow<List<AppNotification>> = dao.observeAll().map {
        it.map(NotificationEntity::toModel)
    }

    override fun observeUnreadCount(): Flow<Int> = dao.observeUnreadCount()

    override suspend fun markNotificationRead(id: String): Result<Unit> = runCatching {
        dao.markRead(id)
        syncDao.upsert(
            SyncMetadataEntity(
                entityType = "notification",
                entityId = id,
                state = SyncState.PENDING_UPLOAD,
                operation = SyncOperation.UPDATE,
                localUpdatedAt = System.currentTimeMillis()
            )
        )
        syncScheduler.enqueueSync()
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
