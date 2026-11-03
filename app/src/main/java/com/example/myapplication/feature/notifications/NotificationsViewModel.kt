package com.example.myapplication.feature.notifications

import androidx.lifecycle.ViewModel
import com.example.myapplication.data.model.AppNotification
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class NotificationsUiState(
    val notifications: List<AppNotification> = emptyList(),
)

/** Scaffold ViewModel; feature logic is added when NotificationsScreen is implemented. */
class NotificationsViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(NotificationsUiState())
    val uiState: StateFlow<NotificationsUiState> = _uiState.asStateFlow()

    fun loadNotifications() {
        // TODO: Load notifications from NotificationRepository.
    }
}
