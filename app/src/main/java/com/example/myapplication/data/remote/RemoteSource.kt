package com.example.myapplication.data.remote

import com.example.myapplication.data.model.GroupSummary
import com.example.myapplication.data.model.RemoteExpense
import com.example.myapplication.data.model.RemoteExpenseMeta
import com.example.myapplication.feature.balances.GroupBalances
import com.example.myapplication.feature.groups.GroupMember
import com.example.myapplication.feature.notifications.NotificationItem
import com.example.myapplication.feature.profile.UserProfile

/**
 * The read operations [com.example.myapplication.data.sync.SyncCoordinator] pulls from the
 * backend. [AppRepository] is the production implementation; tests substitute a fake to
 * exercise the sync logic without network or a real Supabase project.
 */
interface RemoteSource {
    suspend fun getGroups(): List<GroupSummary>

    suspend fun getGroupMembers(groupId: String): List<GroupMember>

    suspend fun getGroupExpenses(groupId: String): List<RemoteExpense>

    suspend fun getGroupExpenseMeta(groupId: String): Map<String, RemoteExpenseMeta>

    suspend fun getBalances(): Pair<String, List<GroupBalances>>

    suspend fun getProfile(): UserProfile

    suspend fun getNotifications(): List<NotificationItem>
}
