package com.didit.application.notification

import com.didit.application.notification.provided.NotificationSettingFinder
import com.didit.application.notification.provided.NotificationSettingModifier
import com.didit.application.notification.required.NotificationSettingRepository
import com.didit.domain.notification.NotificationSetting
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
    @Transactional
    override fun updateNightPushConsent(
        userId: UUID,
        consent: Boolean,
    ) {
        val setting =
            notificationSettingFinder.findByUserIdOrNull(userId)
                ?: notificationSettingRepository.save(NotificationSetting.create(userId))

        setting.updateNightPushConsent(consent)
    }

    @Transactional
    override fun updateSetting(
        userId: UUID,
        enabled: Boolean,
        reminderTime: LocalTime,
    ) {
        val setting = notificationSettingFinder.findByUserId(userId)
        setting.updateSetting(enabled, reminderTime, setting.nightPushConsent)
    }
}
