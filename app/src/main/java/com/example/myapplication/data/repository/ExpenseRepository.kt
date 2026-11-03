package com.example.myapplication.data.repository

import com.example.myapplication.data.local.ExpenseEntity
import com.example.myapplication.data.model.Expense
import kotlinx.coroutines.flow.Flow

interface ExpenseRepository {
    suspend fun getExpenses(groupId: String): Result<List<Expense>>
    suspend fun addExpense(expense: Expense): Result<Expense>
    suspend fun updateExpense(expense: Expense): Result<Expense>
    fun observeExpenses(groupId: String): Flow<List<ExpenseEntity>>
    suspend fun addExpense(groupId: String, description: String, amountInCents: Long, payerId: String): Result<Expense>
    suspend fun retryExpense(expenseId: String): Result<Unit>
    suspend fun getPendingExpenses(): List<ExpenseEntity>
}
