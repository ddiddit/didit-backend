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
    fun `findAllByReminderTime - 활성화된 설정만 반환한다`() {
        val userId = UUID.randomUUID()
        val setting = NotificationSetting.create(userId)
        setting.updateSetting(true, LocalTime.of(20, 0), false)
        notificationSettingRepository.save(setting)

        val found = notificationSettingRepository.findAllByReminderTime(LocalTime.of(20, 0), false)

        assertThat(found).hasSize(1)
        assertThat(found[0].userId).isEqualTo(userId)
    }

    @Test
    fun `findAllByReminderTime - 비활성화된 설정은 제외된다`() {
        val userId = UUID.randomUUID()
        val setting = NotificationSetting.create(userId)
        notificationSettingRepository.save(setting)

        val found = notificationSettingRepository.findAllByReminderTime(LocalTime.of(20, 0), false)

        assertThat(found).isEmpty()
    }

    @Test
    fun `findAllByReminderTime - 야간 시간대에 동의하지 않은 사용자는 제외된다`() {
        val userId = UUID.randomUUID()
        val setting = NotificationSetting.create(userId)
        setting.updateNightPushConsent(false)
        notificationSettingRepository.save(setting)

        val found = notificationSettingRepository.findAllByReminderTime(LocalTime.of(22, 0), true)

        assertThat(found).isEmpty()
    }

    @Test
    fun `findAllByReminderTime - 야간 시간대에 동의한 사용자는 반환된다`() {
        val userId = UUID.randomUUID()
        val setting = NotificationSetting.create(userId)
        setting.updateNightPushConsent(true)
        setting.updateSetting(true, LocalTime.of(22, 0), true)
        notificationSettingRepository.save(setting)

        val found = notificationSettingRepository.findAllByReminderTime(LocalTime.of(22, 0), true)

        assertThat(found).hasSize(1)
        assertThat(found[0].userId).isEqualTo(userId)
    }

    @Test
    fun `deleteByUserId - 해당 유저의 알림 설정이 삭제된다`() {
        val userId = UUID.randomUUID()
        val otherUserId = UUID.randomUUID()

        notificationSettingRepository.save(NotificationSetting.create(userId))
        notificationSettingRepository.save(NotificationSetting.create(otherUserId))

        notificationSettingRepository.deleteByUserId(userId)

        assertThat(notificationSettingRepository.findByUserId(userId)).isNull()
        assertThat(notificationSettingRepository.findByUserId(otherUserId)).isNotNull()
    }
}
