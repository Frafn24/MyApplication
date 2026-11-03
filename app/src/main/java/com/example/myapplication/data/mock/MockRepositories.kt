package com.example.myapplication.data.mock

import com.example.myapplication.data.local.ExpenseEntity
import com.example.myapplication.data.model.Expense
import com.example.myapplication.data.model.GroupMember
import com.example.myapplication.data.model.GroupSummary
import com.example.myapplication.data.model.User
import com.example.myapplication.data.repository.AuthRepository
import com.example.myapplication.data.repository.BalanceRepository
import com.example.myapplication.data.repository.ExpenseRepository
import com.example.myapplication.data.repository.GroupRepository
import com.example.myapplication.data.repository.NotificationRepository
import com.example.myapplication.data.repository.ProfileRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/** Temporary mock repositories used until a real data source is connected. */
class MockAuthRepository : AuthRepository {
    override suspend fun login(email: String, password: String): Result<User> =
        if (email.isNotBlank() && password.isNotBlank()) {
            Result.success(User("current-user", "John Doe", email))
        } else {
            Result.failure(IllegalArgumentException("Email and password are required"))
        }

    override suspend fun register(name: String, email: String, password: String): Result<User> =
        if (name.isNotBlank() && email.contains("@") && password.length >= 6) {
            Result.success(User("current-user", name, email))
        } else {
            Result.failure(IllegalArgumentException("Please check the registration details"))
        }

    override suspend fun sendPasswordReset(email: String): Result<Unit> =
        if (email.contains("@")) Result.success(Unit)
        else Result.failure(IllegalArgumentException("Enter a valid email address"))
}

class MockGroupRepository : GroupRepository {
    private val groups = MockData.groups.toMutableList()
    private val members = mutableMapOf("1" to MockData.summerTripMembers.toMutableList())

    override suspend fun getGroups(): Result<List<GroupSummary>> = Result.success(groups.toList())

    override suspend fun getMembers(groupId: String): Result<List<GroupMember>> =
        Result.success(members[groupId]?.toList().orEmpty())

    override suspend fun createGroup(title: String, description: String): Result<GroupSummary> {
        val group = GroupSummary((groups.size + 1).toString(), title, 1, "No balance yet")
        groups += group
        members[group.id] = mutableListOf(GroupMember("current-user", "You", isAdmin = true))
        return Result.success(group)
    }

    override suspend fun addMember(groupId: String, name: String): Result<GroupMember> {
        val member = GroupMember("member-${System.currentTimeMillis()}", name)
        members.getOrPut(groupId) { mutableListOf() }.add(member)
        return Result.success(member)
    }
}

class MockExpenseRepository : ExpenseRepository {
    private val expenses = MockData.expenses.toMutableList()

    override suspend fun getExpenses(groupId: String): Result<List<Expense>> =
        Result.success(expenses.filter { it.groupId == groupId })

    override suspend fun addExpense(expense: Expense): Result<Expense> {
        expenses += expense
        return Result.success(expense)
    }

    override suspend fun updateExpense(expense: Expense): Result<Expense> {
        val index = expenses.indexOfFirst { it.id == expense.id }
        if (index >= 0) expenses[index] = expense
        return Result.success(expense)
    }

    override fun observeExpenses(groupId: String): Flow<List<ExpenseEntity>> = flowOf(emptyList())

    override suspend fun addExpense(
        groupId: String,
        description: String,
        amountInCents: Long,
        payerId: String,
    ): Result<Expense> = addExpense(
        Expense("expense-${System.currentTimeMillis()}", groupId, description, amountInCents, payerId, listOf(payerId)),
    )

    override suspend fun retryExpense(expenseId: String): Result<Unit> = Result.success(Unit)

    override suspend fun getPendingExpenses(): List<ExpenseEntity> = emptyList()
}

class MockBalanceRepository : BalanceRepository {
    override suspend fun getBalances() = Result.success(MockData.balances)
}

class MockNotificationRepository : NotificationRepository {
    override suspend fun getNotifications() = Result.success(MockData.notifications)
}

class MockProfileRepository : ProfileRepository {
    private var profile = User("current-user", "John Doe", "johndoe@test.com", "+45 12 34 56 78")

    override suspend fun getProfile(): Result<User> = Result.success(profile)

    override suspend fun updateProfile(user: User): Result<User> {
        profile = user
        return Result.success(profile)
    }
}
