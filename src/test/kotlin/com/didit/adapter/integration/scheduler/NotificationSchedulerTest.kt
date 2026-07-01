package com.didit.adapter.integration.scheduler

import com.didit.application.notification.provided.NotificationSettingFinder
import com.didit.domain.notification.NotificationSetting
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.eq
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class NotificationSchedulerTest {
    @Mock lateinit var notificationSettingFinder: NotificationSettingFinder

    @Mock lateinit var reminderNotificationSender: ReminderNotificationSender

    @InjectMocks
    lateinit var notificationScheduler: NotificationScheduler

    @Test
    fun `send reminder notifications`() {
        val userId = UUID.randomUUID()
        val setting = NotificationSetting.create(userId)
        whenever(notificationSettingFinder.findAllByReminderTime(any())).thenReturn(listOf(setting))

        notificationScheduler.sendReminderNotifications()

        verify(reminderNotificationSender).send(eq(userId))
    }

    @Test
    fun `no notifications when no settings`() {
        whenever(notificationSettingFinder.findAllByReminderTime(any())).thenReturn(emptyList())

        notificationScheduler.sendReminderNotifications()

        verify(reminderNotificationSender, never()).send(any())
    }

    @Test
    fun `한 유저 전송이 실패해도 나머지 유저에게는 전송된다`() {
        val failing = UUID.randomUUID()
        val succeeding = UUID.randomUUID()
        whenever(notificationSettingFinder.findAllByReminderTime(any()))
            .thenReturn(listOf(NotificationSetting.create(failing), NotificationSetting.create(succeeding)))
        doThrow(RuntimeException("전송 실패")).whenever(reminderNotificationSender).send(failing)

        notificationScheduler.sendReminderNotifications()

        verify(reminderNotificationSender).send(failing)
        verify(reminderNotificationSender).send(succeeding)
    }
}
