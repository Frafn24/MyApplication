package com.example.myapplication.data.repository

import com.example.myapplication.data.model.BalanceEntry
import com.example.myapplication.data.model.BalancesSummary

interface BalanceRepository {
    suspend fun getBalances(): Result<BalancesSummary>
    suspend fun sendPaymentReminder(entry: BalanceEntry): Result<Unit>
}
