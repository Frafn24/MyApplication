package com.example.myapplication.data.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

/**
 * Skeleton for future Room-to-Supabase synchronization.
 * The remote upload is intentionally not implemented yet.
 */
class SyncExpensesWorker(
    appContext: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        // TODO: Load pending expenses from Room and upload them to Supabase.
        // Use Result.retry() for temporary network failures and mark permanent
        // failures as FAILED in the local database.
        return Result.success()
    }
}
