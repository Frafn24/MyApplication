package com.example.myapplication.data.sync

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

interface SyncScheduler {
    fun enqueueSync()
}

class WorkManagerSyncScheduler(context: Context) : SyncScheduler {
    private val workManager = WorkManager.getInstance(context.applicationContext)

    override fun enqueueSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val request = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.SECONDS)
            .build()

        workManager.enqueueUniqueWork(
            SYNC_WORK,
            ExistingWorkPolicy.KEEP,
            request
        )
    }

    /**
     * Keeps the cache fresh even when nothing local changed, so new group activity
     * shows up (badge, system notifications) without opening every screen. Call it
     * on every app start; KEEP ignores repeats.
     */
    fun enqueuePeriodicSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val request = PeriodicWorkRequestBuilder<SyncWorker>(15, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .build()

        workManager.enqueueUniquePeriodicWork(
            PERIODIC_SYNC_WORK,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    private companion object {
        const val SYNC_WORK = "app-sync"
        const val PERIODIC_SYNC_WORK = "app-sync-periodic"
    }
}

class NoOpSyncScheduler : SyncScheduler {
    override fun enqueueSync() = Unit
}
