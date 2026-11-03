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

class InMemoryAuthRepository : AuthRepository {
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

class InMemoryGroupRepository : GroupRepository {
    private val groups = mutableListOf(
        GroupSummary("1", "Summer Trip", 4, "You are owed 250 DKK"),
        GroupSummary("2", "Roommates", 3, "You owe 100 DKK"),
    )

    private val members = mutableMapOf(
        "1" to mutableListOf(
            GroupMember("current-user", "You", isAdmin = true),
            GroupMember("2", "Alex"),
            GroupMember("3", "Sarah"),
            GroupMember("4", "John"),
        ),
    )

    override suspend fun getGroups(): Result<List<GroupSummary>> = Result.success(groups.toList())

    override suspend fun getMembers(groupId: String): Result<List<GroupMember>> =
        Result.success(members[groupId]?.toList().orEmpty())

    override suspend fun createGroup(title: String, description: String): Result<GroupSummary> {
        val group = GroupSummary(
            id = (groups.size + 1).toString(),
            name = title,
            memberCount = 1,
            balanceText = "No balance yet",
        )
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

class InMemoryExpenseRepository : ExpenseRepository {
    private val expenses = mutableListOf(
        Expense("1", "1", "Dinner", 600, "Alex", listOf("You", "Alex", "Sarah", "John")),
        Expense("2", "1", "Gas", 300, "You", listOf("You", "Alex", "Sarah", "John")),
        Expense("3", "1", "Groceries", 180, "Sarah", listOf("You", "Alex", "Sarah", "John")),
    )

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

}

class InMemoryBalanceRepository : BalanceRepository {
    override suspend fun getBalances(): Result<List<Balance>> = Result.success(
        listOf(
            Balance("1", "Summer Trip", "Alex owes you", 100, true),
            Balance("1", "Summer Trip", "Sarah owes you", 150, true),
            Balance("2", "Roommates", "You owe John", 100, false),
        ),
    )
}

class InMemoryNotificationRepository : NotificationRepository {
    override suspend fun getNotifications(): Result<List<AppNotification>> = Result.success(
        listOf(
            AppNotification("1", "New Expense", "Alex added Dinner - 600 DKK", "10 min ago"),
            AppNotification("2", "Payment Reminder", "You owe John 100 DKK", "Yesterday"),
            AppNotification("3", "Group Updated", "Sarah joined Summer Trip", "2 days ago"),
        ),
    )
}

class InMemoryProfileRepository : ProfileRepository {
    private var profile = User("current-user", "John Doe", "johndoe@test.com", "+45 12 34 56 78")

    override suspend fun getProfile(): Result<User> = Result.success(profile)

    override suspend fun updateProfile(user: User): Result<User> {
        profile = user
        return Result.success(profile)
    }
}
