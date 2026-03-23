package com.didit.application.notification

import com.didit.application.notification.provided.NotificationHistoryFinder
import com.didit.application.notification.required.NotificationHistoryRepository
import com.didit.domain.notification.NotificationHistory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.UUID

@Transactional(readOnly = true)
@Service
class NotificationHistoryQueryService(
    private val notificationHistoryRepository: NotificationHistoryRepository,
) : NotificationHistoryFinder {
    override fun findAllByUserId(userId: UUID): List<NotificationHistory> =
        notificationHistoryRepository.findAllByUserIdAndCreatedAtAfterOrderByCreatedAtDesc(
            userId = userId,
            createdAt = LocalDateTime.now().minusDays(30),
        )
}
