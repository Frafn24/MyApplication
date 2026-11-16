package com.example.myapplication.data

import android.content.Context
import com.example.myapplication.data.local.AppDatabase
import com.example.myapplication.data.local.LocalUserData
import com.example.myapplication.data.mock.MockAuthRepository
import com.example.myapplication.data.mock.MockBalanceRepository
import com.example.myapplication.data.mock.MockExpenseRepository
import com.example.myapplication.data.mock.MockGroupRepository
import com.example.myapplication.data.mock.MockNotificationRepository
import com.example.myapplication.data.mock.MockProfileRepository
import com.example.myapplication.data.repository.AuthRepository
import com.example.myapplication.data.repository.BalanceRepository
import com.example.myapplication.data.repository.ExpenseRepository
import com.example.myapplication.data.repository.GroupRepository
import com.example.myapplication.data.repository.NotificationRepository
import com.example.myapplication.data.repository.ProfileRepository
import com.example.myapplication.data.repository.RoomBalanceRepository
import com.example.myapplication.data.repository.RoomExpenseRepository
import com.example.myapplication.data.repository.RoomGroupRepository
import com.example.myapplication.data.repository.RoomNotificationRepository
import com.example.myapplication.data.repository.RoomProfileRepository
import com.example.myapplication.data.repository.SupabaseAuthRepository
import com.example.myapplication.data.sync.SyncCoordinator
import com.example.myapplication.data.sync.WorkManagerSyncScheduler

class AppContainer(
    val authRepository: AuthRepository = MockAuthRepository(),
    val groupRepository: GroupRepository = MockGroupRepository(),
    val expenseRepository: ExpenseRepository = MockExpenseRepository(),
    val balanceRepository: BalanceRepository = MockBalanceRepository(),
    val notificationRepository: NotificationRepository = MockNotificationRepository(),
    val profileRepository: ProfileRepository = MockProfileRepository(),
    val localUserData: LocalUserData? = null,
    val syncCoordinator: SyncCoordinator? = null
) {
    companion object {
        fun withRoom(context: Context): AppContainer {
            val database = AppDatabase.getInstance(context)
            val authRepository = SupabaseAuthRepository()

            return AppContainer(
                authRepository = authRepository,
                groupRepository = RoomGroupRepository(
                    database = database,
                    currentUserId = { authRepository.currentUserIdOrNull() },
                    syncScheduler = WorkManagerSyncScheduler(context)
                ),
                expenseRepository = RoomExpenseRepository(
                    database = database,
                    currentUserId = { authRepository.currentUserIdOrNull() },
                    syncScheduler = WorkManagerSyncScheduler(context)
                ),
                balanceRepository = RoomBalanceRepository(database, WorkManagerSyncScheduler(context)),
                notificationRepository = RoomNotificationRepository(database, WorkManagerSyncScheduler(context)),
                profileRepository = RoomProfileRepository(database) {
                    authRepository.currentUserIdOrNull()
                },
                localUserData = LocalUserData(database, context.applicationContext),
                syncCoordinator = SyncCoordinator(database)
            ).also {
                // Keeps the cache fresh in the background, so badges and system
                // notifications appear without opening each screen.
                WorkManagerSyncScheduler(context).enqueuePeriodicSync()
            }
        }
    }
}
