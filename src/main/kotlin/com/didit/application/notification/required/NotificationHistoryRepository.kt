package com.didit.application.notification.required

import com.didit.domain.notification.NotificationHistory
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.Repository
import org.springframework.data.repository.query.Param
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

    @Modifying
    @Query("DELETE FROM NotificationHistory h WHERE h.userId = :userId")
    fun deleteAllByUserId(
        @Param("userId") userId: UUID,
    )
}
