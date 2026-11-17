package com.example.myapplication.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration

@Database(
    entities = [
        UserEntity::class,
        GroupEntity::class,
        GroupMemberEntity::class,
        ExpenseEntity::class,
        BalanceEntity::class,
        NotificationEntity::class,
        SyncMetadataEntity::class
    ],
    version = 7,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun groupDao(): GroupDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun balanceDao(): BalanceDao
    abstract fun notificationDao(): NotificationDao
    abstract fun syncMetadataDao(): SyncMetadataDao

    /** Removes everything cached for the signed-in user, e.g. on sign-out. */
    suspend fun clearUserData() {
        syncMetadataDao().clear()
        expenseDao().clear()
        groupDao().clearMembers()
        groupDao().clear()
        userDao().clear()
        balanceDao().clear()
        notificationDao().clear()
    }

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "my_application.db"
            ).addMigrations(
                MIGRATION_1_2,
                MIGRATION_2_3,
                MIGRATION_3_4,
                MIGRATION_4_5,
                MIGRATION_5_6,
                MIGRATION_6_7
            ).build().also { instance = it }
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
                    """.trimIndent()
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
                    """.trimIndent()
                )
                database.execSQL(
                    """
                    INSERT INTO expenses_new (id, groupId, title, amountInCents, paidBy, splitBetween, syncStatus)
                    SELECT id, groupId, title, amountDkk * 100, paidBy, splitBetween, 'SYNCED'
                    FROM expenses
                    """.trimIndent()
                )
                database.execSQL("DROP TABLE expenses")
                database.execSQL("ALTER TABLE expenses_new RENAME TO expenses")
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
                database.execSQL(
                    """
            ALTER TABLE sync_metadata
            ADD COLUMN operation TEXT NOT NULL DEFAULT 'create'
                    """.trimIndent()
                )
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE expenses ADD COLUMN paidByName TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE expenses ADD COLUMN myShareInCents INTEGER")
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE expenses ADD COLUMN createdById TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE expenses ADD COLUMN splitAmounts TEXT NOT NULL DEFAULT ''")
            }
        }

        /** v6→v7: recreate balances and notifications tables with new schemas. */
        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                // balances: drop and recreate with composite key (groupId + userId)
                db.execSQL("DROP TABLE IF EXISTS balances")
                db.execSQL(
                    """
                    CREATE TABLE balances (
                        groupId TEXT NOT NULL,
                        userId TEXT NOT NULL,
                        groupName TEXT NOT NULL,
                        personName TEXT NOT NULL,
                        amountCents INTEGER NOT NULL,
                        owedToMe INTEGER NOT NULL,
                        PRIMARY KEY(groupId, userId)
                    )
                    """.trimIndent()
                )

                // notifications: recreate with new schema (SQLite doesn't support DROP COLUMN)
                db.execSQL(
                    """
                    CREATE TABLE notifications_new (
                        id TEXT NOT NULL PRIMARY KEY,
                        title TEXT NOT NULL,
                        message TEXT NOT NULL DEFAULT '',
                        createdAtEpochMs INTEGER NOT NULL DEFAULT 0,
                        isUnread INTEGER NOT NULL DEFAULT 1,
                        groupId TEXT,
                        expenseId TEXT
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT INTO notifications_new (id, title, message, createdAtEpochMs, isUnread, groupId, expenseId)
                    SELECT id, title, description, 0, 1, NULL, NULL FROM notifications
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE notifications")
                db.execSQL("ALTER TABLE notifications_new RENAME TO notifications")
            }
        }
    }
}
