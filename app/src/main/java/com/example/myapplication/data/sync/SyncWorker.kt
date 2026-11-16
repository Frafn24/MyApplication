package com.example.myapplication.data.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.myapplication.data.local.AppDatabase
import com.example.myapplication.data.local.ExpenseEntity
import com.example.myapplication.data.local.SyncMetadataEntity
import com.example.myapplication.data.local.SyncOperation
import com.example.myapplication.data.local.SyncState
import com.example.myapplication.data.local.SyncStatus
import com.example.myapplication.data.remote.AppRepository
import com.example.myapplication.feature.expenses.ExpenseDraft
import java.math.BigDecimal
import kotlinx.coroutines.CancellationException

class SyncWorker(appContext: Context, workerParams: WorkerParameters) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val database = AppDatabase.getInstance(applicationContext)

        return try {
            val touchedGroups = pendingGroupIds(database)
            uploadGroups(database)
            uploadInvites(database)
            uploadRemovals(database)
            uploadExpenseDeletes(database)
            uploadExpenses(database)
            uploadPaymentReminders(database)
            uploadNotificationReads(database)
            refresh(database, touchedGroups)
            Result.success()
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            Result.retry()
        }
    }

    /** Creates groups that were made offline and stores their server id. */
    private suspend fun uploadGroups(database: AppDatabase) {
        val syncDao = database.syncMetadataDao()
        val groupDao = database.groupDao()

        syncDao.getPending()
            .filter { it.entityType == "group" && it.operation == SyncOperation.CREATE }
            .forEach { metadata ->
                val group = groupDao.getById(metadata.entityId) ?: return@forEach

                runCatching {
                    AppRepository.createGroup(
                        name = group.name,
                        description = group.description,
                        members = emptyList()
                    )
                }.onSuccess { remoteId ->
                    syncDao.upsert(
                        metadata.copy(
                            state = SyncState.SYNCED,
                            remoteId = remoteId,
                            serverUpdatedAt = System.currentTimeMillis()
                        )
                    )
                }
                // On failure the group stays pending and is tried again next run.
            }
    }

    /** Adds invited members once their group has a server id. */
    private suspend fun uploadInvites(database: AppDatabase) {
        val syncDao = database.syncMetadataDao()

        syncDao.getPending()
            .filter { it.entityType == "member_invite" }
            .forEach { metadata ->
                val parts = metadata.entityId.split("|", limit = 2)
                if (parts.size != 2) {
                    syncDao.upsert(metadata.copy(state = SyncState.FAILED))
                    return@forEach
                }

                val (localGroupId, email) = parts
                val remoteGroupId = syncDao.get("group", localGroupId)?.remoteId ?: return@forEach

                runCatching { AppRepository.addGroupMember(remoteGroupId, email) }
                    .onSuccess {
                        syncDao.upsert(
                            metadata.copy(
                                state = SyncState.SYNCED,
                                serverUpdatedAt = System.currentTimeMillis()
                            )
                        )
                    }
                    .onFailure {
                        syncDao.upsert(
                            metadata.copy(
                                state = SyncState.FAILED,
                                serverUpdatedAt = System.currentTimeMillis()
                            )
                        )
                    }
            }
    }

    /** Removes members that were removed on this phone. */
    private suspend fun uploadRemovals(database: AppDatabase) {
        val syncDao = database.syncMetadataDao()

        syncDao.getPending()
            .filter { it.entityType == "member_remove" }
            .forEach { metadata ->
                val parts = metadata.entityId.split("|", limit = 2)
                if (parts.size != 2) return@forEach

                val (localGroupId, userId) = parts
                val remoteGroupId = syncDao.get("group", localGroupId)?.remoteId ?: return@forEach

                runCatching { AppRepository.removeGroupMember(remoteGroupId, userId) }
                    .onSuccess {
                        syncDao.upsert(
                            metadata.copy(
                                state = SyncState.SYNCED,
                                serverUpdatedAt = System.currentTimeMillis()
                            )
                        )
                    }
                // On failure it stays pending and is tried again next run.
            }
    }

    private suspend fun uploadExpenseDeletes(database: AppDatabase) {
        val expenseDao = database.expenseDao()
        val syncDao = database.syncMetadataDao()

        expenseDao.getDeleted().forEach { expense ->
            val metadata = syncDao.get("expense", expense.id) ?: return@forEach
            val remoteId = metadata.remoteId ?: return@forEach

            runCatching { AppRepository.deleteExpense(remoteId) }
                .onSuccess {
                    expenseDao.delete(expense)
                    syncDao.delete("expense", expense.id)
                }
            // On failure it stays queued and is retried next run.
        }
    }

    /** Uploads expenses that were created or edited on this phone. */
    private suspend fun uploadExpenses(database: AppDatabase) {
        val expenseDao = database.expenseDao()
        val syncDao = database.syncMetadataDao()

        expenseDao.getPending().forEach { expense ->
            runCatching {
                val metadata = syncDao.get("expense", expense.id)
                    ?: SyncMetadataEntity(
                        entityType = "expense",
                        entityId = expense.id,
                        state = SyncState.PENDING_UPLOAD,
                        operation = SyncOperation.CREATE,
                        localUpdatedAt = System.currentTimeMillis()
                    )

                // A group created offline gets its server id uploaded first.
                val remoteGroupId = syncDao.get("group", expense.groupId)?.remoteId
                    ?: expense.groupId

                val draft = expense.toDraft()

                when (metadata.operation) {
                    SyncOperation.CREATE -> {
                        val remoteId = AppRepository.createExpense(remoteGroupId, draft)

                        syncDao.upsert(
                            metadata.copy(
                                state = SyncState.SYNCED,
                                remoteId = remoteId,
                                serverUpdatedAt = System.currentTimeMillis()
                            )
                        )
                    }

                    SyncOperation.UPDATE -> {
                        val remoteId = metadata.remoteId
                            ?: error("Expense ${expense.id} has no server id yet")

                        AppRepository.updateExpense(remoteId, draft)

                        syncDao.upsert(
                            metadata.copy(
                                state = SyncState.SYNCED,
                                serverUpdatedAt = System.currentTimeMillis()
                            )
                        )
                    }

                    else -> error("Unsupported expense operation: ${metadata.operation}")
                }
            }.onSuccess {
                expenseDao.updateSyncStatus(expense.id, SyncStatus.SYNCED)
            }.onFailure {
                expenseDao.updateSyncStatus(expense.id, SyncStatus.FAILED)
            }
            // On failure the expense stays PENDING and is tried again next run.
        }
    }

    private fun ExpenseEntity.toDraft(): ExpenseDraft = ExpenseDraft(
        title = title,
        amount = BigDecimal.valueOf(amountInCents, 2),
        paidBy = paidBy,
        splitBetween = splitBetween
            .split("|")
            .filter(String::isNotBlank)
    )

    /** The local groups that have changes waiting, so they can be re-pulled after upload. */
    private suspend fun pendingGroupIds(database: AppDatabase): Set<String> {
        val syncDao = database.syncMetadataDao()
        val expenseDao = database.expenseDao()

        return syncDao.getPending().mapNotNull { metadata ->
            when (metadata.entityType) {
                "group" -> metadata.entityId
                "member_invite", "member_remove" -> metadata.entityId.substringBefore("|")
                "expense" -> expenseDao.getById(metadata.entityId)?.groupId
                else -> null
            }
        }.toSet()
    }

    /** Fills Room with what the server now has, so the screens update by themselves. */
    /** Sends queued payment reminders. */
    private suspend fun uploadPaymentReminders(database: AppDatabase) {
        val syncDao = database.syncMetadataDao()

        syncDao.getPending()
            .filter { it.entityType == "payment_reminder" }
            .forEach { metadata ->
                val parts = metadata.entityId.split("|", limit = 3)
                if (parts.size < 2) {
                    syncDao.upsert(metadata.copy(state = SyncState.FAILED))
                    return@forEach
                }

                val (groupId, userId) = parts
                val remoteGroupId = syncDao.get("group", groupId)?.remoteId ?: return@forEach

                runCatching { AppRepository.sendPaymentReminder(remoteGroupId, userId) }
                    .onSuccess {
                        syncDao.delete("payment_reminder", metadata.entityId)
                    }
                    .onFailure {
                        syncDao.upsert(
                            metadata.copy(
                                state = SyncState.FAILED,
                                serverUpdatedAt = System.currentTimeMillis()
                            )
                        )
                    }
            }
    }

    /** Marks notifications as read on the server. */
    private suspend fun uploadNotificationReads(database: AppDatabase) {
        val syncDao = database.syncMetadataDao()

        syncDao.getPending()
            .filter { it.entityType == "notification" }
            .forEach { metadata ->
                runCatching { AppRepository.markNotificationRead(metadata.entityId) }
                    .onSuccess {
                        syncDao.delete("notification", metadata.entityId)
                    }
                    .onFailure {
                        syncDao.upsert(
                            metadata.copy(
                                state = SyncState.FAILED,
                                serverUpdatedAt = System.currentTimeMillis()
                            )
                        )
                    }
            }
    }

    /** Fills Room with what the server now has, so the screens update by themselves. */
    private suspend fun refresh(database: AppDatabase, groupIds: Set<String>) {
        val coordinator = SyncCoordinator(database)
        coordinator.syncGroups()
        coordinator.syncBalances()
        groupIds.forEach { coordinator.syncGroup(it) }
    }
}
