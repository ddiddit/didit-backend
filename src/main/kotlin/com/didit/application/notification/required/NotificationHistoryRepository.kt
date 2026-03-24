package com.didit.application.notification.required

import com.didit.domain.notification.NotificationHistory
import org.springframework.data.repository.Repository
import java.time.LocalDateTime
import java.util.UUID

interface NotificationHistoryRepository : Repository<NotificationHistory, UUID> {
    fun save(notificationHistory: NotificationHistory): NotificationHistory

    fun findAllByUserIdAndCreatedAtAfterOrderByCreatedAtDesc(
        userId: UUID,
        createdAt: LocalDateTime,
    ): List<NotificationHistory>

    fun findByIdAndUserId(
        id: UUID,
        userId: UUID,
    ): NotificationHistory?
}
