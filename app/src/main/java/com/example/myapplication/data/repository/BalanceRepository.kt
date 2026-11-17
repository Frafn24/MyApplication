package com.example.myapplication.data.repository

import com.example.myapplication.data.model.BalanceEntry
import com.example.myapplication.data.model.BalancesSummary
import kotlinx.coroutines.flow.Flow

interface BalanceRepository {
    suspend fun getBalances(): Result<BalancesSummary>
    fun observeBalances(): Flow<BalancesSummary>
    suspend fun sendPaymentReminder(entry: BalanceEntry): Result<Unit>
}
