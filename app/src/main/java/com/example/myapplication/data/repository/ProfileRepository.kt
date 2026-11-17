package com.example.myapplication.data.repository

import com.example.myapplication.data.model.User
import com.example.myapplication.feature.profile.UserProfile
import kotlinx.coroutines.flow.Flow

interface ProfileRepository {
    suspend fun getProfile(): Result<User>
    fun observeProfile(): Flow<UserProfile?>
    suspend fun updateProfile(user: User): Result<User>
}
