package com.example.myapplication.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: String,
    val name: String,
    val email: String,
    val phone: String,
    val notificationsEnabled: Boolean
)

@Entity(tableName = "groups")
data class GroupEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String = "",
    val memberCount: Int,
    val balanceText: String
)

@Entity(tableName = "group_members", primaryKeys = ["groupId", "userId"])
data class GroupMemberEntity(val groupId: String, val userId: String, val name: String, val isAdmin: Boolean)

@Entity(tableName = "expenses")
data class ExpenseEntity(
    @PrimaryKey val id: String,
    val groupId: String,
    val title: String,
    val amountInCents: Long,
    val paidBy: String,
    val splitBetween: String,
    @ColumnInfo(defaultValue = "''")
    val paidByName: String = "",
    val myShareInCents: Long? = null,
    @ColumnInfo(defaultValue = "''")
    val createdById: String = "",
    @ColumnInfo(defaultValue = "''")
    val splitAmounts: String = "",
    @ColumnInfo(defaultValue = "'SYNCED'")
    val syncStatus: String = SyncStatus.SYNCED
)

@Entity(tableName = "balances", primaryKeys = ["groupId", "userId"])
data class BalanceEntity(
    val groupId: String,
    val userId: String,
    val groupName: String,
    val personName: String,
    val amountCents: Long,
    val owedToMe: Boolean
)

@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey val id: String,
    val title: String,
    val message: String,
    @ColumnInfo(defaultValue = "0")
    val createdAtEpochMs: Long,
    @ColumnInfo(defaultValue = "1")
    val isUnread: Boolean,
    val groupId: String? = null,
    val expenseId: String? = null
)
