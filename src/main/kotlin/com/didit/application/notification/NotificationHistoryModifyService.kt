package com.didit.application.notification

import com.didit.application.notification.exception.NotificationHistoryNotFoundException
import com.didit.application.notification.provided.NotificationHistoryRegister
import com.didit.application.notification.required.NotificationHistoryRepository
import com.didit.domain.notification.NotificationHistory
import com.didit.domain.notification.NotificationHistoryCreateRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.UUID

@Transactional(readOnly = true)
@Service
class NotificationHistoryModifyService(
    private val notificationHistoryRepository: NotificationHistoryRepository,
) : NotificationHistoryRegister {
    @Transactional
    override fun save(request: NotificationHistoryCreateRequest): NotificationHistory =
        notificationHistoryRepository.save(NotificationHistory.create(request))

    @Transactional
    override fun read(
        id: UUID,
        userId: UUID,
    ) {
        notificationHistoryRepository
            .findByIdAndUserId(id, userId)
            ?.read()
            ?: throw NotificationHistoryNotFoundException(id)
    }

    @Transactional
    override fun readAll(userId: UUID) {
        notificationHistoryRepository
            .findAllByUserIdAndCreatedAtAfterOrderByCreatedAtDesc(
                userId = userId,
                createdAt = LocalDateTime.now().minusDays(30),
            ).forEach { it.read() }
    }
}
