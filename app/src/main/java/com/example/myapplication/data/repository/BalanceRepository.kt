package com.example.myapplication.data.repository

import com.example.myapplication.data.model.Balance

interface BalanceRepository {
    suspend fun getBalances(): Result<List<Balance>>
}
