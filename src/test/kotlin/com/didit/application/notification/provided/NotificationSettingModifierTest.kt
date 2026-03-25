package com.didit.application.notification.provided

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.verify
import java.time.LocalTime
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class NotificationSettingModifierTest {
    @Mock
    lateinit var notificationSettingModifier: NotificationSettingModifier

    @Test
    fun `updateSetting`() {
        val userId = UUID.randomUUID()

        notificationSettingModifier.updateSetting(userId, true, LocalTime.of(21, 0))

        verify(notificationSettingModifier).updateSetting(userId, true, LocalTime.of(21, 0))
    }

    @Test
    fun `updateNightPushConsent`() {
        val userId = UUID.randomUUID()

        notificationSettingModifier.updateNightPushConsent(userId, true)

        verify(notificationSettingModifier).updateNightPushConsent(userId, true)
    }
}
