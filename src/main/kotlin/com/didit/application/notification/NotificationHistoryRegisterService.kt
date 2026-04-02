package com.didit.application.notification

import com.didit.application.notification.exception.NotificationHistoryNotFoundException
import com.didit.application.notification.provided.NotificationHistoryFinder
import com.didit.application.notification.provided.NotificationHistoryRegister
import com.didit.application.notification.required.NotificationHistoryRepository
import com.didit.domain.notification.NotificationHistory
import com.didit.domain.notification.NotificationHistoryCreateRequest
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Transactional(readOnly = true)
@Service
class NotificationHistoryRegisterService(
    private val notificationHistoryRepository: NotificationHistoryRepository,
    private val notificationHistoryFinder: NotificationHistoryFinder,
) : NotificationHistoryRegister {
    companion object {
        private val logger = LoggerFactory.getLogger(NotificationHistoryRegisterService::class.java)
    }

    @Transactional
    override fun save(request: NotificationHistoryCreateRequest): NotificationHistory {
        val saved = notificationHistoryRepository.save(NotificationHistory.create(request))

        logger.info("알림 히스토리 저장 - userId: ${request.userId}, type: ${request.type}")

        return saved
    }

    @Transactional
    override fun readAll(userId: UUID) {
        notificationHistoryFinder
            .findAllByUserId(userId)
            .forEach { it.read() }

        logger.info("알림 전체 읽음 처리 - userId: $userId")
    }

    @Transactional
    override fun read(
        notificationId: UUID,
        userId: UUID,
    ) {
        val notification =
            notificationHistoryRepository.findByIdAndUserId(notificationId, userId)
                ?: throw NotificationHistoryNotFoundException(notificationId, userId)

        notification.read()

        logger.info("알림 개별 읽음 처리 - notificationId: $notificationId, userId: $userId")
    }
}
