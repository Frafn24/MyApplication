package com.example.myapplication.data.repository

import com.example.myapplication.data.model.User

interface AuthRepository {
    suspend fun login(email: String, password: String): Result<User>
    suspend fun register(name: String, email: String, password: String): Result<User>
    suspend fun sendPasswordReset(email: String): Result<Unit>
}
