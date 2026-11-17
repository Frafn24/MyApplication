package com.example.myapplication.data.local

import android.content.Context

/**
 * Clears the local cache when a different account signs in, so one user's
 * groups and expenses are never shown to another.
 */
class LocalUserData(private val database: AppDatabase, context: Context) {
    private val prefs = context.getSharedPreferences("local_user", Context.MODE_PRIVATE)

    suspend fun clear() {
        database.clearUserData()
        prefs.edit().remove(KEY_OWNER).apply()
    }

    /**
     * Wipes the cache when the signed-in user differs from the one it was
     * filled for. Also runs once after an upgrade, where no owner is stored yet.
     */
    suspend fun ensureOwner(userId: String?) {
        if (userId == null) return
        if (prefs.getString(KEY_OWNER, null) != userId) {
            database.clearUserData()
            prefs.edit().putString(KEY_OWNER, userId).apply()
        }
    }

    private companion object {
        const val KEY_OWNER = "owner_id"
    }
}
