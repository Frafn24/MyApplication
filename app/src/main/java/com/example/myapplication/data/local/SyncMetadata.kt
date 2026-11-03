package com.example.myapplication.data.local

import androidx.room.Entity

/**
 * Local bookkeeping for future synchronization with a remote backend.
 * This table does not perform synchronization by itself.
 */
@Entity(tableName = "sync_metadata", primaryKeys = ["entityType", "entityId"])
data class SyncMetadataEntity(
    val entityType: String,
    val entityId: String,
    val remoteId: String? = null,
    val state: String = SyncState.SYNCED,
    val localUpdatedAt: Long = 0L,
    val serverUpdatedAt: Long? = null,
    val isDeleted: Boolean = false,
)

object SyncState {
    const val SYNCED = "synced"
    const val PENDING_UPLOAD = "pending_upload"
    const val PENDING_DELETE = "pending_delete"
    const val CONFLICT = "conflict"
}

object SyncStatus {
    const val PENDING = "PENDING"
    const val SYNCED = "SYNCED"
    const val FAILED = "FAILED"
}
