package com.didit.application.notification.provided

import com.didit.domain.notification.NotificationSetting
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.LocalTime
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

    @Test
    fun `findByUserIdOrNull - 존재하는 경우`() {
        val userId = UUID.randomUUID()
        val setting = NotificationSetting.create(userId)
        whenever(notificationSettingFinder.findByUserIdOrNull(userId)).thenReturn(setting)

        val found = notificationSettingFinder.findByUserIdOrNull(userId)

        verify(notificationSettingFinder).findByUserIdOrNull(userId)
        assertThat(found).isNotNull
        assertThat(found!!.userId).isEqualTo(userId)
    }

    @Test
    fun `findByUserIdOrNull - 존재하지 않는 경우`() {
        val userId = UUID.randomUUID()
        whenever(notificationSettingFinder.findByUserIdOrNull(userId)).thenReturn(null)

        val found = notificationSettingFinder.findByUserIdOrNull(userId)

        verify(notificationSettingFinder).findByUserIdOrNull(userId)
        assertThat(found).isNull()
    }

    @Test
    fun `findAllByReminderTime`() {
        val settings =
            listOf(
                NotificationSetting.create(UUID.randomUUID()),
                NotificationSetting.create(UUID.randomUUID()),
            )
        whenever(notificationSettingFinder.findAllByReminderTime(LocalTime.of(20, 0))).thenReturn(settings)

        val found = notificationSettingFinder.findAllByReminderTime(LocalTime.of(20, 0))

        verify(notificationSettingFinder).findAllByReminderTime(LocalTime.of(20, 0))
        assertThat(found).hasSize(2)
    }
}
