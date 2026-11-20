package com.example.myapplication.data.sync

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.myapplication.data.local.AppDatabase
import com.example.myapplication.data.local.ExpenseEntity
import com.example.myapplication.data.local.GroupEntity
import com.example.myapplication.data.local.SyncMetadataEntity
import com.example.myapplication.data.local.SyncOperation
import com.example.myapplication.data.local.SyncState
import com.example.myapplication.data.local.SyncStatus
import com.example.myapplication.data.model.GroupSummary
import com.example.myapplication.data.model.RemoteExpense
import com.example.myapplication.data.model.RemoteExpenseMeta
import com.example.myapplication.data.remote.RemoteSource
import com.example.myapplication.feature.balances.GroupBalances
import com.example.myapplication.feature.groups.GroupMember
import com.example.myapplication.feature.notifications.NotificationItem
import com.example.myapplication.feature.profile.UserProfile
import java.io.IOException
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Reliability QA: instrumented tests of the offline-first sync, using an
 * in-memory Room database and a fake [RemoteSource] that can simulate the
 * network being unavailable.
 *
 * Covers the three requirements from the project description:
 *  1. Offline availability — previously retrieved data stays accessible
 *     when a request fails because the device is offline.
 *  2. Fault tolerance — a failed request surfaces as a failed [Result] and
 *     never throws into the app; the local cache is left untouched.
 *  3. Fault tolerance / conflict protection — a local change waiting for
 *     upload is never overwritten by a background pull.
 */
@RunWith(AndroidJUnit4::class)
class SyncCoordinatorTest {

    private lateinit var database: AppDatabase
    private lateinit var remote: FakeRemoteSource
    private lateinit var coordinator: SyncCoordinator

    @Before
    fun setUp() = runBlocking {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()

        remote = FakeRemoteSource()
        coordinator = SyncCoordinator(database, currentUserId = { USER_ID }, remote = remote)
    }

    @After
    fun tearDown() {
        database.close()
    }

    // --- Offline availability ---

    @Test
    fun previouslyPulledDataStaysAccessibleWhenOffline() = runBlocking {
        // Arrange: a successful pull fills the cache.
        coordinator.syncGroups().getOrThrow()
        assertEquals(1, database.groupDao().getAll().size)

        // Act: the network goes down and another pull is attempted.
        remote.offline = true

        val result = coordinator.syncGroups()

        // Assert: the pull fails gracefully...
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IOException)

        // ...and the cached data is still readable for the screens.
        val groups = database.groupDao().getAll()
        assertEquals(1, groups.size)
        assertEquals("Flat 14", groups.single().name)
    }

    @Test
    fun offlinePullLeavesPendingLocalChangesInPlace() = runBlocking {
        // Arrange: a local group edit waiting for upload (the outbox pattern).
        database.groupDao().insert(GROUP_LOCAL)
        database.syncMetadataDao().upsert(
            SyncMetadataEntity(
                entityType = "group",
                entityId = GROUP_LOCAL_ID,
                state = SyncState.PENDING_UPLOAD,
                operation = SyncOperation.UPDATE
            )
        )

        // Act: pull while offline fails...
        remote.offline = true
        val result = coordinator.syncGroups()

        // Assert: ...but the pending local change is untouched.
        assertTrue(result.isFailure)
        assertEquals(GROUP_LOCAL, database.groupDao().getById(GROUP_LOCAL_ID))
        val metadata = database.syncMetadataDao().get("group", GROUP_LOCAL_ID)
        assertNotNull(metadata)
        assertEquals(SyncState.PENDING_UPLOAD, metadata?.state)
    }

    // --- Fault tolerance: conflict protection during pull ---

    @Test
    fun pullDoesNotOverwritePendingLocalExpense() = runBlocking {
        // Arrange: an expense created offline, waiting for upload.
        database.groupDao().insert(GROUP_SYNCED)
        database.syncMetadataDao().upsert(
            SyncMetadataEntity(
                entityType = "group",
                entityId = GROUP_ID,
                remoteId = GROUP_ID,
                state = SyncState.SYNCED
            )
        )
        database.expenseDao().insert(EXPENSE_PENDING)
        database.syncMetadataDao().upsert(
            SyncMetadataEntity(
                entityType = "expense",
                entityId = EXPENSE_PENDING_ID,
                remoteId = EXPENSE_PENDING_ID,
                state = SyncState.PENDING_UPLOAD,
                operation = SyncOperation.CREATE
            )
        )

        // The server now has a *different* version of that same expense.
        remote.expenses = listOf(
            RemoteExpense(
                id = EXPENSE_PENDING_ID,
                description = "Server version (should be ignored)",
                amountInCents = 9_900_00,
                paidBy = USER_ID,
                paidByName = "You",
                myShareInCents = 4_950_00
            )
        )

        // Act: pull the group while the local change is still pending.
        coordinator.syncGroup(GROUP_ID).getOrThrow()

        // Assert: the local pending change was NOT overwritten.
        val cached = database.expenseDao().getById(EXPENSE_PENDING_ID)
        assertNotNull(cached)
        assertEquals("Pizza Friday", cached?.title)
        assertEquals(2_400_00L, cached?.amountInCents)
        assertEquals(SyncStatus.PENDING, cached?.syncStatus)
    }

    @Test
    fun pullStoresServerExpenseAndDropsRemoteDeleted() = runBlocking {
        // Arrange: one synced expense cached, the server no longer has it.
        database.groupDao().insert(GROUP_SYNCED)
        database.syncMetadataDao().upsert(
            SyncMetadataEntity(
                entityType = "group",
                entityId = GROUP_ID,
                remoteId = GROUP_ID,
                state = SyncState.SYNCED
            )
        )
        database.expenseDao().insert(EXPENSE_SYNCED)
        database.syncMetadataDao().upsert(
            SyncMetadataEntity(
                entityType = "expense",
                entityId = EXPENSE_SYNCED_ID,
                remoteId = EXPENSE_SYNCED_ID,
                state = SyncState.SYNCED
            )
        )

        // Act: the server has a new expense and not the old one anymore.
        remote.expenses = listOf(REMOTE_EXPENSE)
        coordinator.syncGroup(GROUP_ID).getOrThrow()

        // Assert: the new expense is cached with its remote id...
        val expenses = database.expenseDao().getForGroup(GROUP_ID)
        assertEquals(1, expenses.size)
        assertEquals("Taxi to the airport", expenses.single().title)

        // ...and the expense deleted on the server is gone locally too,
        // without touching anything else in the cache.
        assertEquals(null, database.expenseDao().getById(EXPENSE_SYNCED_ID))
    }

    // --- Test double ---

    /** In-memory stand-in for the Supabase backend. [offline] makes every call fail. */
    private class FakeRemoteSource : RemoteSource {
        var offline = false
        var groups = listOf(GROUP_SUMMARY)
        var expenses = emptyList<RemoteExpense>()

        private fun checkOnline() {
            if (offline) throw IOException("No connection. Check your internet and try again.")
        }

        override suspend fun getGroups(): List<GroupSummary> {
            checkOnline()
            return groups
        }

        override suspend fun getGroupMembers(groupId: String): List<GroupMember> {
            checkOnline()
            return listOf(GroupMember(id = USER_ID, name = "Alice", isAdmin = true, isMe = true))
        }

        override suspend fun getGroupExpenses(groupId: String): List<RemoteExpense> {
            checkOnline()
            return expenses
        }

        override suspend fun getGroupExpenseMeta(groupId: String): Map<String, RemoteExpenseMeta> {
            checkOnline()
            return emptyMap()
        }

        override suspend fun getBalances(): Pair<String, List<GroupBalances>> {
            checkOnline()
            return "" to emptyList()
        }

        override suspend fun getProfile(): UserProfile {
            checkOnline()
            return UserProfile(name = "Alice", email = "alice@example.com", phone = "", notificationsEnabled = true)
        }

        override suspend fun getNotifications(): List<NotificationItem> {
            checkOnline()
            return emptyList()
        }
    }

    private companion object {
        const val USER_ID = "user-1"
        const val GROUP_ID = "group-remote-1"
        const val GROUP_LOCAL_ID = "group-local-1"
        const val EXPENSE_PENDING_ID = "expense-local-1"
        const val EXPENSE_SYNCED_ID = "expense-remote-1"

        val GROUP_SUMMARY =
            GroupSummary(
                id = GROUP_ID,
                name = "Flat 14",
                memberCount = 3,
                balanceText = "All settled up"
            )

        val GROUP_SYNCED =
            GroupEntity(
                id = GROUP_ID,
                name = "Flat 14",
                memberCount = 3,
                balanceText = "All settled up"
            )

        val GROUP_LOCAL =
            GroupEntity(
                id = GROUP_LOCAL_ID,
                name = "Flat 14 (renamed)",
                memberCount = 3,
                balanceText = "All settled up"
            )

        val EXPENSE_PENDING = ExpenseEntity(
            id = EXPENSE_PENDING_ID,
            groupId = GROUP_ID,
            title = "Pizza Friday",
            amountInCents = 2_400_00,
            paidBy = USER_ID,
            splitBetween = "$USER_ID|user-2",
            syncStatus = SyncStatus.PENDING
        )

        val EXPENSE_SYNCED = ExpenseEntity(
            id = EXPENSE_SYNCED_ID,
            groupId = GROUP_ID,
            title = "Old dinner",
            amountInCents = 1_000_00,
            paidBy = USER_ID,
            splitBetween = "$USER_ID|user-2",
            syncStatus = SyncStatus.SYNCED
        )

        val REMOTE_EXPENSE = RemoteExpense(
            id = "expense-remote-2",
            description = "Taxi to the airport",
            amountInCents = 4_800_00,
            paidBy = USER_ID,
            paidByName = "You",
            myShareInCents = 2_400_00
        )
    }
}
