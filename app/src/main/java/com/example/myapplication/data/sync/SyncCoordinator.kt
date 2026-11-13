package com.example.myapplication.data.sync

import com.example.myapplication.data.local.AppDatabase
import com.example.myapplication.data.local.GroupEntity
import com.example.myapplication.data.remote.AppRepository

class SyncCoordinator(private val database: AppDatabase) {
    suspend fun syncGroups(): Result<Unit> = runCatching {
        val remoteGroups = AppRepository.getGroups()

        remoteGroups.forEach { group ->
            database.groupDao().insert(
                GroupEntity(
                    id = group.id,
                    name = group.name,
                    memberCount = group.memberCount,
                    balanceText = group.balanceText
                )
            )
        }
    }
}
