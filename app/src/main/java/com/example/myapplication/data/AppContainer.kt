package com.example.myapplication.data

import com.example.myapplication.data.repository.AuthRepository
import com.example.myapplication.data.repository.BalanceRepository
import com.example.myapplication.data.repository.ExpenseRepository
import com.example.myapplication.data.repository.GroupRepository
import com.example.myapplication.data.repository.InMemoryAuthRepository
import com.example.myapplication.data.repository.InMemoryBalanceRepository
import com.example.myapplication.data.repository.InMemoryExpenseRepository
import com.example.myapplication.data.repository.InMemoryGroupRepository
import com.example.myapplication.data.repository.InMemoryNotificationRepository
import com.example.myapplication.data.repository.InMemoryProfileRepository
import com.example.myapplication.data.repository.NotificationRepository
import com.example.myapplication.data.repository.ProfileRepository

class AppContainer(
    val authRepository: AuthRepository = InMemoryAuthRepository(),
    val groupRepository: GroupRepository = InMemoryGroupRepository(),
    val expenseRepository: ExpenseRepository = InMemoryExpenseRepository(),
    val balanceRepository: BalanceRepository = InMemoryBalanceRepository(),
    val notificationRepository: NotificationRepository = InMemoryNotificationRepository(),
    val profileRepository: ProfileRepository = InMemoryProfileRepository(),
)
