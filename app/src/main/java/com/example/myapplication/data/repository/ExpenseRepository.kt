package com.example.myapplication.data.repository

import com.example.myapplication.data.model.Expense

interface ExpenseRepository {
    suspend fun getExpenses(groupId: String): Result<List<Expense>>
    suspend fun addExpense(expense: Expense): Result<Expense>
    suspend fun updateExpense(expense: Expense): Result<Expense>
}
