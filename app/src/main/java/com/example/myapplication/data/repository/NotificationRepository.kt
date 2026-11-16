package com.example.myapplication.data.repository

import com.example.myapplication.data.model.AppNotification
import kotlinx.coroutines.flow.Flow

interface NotificationRepository {
    suspend fun getNotifications(): Result<List<AppNotification>>
    fun observeNotifications(): Flow<List<AppNotification>>
    suspend fun markNotificationRead(id: String): Result<Unit>
}
