package com.didit.application.notification.required

import com.didit.domain.notification.NotificationSetting
import com.didit.support.RepositoryTestSupport
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalTime
import java.util.UUID

class NotificationSettingRepositoryTest : RepositoryTestSupport() {
    @Autowired
    lateinit var notificationSettingRepository: NotificationSettingRepository

    @Test
    fun `save`() {
        val setting = NotificationSetting.create(UUID.randomUUID())

        val saved = notificationSettingRepository.save(setting)

        assertThat(saved.userId).isEqualTo(setting.userId)
        assertThat(saved.enabled).isFalse()
    }

    @Test
    fun `findByUserId`() {
        val userId = UUID.randomUUID()
        notificationSettingRepository.save(NotificationSetting.create(userId))

        val found = notificationSettingRepository.findByUserId(userId)

        assertThat(found).isNotNull
        assertThat(found?.userId).isEqualTo(userId)
    }

    @Test
    fun `findByUserId - return null when not exists`() {
        val found = notificationSettingRepository.findByUserId(UUID.randomUUID())

        assertThat(found).isNull()
    }

    @Test
    fun `findAllByEnabledTrueAndReminderTime`() {
        val userId = UUID.randomUUID()
        val setting = NotificationSetting.create(userId)
        setting.updateSetting(true, LocalTime.of(20, 0), false)
        notificationSettingRepository.save(setting)

        val found = notificationSettingRepository.findAllByEnabledTrueAndReminderTime(LocalTime.of(20, 0))

        assertThat(found).hasSize(1)
        assertThat(found[0].userId).isEqualTo(userId)
    }
}
