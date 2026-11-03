package com.example.myapplication.data.repository

import com.example.myapplication.data.model.User

interface ProfileRepository {
    suspend fun getProfile(): Result<User>
    suspend fun updateProfile(user: User): Result<User>
}
