package com.didit.application.notification

import com.didit.application.notification.provided.NotificationHistoryRegister
import com.didit.domain.notification.NotificationHistoryCreateRequest
import com.didit.domain.notification.NotificationType
import com.didit.domain.retrospect.RetrospectiveCompletedEvent
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Component
class RetrospectiveNotificationEventListener(
    private val notificationHistoryRegister: NotificationHistoryRegister,
) {
    companion object {
        private val logger = LoggerFactory.getLogger(RetrospectiveNotificationEventListener::class.java)

        const val RETROSPECTIVE_RESULT_CREATED_TITLE = "회고 결과가 생성되었어요"
        const val RETROSPECTIVE_RESULT_CREATED_BODY = "작성한 회고 결과를 확인해 보세요."
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun onRetrospectiveCompleted(event: RetrospectiveCompletedEvent) {
        try {
            notificationHistoryRegister.save(
                NotificationHistoryCreateRequest(
                    userId = event.userId,
                    type = NotificationType.RETROSPECTIVE_RESULT_CREATED,
                    title = RETROSPECTIVE_RESULT_CREATED_TITLE,
                    body = RETROSPECTIVE_RESULT_CREATED_BODY,
                ),
            )
        } catch (e: Exception) {
            logger.error("회고 결과 생성 완료 알림 저장 실패 - userId: ${event.userId}", e)
        }
    }
}
