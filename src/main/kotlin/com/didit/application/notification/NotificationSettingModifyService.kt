package com.didit.application.notification

import com.didit.application.notification.provided.NotificationSettingFinder
import com.didit.application.notification.provided.NotificationSettingModifier
import com.didit.application.notification.required.NotificationSettingRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalTime
import java.util.UUID

@Transactional(readOnly = true)
@Service
class NotificationSettingModifyService(
    private val notificationSettingRepository: NotificationSettingRepository,
    private val notificationSettingFinder: NotificationSettingFinder,
) : NotificationSettingModifier {

    @Transactional
    override fun updateSetting(userId: UUID, enabled: Boolean, reminderTime: LocalTime) {
        val setting = notificationSettingFinder.findByUserId(userId)

        setting.updateSetting(enabled, reminderTime)
    }

    @Transactional
    override fun updateMarketingConsent(userId: UUID, consent: Boolean) {
        val setting = notificationSettingFinder.findByUserId(userId)

        setting.updateMarketingConsent(consent)
    }

    @Transactional
    override fun updateNightPushConsent(userId: UUID, consent: Boolean) {
        val setting = notificationSettingFinder.findByUserId(userId)

        setting.updateNightPushConsent(consent)
    }
}