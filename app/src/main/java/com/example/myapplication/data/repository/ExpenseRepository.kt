package com.example.myapplication.data.repository

import com.example.myapplication.data.local.ExpenseEntity
import com.example.myapplication.data.model.Expense
import com.example.myapplication.feature.expenses.ExpenseDetails
import com.example.myapplication.feature.expenses.ExpenseEdit
import kotlinx.coroutines.flow.Flow

interface ExpenseRepository {
    suspend fun getExpenses(groupId: String): Result<List<Expense>>
    suspend fun addExpense(expense: Expense): Result<Expense>
    suspend fun updateExpense(expense: Expense): Result<Expense>
    fun observeExpenses(groupId: String): Flow<List<ExpenseEntity>>
    suspend fun addExpense(groupId: String, description: String, amountInCents: Long, payerId: String): Result<Expense>
    suspend fun retryExpense(expenseId: String): Result<Unit>
    suspend fun getPendingExpenses(): List<ExpenseEntity>
    suspend fun getExpenseDetails(expenseId: String): Result<ExpenseDetails>
    suspend fun getExpenseEdit(expenseId: String): Result<ExpenseEdit>
    suspend fun deleteExpense(expenseId: String): Result<Unit>
}
