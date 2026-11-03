package com.example.myapplication.data.sync

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

interface SyncScheduler {
    fun enqueueExpenseSync()
}

class WorkManagerSyncScheduler(context: Context) : SyncScheduler {
    private val workManager = WorkManager.getInstance(context.applicationContext)

    override fun enqueueExpenseSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val request = OneTimeWorkRequestBuilder<SyncExpensesWorker>()
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.SECONDS)
            .build()

        workManager.enqueueUniqueWork(
            EXPENSE_SYNC_WORK,
            ExistingWorkPolicy.KEEP,
            request,
        )
    }

    private companion object {
        const val EXPENSE_SYNC_WORK = "expense-sync"
    }
}

class NoOpSyncScheduler : SyncScheduler {
    override fun enqueueExpenseSync() = Unit
}
