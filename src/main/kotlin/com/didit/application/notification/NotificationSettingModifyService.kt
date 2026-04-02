package com.didit.application.notification

import com.didit.application.notification.provided.NotificationSettingFinder
import com.didit.application.notification.provided.NotificationSettingModifier
import com.didit.application.notification.required.NotificationSettingRepository
import com.didit.domain.notification.NotificationSetting
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalTime
import java.util.UUID

@Transactional(readOnly = true)
@Service
class NotificationSettingModifyService(
    private val notificationSettingFinder: NotificationSettingFinder,
    private val notificationSettingRepository: NotificationSettingRepository,
) : NotificationSettingModifier {
    companion object {
        private val logger = LoggerFactory.getLogger(NotificationSettingModifyService::class.java)
    }

    @Transactional
    override fun updateNightPushConsent(
        userId: UUID,
        consent: Boolean,
    ) {
        val setting =
            notificationSettingFinder.findByUserIdOrNull(userId)
                ?: notificationSettingRepository.save(NotificationSetting.create(userId))

        setting.updateNightPushConsent(consent)

        logger.info("야간 푸시 동의 수정 - userId: $userId, consent: $consent")
    }

    @Transactional
    override fun updateSetting(
        userId: UUID,
        enabled: Boolean,
        reminderTime: LocalTime,
    ) {
        val setting = notificationSettingFinder.findByUserId(userId)

        setting.updateSetting(enabled, reminderTime, setting.nightPushConsent)

        logger.info("알림 설정 수정 - userId: $userId, enabled: $enabled, reminderTime: $reminderTime")
    }
}
