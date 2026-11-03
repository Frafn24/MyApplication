package com.example.myapplication.data.repository

import com.example.myapplication.data.model.AppNotification
import com.example.myapplication.data.model.Balance
import com.example.myapplication.data.model.Expense
import com.example.myapplication.data.model.GroupMember
import com.example.myapplication.data.model.GroupSummary
import com.example.myapplication.data.model.User

interface AuthRepository {
    suspend fun login(email: String, password: String): Result<User>
    suspend fun register(name: String, email: String, password: String): Result<User>
    suspend fun sendPasswordReset(email: String): Result<Unit>
}

interface GroupRepository {
    suspend fun getGroups(): Result<List<GroupSummary>>
    suspend fun getMembers(groupId: String): Result<List<GroupMember>>
    suspend fun createGroup(title: String, description: String): Result<GroupSummary>
    suspend fun addMember(groupId: String, name: String): Result<GroupMember>
}

interface ExpenseRepository {
    suspend fun getExpenses(groupId: String): Result<List<Expense>>
    suspend fun addExpense(expense: Expense): Result<Expense>
    suspend fun updateExpense(expense: Expense): Result<Expense>
}

interface BalanceRepository {
    suspend fun getBalances(): Result<List<Balance>>
}

interface NotificationRepository {
    suspend fun getNotifications(): Result<List<AppNotification>>
}

interface ProfileRepository {
    suspend fun getProfile(): Result<User>
    suspend fun updateProfile(user: User): Result<User>
}
