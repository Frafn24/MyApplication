package com.example.myapplication.data.repository

import com.example.myapplication.data.model.GroupMember
import com.example.myapplication.data.model.GroupSummary
import com.example.myapplication.feature.groups.GroupDetails
import kotlinx.coroutines.flow.Flow

interface GroupRepository {
    suspend fun getGroups(): Result<List<GroupSummary>>
    suspend fun getDetails(groupId: String): Result<GroupDetails>
    suspend fun getMembers(groupId: String): Result<List<GroupMember>>
    suspend fun createGroup(
        title: String,
        description: String,
        invitedEmails: List<String> = emptyList()
    ): Result<GroupSummary>
    suspend fun inviteMember(groupId: String, email: String): Result<Unit>
    suspend fun removeMember(groupId: String, userId: String): Result<Unit>
    fun observeMembers(groupId: String): Flow<List<GroupMember>>
    suspend fun cancelInvite(groupId: String, email: String): Result<Unit>
}
