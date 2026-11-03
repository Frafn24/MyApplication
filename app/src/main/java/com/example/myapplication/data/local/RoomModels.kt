package com.example.myapplication.data.local

import androidx.room.Entity
import androidx.room.ColumnInfo
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: String,
    val name: String,
    val email: String,
    val phone: String,
    val notificationsEnabled: Boolean,
)

@Entity(tableName = "groups")
data class GroupEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String = "",
    val memberCount: Int,
    val balanceText: String,
)

@Entity(tableName = "group_members", primaryKeys = ["groupId", "userId"])
data class GroupMemberEntity(
    val groupId: String,
    val userId: String,
    val name: String,
    val isAdmin: Boolean,
)

@Entity(tableName = "expenses")
data class ExpenseEntity(
    @PrimaryKey val id: String,
    val groupId: String,
    val title: String,
    val amountInCents: Long,
    val paidBy: String,
    val splitBetween: String,
    @ColumnInfo(defaultValue = "'SYNCED'")
    val syncStatus: String = SyncStatus.SYNCED,
)

@Entity(tableName = "balances")
data class BalanceEntity(
    @PrimaryKey val id: String,
    val groupId: String,
    val groupName: String,
    val description: String,
    val amountDkk: Int,
    val isOwedToCurrentUser: Boolean,
)

@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val timeLabel: String,
)
