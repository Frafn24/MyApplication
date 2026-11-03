package com.example.myapplication.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.migration.Migration
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        UserEntity::class,
        GroupEntity::class,
        GroupMemberEntity::class,
        ExpenseEntity::class,
        BalanceEntity::class,
        NotificationEntity::class,
        SyncMetadataEntity::class,
    ],
    version = 3,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun groupDao(): GroupDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun balanceDao(): BalanceDao
    abstract fun notificationDao(): NotificationDao
    abstract fun syncMetadataDao(): SyncMetadataDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "my_application.db",
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3).build().also { instance = it }
            }

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS sync_metadata (
                        entityType TEXT NOT NULL,
                        entityId TEXT NOT NULL,
                        remoteId TEXT,
                        state TEXT NOT NULL,
                        localUpdatedAt INTEGER NOT NULL,
                        serverUpdatedAt INTEGER,
                        isDeleted INTEGER NOT NULL,
                        PRIMARY KEY(entityType, entityId)
                    )
                    """.trimIndent(),
                )
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE expenses_new (
                        id TEXT NOT NULL,
                        groupId TEXT NOT NULL,
                        title TEXT NOT NULL,
                        amountInCents INTEGER NOT NULL,
                        paidBy TEXT NOT NULL,
                        splitBetween TEXT NOT NULL,
                        syncStatus TEXT NOT NULL DEFAULT 'SYNCED',
                        PRIMARY KEY(id)
                    )
                    """.trimIndent(),
                )
                database.execSQL(
                    """
                    INSERT INTO expenses_new (id, groupId, title, amountInCents, paidBy, splitBetween, syncStatus)
                    SELECT id, groupId, title, amountDkk * 100, paidBy, splitBetween, 'SYNCED'
                    FROM expenses
                    """.trimIndent(),
                )
                database.execSQL("DROP TABLE expenses")
                database.execSQL("ALTER TABLE expenses_new RENAME TO expenses")
            }
        }
    }
}
