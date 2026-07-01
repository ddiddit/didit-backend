package com.didit.adapter.integration.scheduler

import com.didit.application.notification.provided.NotificationHistoryRegister
import com.didit.application.notification.provided.UserPushSender
import com.didit.domain.notification.NotificationHistoryCreateRequest
import com.didit.domain.notification.NotificationType
import jakarta.transaction.Transactional
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class ReminderNotificationSender(
    private val notificationHistoryRegister: NotificationHistoryRegister,
    private val userPushSender: UserPushSender,
) {
    @Transactional
    fun send(userId: UUID) {
        notificationHistoryRegister.save(
            NotificationHistoryCreateRequest(
                userId = userId,
                type = NotificationType.DAILY_REMINDER,
                title = NotificationScheduler.DAILY_REMINDER_TITLE,
                body = NotificationScheduler.DAILY_REMINDER_BODY,
            ),
        )
        userPushSender.sendToUser(
            userId,
            NotificationScheduler.DAILY_REMINDER_TITLE,
            NotificationScheduler.DAILY_REMINDER_BODY,
            NotificationScheduler.DAILY_REMINDER_LINK,
        )
    }
}
