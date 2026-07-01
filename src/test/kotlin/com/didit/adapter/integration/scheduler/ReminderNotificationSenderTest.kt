package com.didit.adapter.integration.scheduler

import com.didit.application.notification.provided.NotificationHistoryRegister
import com.didit.application.notification.provided.UserPushSender
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class ReminderNotificationSenderTest {
    @Mock lateinit var notificationHistoryRegister: NotificationHistoryRegister

    @Mock lateinit var userPushSender: UserPushSender

    @InjectMocks
    lateinit var reminderNotificationSender: ReminderNotificationSender

    @Test
    fun `send - 알림 이력을 저장하고 푸시를 전송한다`() {
        val userId = UUID.randomUUID()

        reminderNotificationSender.send(userId)

        verify(notificationHistoryRegister).save(any())
        verify(userPushSender).sendToUser(
            eq(userId),
            eq(NotificationScheduler.DAILY_REMINDER_TITLE),
            eq(NotificationScheduler.DAILY_REMINDER_BODY),
            eq(NotificationScheduler.DAILY_REMINDER_LINK),
        )
    }
}
