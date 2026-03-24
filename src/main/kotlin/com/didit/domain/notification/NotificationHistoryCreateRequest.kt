package com.didit.domain.notification

import java.util.UUID

data class NotificationHistoryCreateRequest(
    val userId: UUID,
    val type: NotificationType,
    val title: String,
    val body: String,
)
