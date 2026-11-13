package com.example.myapplication.data.repository

import com.example.myapplication.data.model.GroupMember
import com.example.myapplication.data.model.GroupSummary

interface GroupRepository {
    suspend fun getGroups(): Result<List<GroupSummary>>
    suspend fun getMembers(groupId: String): Result<List<GroupMember>>
    suspend fun createGroup(title: String, description: String): Result<GroupSummary>
    suspend fun addMember(groupId: String, name: String): Result<GroupMember>
}
