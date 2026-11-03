package com.example.myapplication.data.repository

import com.example.myapplication.data.model.AppNotification

interface NotificationRepository {
    suspend fun getNotifications(): Result<List<AppNotification>>
}
