package com.example.myapplication.data.model

data class User(
    val id: String,
    val name: String,
    val email: String,
    val phone: String = "",
    val notificationsEnabled: Boolean = true
)

data class GroupSummary(val id: String, val name: String, val memberCount: Int, val balanceText: String)

data class GroupMember(
    val id: String,
    val name: String,
    val isAdmin: Boolean = false,
    val isPending: Boolean = false,
    val isFailed: Boolean = false
)

data class Expense(
    val id: String,
    val groupId: String,
    val title: String,
    val amountInCents: Long,
    val paidBy: String,
    val splitBetween: List<String>
)

data class Balance(
    val groupId: String,
    val groupName: String,
    val description: String,
    val amountDkk: Int,
    val isOwedToCurrentUser: Boolean
)

data class RemoteExpense(
    val id: String,
    val description: String,
    val amountInCents: Long,
    val paidBy: String,
    val paidByName: String,
    val myShareInCents: Long
)

data class AppNotification(
    val id: String,
    val title: String,
    val message: String,
    val timeLabel: String,
    val isUnread: Boolean = false,
    val groupId: String? = null,
    val expenseId: String? = null
)

data class RemoteExpenseMeta(val createdBy: String, val splits: Map<String, Long>)

/** Aggregated balances for the Balances screen. */
data class BalancesSummary(val totalText: String, val groups: List<GroupBalances>)

/** One group's balance entries. */
data class GroupBalances(val groupName: String, val entries: List<BalanceEntry>)

/** One person in a group's balance. */
data class BalanceEntry(
    val id: String, // "<groupId>/<userId>"
    val personName: String,
    val amountText: String, // "150 DKK"
    val direction: BalanceDirection,
    // A reminder for this person is waiting to be sent.
    val isPending: Boolean = false,
    val isFailed: Boolean = false
)

enum class BalanceDirection { OwedToYou, YouOwe }
