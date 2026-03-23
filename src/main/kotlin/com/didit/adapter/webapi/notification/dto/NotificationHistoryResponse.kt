package com.didit.adapter.webapi.notification.dto

import com.didit.domain.notification.NotificationHistory
import com.didit.domain.notification.NotificationType
import java.time.LocalDateTime
import java.util.UUID

data class NotificationHistoryResponse(
    val id: UUID,
    val type: NotificationType,
    val title: String,
    val body: String,
    val isRead: Boolean,
    val createdAt: LocalDateTime?,
) {
    companion object {
        fun from(history: NotificationHistory): NotificationHistoryResponse =
            NotificationHistoryResponse(
                id = history.id,
                type = history.type,
                title = history.title,
                body = history.body,
                isRead = history.isRead,
                createdAt = history.createdAt,
            )
    }
}
