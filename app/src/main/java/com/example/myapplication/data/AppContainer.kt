package com.example.myapplication.data

import com.example.myapplication.data.repository.AuthRepository
import com.example.myapplication.data.repository.BalanceRepository
import com.example.myapplication.data.repository.ExpenseRepository
import com.example.myapplication.data.repository.GroupRepository
import com.example.myapplication.data.repository.NotificationRepository
import com.example.myapplication.data.repository.ProfileRepository
import com.example.myapplication.data.mock.MockAuthRepository
import com.example.myapplication.data.mock.MockBalanceRepository
import com.example.myapplication.data.mock.MockExpenseRepository
import com.example.myapplication.data.mock.MockGroupRepository
import com.example.myapplication.data.mock.MockNotificationRepository
import com.example.myapplication.data.mock.MockProfileRepository

class AppContainer(
    val authRepository: AuthRepository = MockAuthRepository(),
    val groupRepository: GroupRepository = MockGroupRepository(),
    val expenseRepository: ExpenseRepository = MockExpenseRepository(),
    val balanceRepository: BalanceRepository = MockBalanceRepository(),
    val notificationRepository: NotificationRepository = MockNotificationRepository(),
    val profileRepository: ProfileRepository = MockProfileRepository(),
)
