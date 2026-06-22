package com.didit.adapter.integration.scheduler

import com.didit.application.notification.provided.NotificationHistoryRegister
import com.didit.application.notification.provided.NotificationSettingFinder
import com.didit.application.notification.provided.UserPushSender
import com.didit.domain.notification.NotificationSetting
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class NotificationSchedulerTest {
    @Mock lateinit var notificationSettingFinder: NotificationSettingFinder

    @Mock lateinit var notificationHistoryRegister: NotificationHistoryRegister

    @Mock lateinit var userPushSender: UserPushSender

    @InjectMocks
    lateinit var notificationScheduler: NotificationScheduler

    @Test
    fun `send reminder notifications`() {
        val userId = UUID.randomUUID()
        val setting = NotificationSetting.create(userId)
        whenever(notificationSettingFinder.findAllByReminderTime(any())).thenReturn(listOf(setting))

        notificationScheduler.sendReminderNotifications()

        verify(notificationHistoryRegister).save(any())
        verify(userPushSender).sendToUser(
            eq(userId),
            eq(NotificationScheduler.DAILY_REMINDER_TITLE),
            eq(NotificationScheduler.DAILY_REMINDER_BODY),
            eq(NotificationScheduler.DAILY_REMINDER_LINK),
        )
    }

    @Test
    fun `no notifications when no settings`() {
        whenever(notificationSettingFinder.findAllByReminderTime(any())).thenReturn(emptyList())

        notificationScheduler.sendReminderNotifications()

        verify(userPushSender, never()).sendToUser(any(), any(), any(), any())
        verify(notificationHistoryRegister, never()).save(any())
    }
}
