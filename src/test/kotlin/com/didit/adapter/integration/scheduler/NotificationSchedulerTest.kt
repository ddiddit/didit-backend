package com.didit.adapter.integration.scheduler

import com.didit.adapter.integration.fcm.FcmClient
import com.didit.application.notification.provided.DeviceTokenFinder
import com.didit.application.notification.provided.NotificationHistoryRegister
import com.didit.application.notification.provided.NotificationSettingFinder
import com.didit.domain.notification.DeviceToken
import com.didit.domain.notification.DeviceTokenRegisterRequest
import com.didit.domain.notification.DeviceType
import com.didit.domain.notification.NotificationSetting
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class NotificationSchedulerTest {
    @Mock
    lateinit var notificationSettingFinder: NotificationSettingFinder

    @Mock
    lateinit var deviceTokenFinder: DeviceTokenFinder

    @Mock
    lateinit var fcmClient: FcmClient

    @Mock
    lateinit var notificationHistoryRegister: NotificationHistoryRegister

    @InjectMocks
    lateinit var notificationScheduler: NotificationScheduler

    @Test
    fun `send reminder notifications`() {
        val userId = UUID.randomUUID()
        val setting = NotificationSetting.create(userId)
        val token =
            DeviceToken.register(
                DeviceTokenRegisterRequest(
                    userId = userId,
                    token = "test-token",
                    deviceType = DeviceType.IOS,
                ),
            )
        whenever(notificationSettingFinder.findAllByReminderTime(any())).thenReturn(listOf(setting))
        whenever(deviceTokenFinder.findAllByUserId(userId)).thenReturn(listOf(token))

        notificationScheduler.sendReminderNotifications()

        verify(fcmClient).sendMessage("test-token", NotificationScheduler.DAILY_REMINDER_TITLE, NotificationScheduler.DAILY_REMINDER_BODY)
        verify(notificationHistoryRegister).save(any())
    }

    @Test
    fun `no notifications when no settings`() {
        whenever(notificationSettingFinder.findAllByReminderTime(any())).thenReturn(emptyList())

        notificationScheduler.sendReminderNotifications()

        verify(fcmClient, never()).sendMessage(any(), any(), any())
        verify(notificationHistoryRegister, never()).save(any())
    }

    @Test
    fun `no notifications when no device tokens`() {
        val userId = UUID.randomUUID()
        val setting = NotificationSetting.create(userId)
        whenever(notificationSettingFinder.findAllByReminderTime(any())).thenReturn(listOf(setting))
        whenever(deviceTokenFinder.findAllByUserId(userId)).thenReturn(emptyList())

        notificationScheduler.sendReminderNotifications()

        verify(fcmClient, never()).sendMessage(any(), any(), any())
        verify(notificationHistoryRegister, never()).save(any())
    }
}
