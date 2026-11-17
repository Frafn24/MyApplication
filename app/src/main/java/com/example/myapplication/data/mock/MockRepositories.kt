package com.example.myapplication.data.mock

import com.example.myapplication.data.local.ExpenseEntity
import com.example.myapplication.data.model.AppNotification
import com.example.myapplication.data.model.BalanceEntry
import com.example.myapplication.data.model.BalancesSummary
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
import com.example.myapplication.feature.expenses.ExpenseDetails
import com.example.myapplication.feature.expenses.ExpenseDraft
import com.example.myapplication.feature.expenses.ExpenseEdit
import com.example.myapplication.feature.expenses.ExpenseMember
import com.example.myapplication.feature.expenses.ExpenseSplit
import com.example.myapplication.feature.expenses.SharedExpenses
import com.example.myapplication.feature.groups.GroupDetails
import com.example.myapplication.feature.groups.GroupExpenseItem
import com.example.myapplication.feature.profile.UserProfile
import java.math.BigDecimal
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf

/** Temporary mock repositories used until a real data source is connected. */
class MockAuthRepository : AuthRepository {
    private var signedIn = false

    override suspend fun isSignedIn(): Boolean = signedIn

    override fun currentUserIdOrNull(): String? = if (signedIn) "current-user" else null

    override suspend fun login(email: String, password: String): Result<User> =
        if (email.isNotBlank() && password.isNotBlank()) {
            signedIn = true
            Result.success(User("current-user", "John Doe", email))
        } else {
            Result.failure(IllegalArgumentException("Email and password are required"))
        }

    override suspend fun register(name: String, email: String, password: String): Result<User> =
        if (name.isNotBlank() && email.contains("@") && password.length >= 6) {
            signedIn = true
            Result.success(User("current-user", name, email))
        } else {
            Result.failure(IllegalArgumentException("Please check the registration details"))
        }

    override suspend fun sendPasswordReset(email: String): Result<Unit> = if (email.contains("@")) {
        Result.success(Unit)
    } else {
        Result.failure(IllegalArgumentException("Enter a valid email address"))
    }
}

class MockGroupRepository : GroupRepository {
    private val groups = MockData.groups.toMutableList()
    private val members = mutableMapOf("1" to MockData.summerTripMembers.toMutableList())

    override suspend fun getGroups(): Result<List<GroupSummary>> = Result.success(groups.toList())

    override suspend fun getDetails(groupId: String): Result<GroupDetails> = Result.success(
        GroupDetails(
            id = groupId,
            name = groups.firstOrNull { it.id == groupId }?.name.orEmpty(),
            description = "",
            balanceText = groups.firstOrNull { it.id == groupId }?.balanceText.orEmpty(),
            expenses = MockData.expenses
                .filter { it.groupId == groupId }
                .map {
                    GroupExpenseItem(
                        id = it.id,
                        title = it.title,
                        amountText = "${it.amountInCents / 100} DKK",
                        detailText = "${it.paidBy} paid"
                    )
                }
        )
    )

    override suspend fun getMembers(groupId: String): Result<List<GroupMember>> =
        Result.success(members[groupId]?.toList().orEmpty())

    override suspend fun createGroup(
        title: String,
        description: String,
        invitedEmails: List<String>
    ): Result<GroupSummary> {
        val group = GroupSummary((groups.size + 1).toString(), title, 1, "No balance yet")
        groups += group
        members[group.id] = mutableListOf(GroupMember("current-user", "You", isAdmin = true))
        return Result.success(group)
    }

    override suspend fun inviteMember(groupId: String, email: String): Result<Unit> {
        val member = GroupMember("member-${System.currentTimeMillis()}", email)
        members.getOrPut(groupId) { mutableListOf() }.add(member)
        return Result.success(Unit)
    }

    override suspend fun removeMember(groupId: String, userId: String): Result<Unit> {
        members[groupId]?.removeAll { it.id == userId }
        return Result.success(Unit)
    }

    override fun observeMembers(groupId: String): Flow<List<GroupMember>> = flowOf(members[groupId]?.toList().orEmpty())
    override suspend fun cancelInvite(groupId: String, email: String): Result<Unit> {
        members[groupId]?.removeAll { it.name == email }
        return Result.success(Unit)
    }

    override fun observeDetails(groupId: String): Flow<GroupDetails?> = flow {
        emit(getDetails(groupId).getOrNull())
    }
}

class MockExpenseRepository : ExpenseRepository {
    private val expenses = MockData.expenses.toMutableList()

    override suspend fun getExpenses(groupId: String): Result<List<Expense>> =
        Result.success(expenses.filter { it.groupId == groupId })

    override suspend fun getExpenseDetails(expenseId: String): Result<ExpenseDetails> {
        val expense = expenses.firstOrNull { it.id == expenseId }
            ?: return Result.failure(IllegalStateException("Expense not found"))

        val splits = if (expense.splitBetween.isEmpty()) {
            emptyList()
        } else {
            val cents = expense.amountInCents / expense.splitBetween.size
            expense.splitBetween.map { ExpenseSplit(it, BigDecimal.valueOf(cents, 2)) }
        }

        return Result.success(
            ExpenseDetails(
                title = expense.title,
                amount = BigDecimal.valueOf(expense.amountInCents, 2),
                paidBy = expense.paidBy,
                splits = splits,
                canEdit = true,
                canDelete = true
            )
        )
    }

    override suspend fun getExpenseEdit(expenseId: String): Result<ExpenseEdit> {
        val expense = expenses.firstOrNull { it.id == expenseId }
            ?: return Result.failure(IllegalStateException("Expense not found"))

        return Result.success(
            ExpenseEdit(
                groupId = expense.groupId,
                draft = ExpenseDraft(
                    title = expense.title,
                    amount = BigDecimal.valueOf(expense.amountInCents, 2),
                    paidBy = expense.paidBy,
                    splitBetween = expense.splitBetween
                ),
                members = expense.splitBetween.map { ExpenseMember(it, it) }
            )
        )
    }

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

    override fun observeSharedExpenses(groupId: String, userId: String): Flow<SharedExpenses?> =
        kotlinx.coroutines.flow.flowOf(
            SharedExpenses(
                title = "You and Alex · Summer Trip",
                totalText = "Alex owes you 150 DKK",
                expenses = emptyList()
            )
        )

    override suspend fun addExpense(
        groupId: String,
        description: String,
        amountInCents: Long,
        payerId: String
    ): Result<Expense> = addExpense(
        Expense("expense-${System.currentTimeMillis()}", groupId, description, amountInCents, payerId, listOf(payerId))
    )

    override suspend fun retryExpense(expenseId: String): Result<Unit> = Result.success(Unit)

    override suspend fun getPendingExpenses(): List<ExpenseEntity> = emptyList()

    override suspend fun deleteExpense(expenseId: String): Result<Unit> {
        expenses.removeAll { it.id == expenseId }
        return Result.success(Unit)
    }
}

class MockBalanceRepository : BalanceRepository {
    override suspend fun getBalances() = Result.success(MockData.balancesSummary)

    override fun observeBalances(): Flow<BalancesSummary> = kotlinx.coroutines.flow.flowOf(MockData.balancesSummary)

    override suspend fun sendPaymentReminder(entry: BalanceEntry): Result<Unit> = Result.success(Unit)
}

class MockNotificationRepository : NotificationRepository {
    override suspend fun getNotifications() = Result.success(MockData.notifications)

    override fun observeNotifications(): Flow<List<AppNotification>> =
        kotlinx.coroutines.flow.flowOf(MockData.notifications)

    override fun observeUnreadCount(): Flow<Int> = kotlinx.coroutines.flow.flowOf(2)

    override suspend fun markNotificationRead(id: String): Result<Unit> = Result.success(Unit)
}

class MockProfileRepository : ProfileRepository {
    private var profile = User("current-user", "John Doe", "johndoe@test.com", "+45 12 34 56 78")

    override suspend fun getProfile(): Result<User> = Result.success(profile)

    override fun observeProfile(): Flow<UserProfile?> = kotlinx.coroutines.flow.flowOf(
        UserProfile("John Doe", "johndoe@test.com", "+45 12 34 56 78", true)
    )

    override suspend fun updateProfile(user: User): Result<User> {
        profile = user
        return Result.success(profile)
    }
}
