package com.didit.domain.notification

import org.assertj.core.api.Assertions.assertThat
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
        assertThat(setting.marketingConsent).isFalse()
        assertThat(setting.nightPushConsent).isFalse()
        assertThat(setting.reminderTime).isEqualTo(LocalTime.of(20, 0))
    }

    @Test
    fun `update setting`() {
        val setting = NotificationSetting.create(UUID.randomUUID())

        setting.updateSetting(
            enabled = true,
            reminderTime = LocalTime.of(21, 0),
        )

        assertThat(setting.enabled).isTrue()
        assertThat(setting.reminderTime).isEqualTo(LocalTime.of(21, 0))
    }

    @Test
    fun `update marketing consent`() {
        val setting = NotificationSetting.create(UUID.randomUUID())

        setting.updateMarketingConsent(true)

        assertThat(setting.marketingConsent).isTrue()
    }

    @Test
    fun `update night push consent`() {
        val setting = NotificationSetting.create(UUID.randomUUID())

        setting.updateNightPushConsent(true)

        assertThat(setting.nightPushConsent).isTrue()
    }
}