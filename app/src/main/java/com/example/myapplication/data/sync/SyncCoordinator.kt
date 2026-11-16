package com.example.myapplication.data.sync

import com.example.myapplication.data.local.AppDatabase
import com.example.myapplication.data.local.BalanceEntity
import com.example.myapplication.data.local.ExpenseEntity
import com.example.myapplication.data.local.GroupEntity
import com.example.myapplication.data.local.GroupMemberEntity
import com.example.myapplication.data.local.NotificationEntity
import com.example.myapplication.data.local.SyncMetadataEntity
import com.example.myapplication.data.local.SyncState
import com.example.myapplication.data.local.SyncStatus
import com.example.myapplication.data.remote.AppRepository
import com.example.myapplication.data.sync.encodeSplits

class SyncCoordinator(private val database: AppDatabase) {
    /** Pulls the group list into Room. */
    suspend fun syncGroups(): Result<Unit> = runCatching {
        val remoteGroups = AppRepository.getGroups()
        val groupDao = database.groupDao()
        val syncDao = database.syncMetadataDao()
        val expenseDao = database.expenseDao()

        remoteGroups.forEach { group ->
            // A group created locally is stored under "group-..." with the server
            // id in remoteId, so reuse that row instead of inserting a duplicate.
            val metadata = syncDao.getByRemoteId("group", group.id)
            val localId = metadata?.entityId ?: group.id

            groupDao.insert(
                GroupEntity(
                    id = localId,
                    name = group.name,
                    memberCount = group.memberCount,
                    balanceText = group.balanceText
                )
            )

            syncDao.upsert(
                (
                    metadata ?: SyncMetadataEntity(
                        entityType = "group",
                        entityId = localId,
                        remoteId = group.id
                    )
                    ).copy(
                    state = SyncState.SYNCED,
                    serverUpdatedAt = System.currentTimeMillis()
                )
            )
        }

        // Groups the user is no longer a member of must disappear from the cache.
        val remoteIds = remoteGroups.map { it.id }.toSet()
        groupDao.getAll().forEach { local ->
            val metadata = syncDao.get("group", local.id) ?: return@forEach
            val remoteId = metadata.remoteId ?: return@forEach

            if (metadata.state == SyncState.SYNCED && remoteId !in remoteIds) {
                // Includes rows waiting to be deleted, so their metadata is cleaned up too.
                expenseDao.getAllForGroup(local.id).forEach { expense ->
                    syncDao.delete("expense", expense.id)
                }
                expenseDao.deleteForGroup(local.id)
                groupDao.deleteMembersForGroup(local.id)
                groupDao.delete(local.id)
                syncDao.delete("group", local.id)
            }
        }

        // Removes groups cached before remote ids were tracked.
        groupDao.deleteUnlinkedLocalGroups()
    }

    /** Pulls members and expenses for one group into Room. */
    suspend fun syncGroup(groupId: String): Result<Unit> = runCatching {
        val groupDao = database.groupDao()
        val expenseDao = database.expenseDao()
        val syncDao = database.syncMetadataDao()

        val remoteGroupId = syncDao.get("group", groupId)?.remoteId ?: groupId
        // Members removed offline must not reappear when the list is pulled.
        val pendingRemovals = syncDao.getPending()
            .mapNotNull { metadata ->
                if (metadata.entityType != "member_remove") return@mapNotNull null
                val parts = metadata.entityId.split("|", limit = 2)
                if (parts.size == 2 && parts[0] == groupId) parts[1] else null
            }
            .toSet()

        // Members: the server is the source of truth for the cached list.
        val members = AppRepository.getGroupMembers(remoteGroupId)
            .filterNot { it.id in pendingRemovals }

        members.forEach { member ->
            groupDao.insertMember(
                GroupMemberEntity(
                    groupId = groupId,
                    userId = member.id,
                    name = member.name,
                    isAdmin = member.isAdmin
                )
            )
        }

        // Remove members that are gone on the server, without emptying the
        // table first (which would flicker the list on screen).
        val keepIds = members.map { it.id }.toSet()
        groupDao.getMembers(groupId)
            .filterNot { it.userId in keepIds }
            .forEach { groupDao.deleteMember(groupId, it.userId) }
        // Expenses.
        val expenseMeta = runCatching { AppRepository.getGroupExpenseMeta(remoteGroupId) }
            .getOrDefault(emptyMap())
        val remoteExpenses = AppRepository.getGroupExpenses(remoteGroupId)
        remoteExpenses.forEach { remote ->
            val metadata = syncDao.getByRemoteId("expense", remote.id)

            // A local change waiting for upload must not be overwritten.
            if (metadata != null && metadata.state != SyncState.SYNCED) return@forEach

            val localId = metadata?.entityId ?: remote.id
            val splits = expenseMeta[remote.id]?.splits.orEmpty()

            expenseDao.insert(
                ExpenseEntity(
                    id = localId,
                    groupId = groupId,
                    title = remote.description,
                    amountInCents = remote.amountInCents,
                    paidBy = remote.paidBy,
                    splitBetween = splits.keys.joinToString("|"),
                    paidByName = remote.paidByName,
                    myShareInCents = remote.myShareInCents,
                    createdById = expenseMeta[remote.id]?.createdBy.orEmpty(),
                    splitAmounts = encodeSplits(splits),
                    syncStatus = SyncStatus.SYNCED
                )
            )

            syncDao.upsert(
                (
                    metadata ?: SyncMetadataEntity(
                        entityType = "expense",
                        entityId = localId,
                        remoteId = remote.id
                    )
                    ).copy(
                    state = SyncState.SYNCED,
                    serverUpdatedAt = System.currentTimeMillis()
                )
            )
        }

        // Expenses deleted on the server disappear locally too.
        val remoteIds = remoteExpenses.map { it.id }.toSet()
        expenseDao.getForGroup(groupId).forEach { local ->
            val metadata = syncDao.get("expense", local.id) ?: return@forEach
            val remoteId = metadata.remoteId ?: return@forEach

            if (metadata.state == SyncState.SYNCED && remoteId !in remoteIds) {
                expenseDao.delete(local)
                syncDao.delete("expense", local.id)
            }
        }
    }

    /** Best-effort refresh using the server's group id, e.g. from a cached balance row. */
    suspend fun syncGroupByRemoteId(remoteGroupId: String): Result<Unit> {
        val localId = database.syncMetadataDao().getByRemoteId("group", remoteGroupId)?.entityId ?: remoteGroupId
        return syncGroup(localId)
    }

    /** Pulls balances into Room. */
    suspend fun syncBalances(): Result<Unit> = runCatching {
        val (totalText, groupBalances) = AppRepository.getBalances()
        val balanceDao = database.balanceDao()

        // Flatten GroupBalances into BalanceEntity rows.
        val entities = groupBalances.flatMap { gb ->
            gb.entries.map { entry ->
                BalanceEntity(
                    groupId = entry.id.substringBefore("/"),
                    userId = entry.id.substringAfter("/"),
                    groupName = gb.groupName,
                    personName = entry.personName,
                    amountCents = (entry.amountText.replace(" DKK", "").replace(",", "").toDouble() * 100).toLong(),
                    owedToMe = entry.direction.name == "OwedToYou"
                )
            }
        }

        balanceDao.clear()
        if (entities.isNotEmpty()) balanceDao.insertAll(entities)
    }

    /** Pulls notifications into Room. Returns the ones that arrived with this pull. */
    suspend fun syncNotifications(): Result<List<NotificationEntity>> = runCatching {
        val remote = AppRepository.getNotifications()
        val notificationDao = database.notificationDao()
        val syncDao = database.syncMetadataDao()

        // Mark-as-read changes still waiting to be uploaded must not be undone by the pull.
        val readPendingIds = syncDao.getPending()
            .filter { it.entityType == "notification" }
            .map { it.entityId }
            .toSet()

        val entities = remote.map { n ->
            NotificationEntity(
                id = n.id,
                title = n.title,
                message = n.message,
                createdAtEpochMs = n.createdAtEpochMs,
                isUnread = n.isUnread && n.id !in readPendingIds,
                groupId = n.groupId,
                expenseId = n.expenseId
            )
        }

        // The very first pull is old news, so nothing counts as new then.
        val previousIds = notificationDao.getAll().map { it.id }.toSet()
        val newlyArrived = if (previousIds.isEmpty()) {
            emptyList()
        } else {
            entities.filter { it.id !in previousIds && it.isUnread }
        }

        notificationDao.clear()
        if (entities.isNotEmpty()) notificationDao.insertAll(entities)

        newlyArrived
    }
}
