package com.didit.application.notification.provided

import com.didit.domain.notification.NotificationSetting
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class NotificationSettingFinderTest {
    @Mock
    lateinit var notificationSettingFinder: NotificationSettingFinder

    @Test
    fun `findByUserId`() {
        val userId = UUID.randomUUID()
        val setting = NotificationSetting.create(userId)
        whenever(notificationSettingFinder.findByUserId(userId)).thenReturn(setting)

        val found = notificationSettingFinder.findByUserId(userId)

        verify(notificationSettingFinder).findByUserId(userId)
        assertThat(found.userId).isEqualTo(userId)
    }
}
