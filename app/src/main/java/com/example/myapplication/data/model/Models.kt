package com.example.myapplication.data.model

data class User(
    val id: String,
    val name: String,
    val email: String,
    val phone: String = "",
    val notificationsEnabled: Boolean = true
)

data class GroupSummary(val id: String, val name: String, val memberCount: Int, val balanceText: String)

data class GroupMember(val id: String, val name: String, val isAdmin: Boolean = false)

data class Expense(
    val id: String,
    val groupId: String,
    val title: String,
    val amountDkk: Int,
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

data class AppNotification(val id: String, val title: String, val description: String, val timeLabel: String)
