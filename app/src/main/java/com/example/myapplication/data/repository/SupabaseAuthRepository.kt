package com.example.myapplication.data.repository

import com.example.myapplication.data.model.User
import com.example.myapplication.data.remote.AppRepository

class SupabaseAuthRepository : AuthRepository {
    override suspend fun isSignedIn(): Boolean = AppRepository.isSignedIn()

    override fun currentUserIdOrNull(): String? = AppRepository.currentUserIdOrNull()

    override suspend fun login(email: String, password: String): Result<User> = runCatching {
        AppRepository.signIn(email, password)
        User("current-user", email, email)
    }

    override suspend fun register(name: String, email: String, password: String): Result<User> = runCatching {
        val signedIn = AppRepository.signUp(name, email, password)

        if (!signedIn) {
            error("Confirm your email before signing in.")
        }

        User("current-user", name, email)
    }

    override suspend fun sendPasswordReset(email: String): Result<Unit> = runCatching {
        AppRepository.sendPasswordReset(email)
    }
}
