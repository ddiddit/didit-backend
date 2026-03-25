package com.didit.domain.notification

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.time.LocalTime
import java.util.UUID

class NotificationSettingTest {
    @Test
    fun `create`() {
        val userId = UUID.randomUUID()
        val setting = NotificationSetting.create(userId)

        assertThat(setting.userId).isEqualTo(userId)
        assertThat(setting.enabled).isFalse()
        assertThat(setting.nightPushConsent).isFalse()
        assertThat(setting.reminderTime).isEqualTo(LocalTime.of(20, 0))
    }

    @Test
    fun `update setting`() {
        val setting = NotificationSetting.create(UUID.randomUUID())

        setting.updateSetting(
            enabled = true,
            reminderTime = LocalTime.of(20, 0),
            nightPushConsent = false,
        )

        assertThat(setting.enabled).isTrue()
        assertThat(setting.reminderTime).isEqualTo(LocalTime.of(20, 0))
    }

    @Test
    fun `update setting - night time without consent throws exception`() {
        val setting = NotificationSetting.create(UUID.randomUUID())

        assertThatThrownBy {
            setting.updateSetting(
                enabled = true,
                reminderTime = LocalTime.of(22, 0),
                nightPushConsent = false,
            )
        }.isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `update setting - night time with consent success`() {
        val setting = NotificationSetting.create(UUID.randomUUID())

        setting.updateSetting(
            enabled = true,
            reminderTime = LocalTime.of(22, 0),
            nightPushConsent = true,
        )

        assertThat(setting.reminderTime).isEqualTo(LocalTime.of(22, 0))
    }
}
