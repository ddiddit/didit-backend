package com.didit.adapter.webapi.notification.dto

import com.didit.domain.notification.NotificationSetting
import java.time.LocalTime

data class NotificationSettingResponse(
    val enabled: Boolean,
    val reminderTime: LocalTime,
    val marketingConsent: Boolean,
    val nightPushConsent: Boolean,
) {
    companion object {
        fun from(setting: NotificationSetting): NotificationSettingResponse =
            NotificationSettingResponse(
                enabled = setting.enabled,
                reminderTime = setting.reminderTime,
                marketingConsent = setting.marketingConsent,
                nightPushConsent = setting.nightPushConsent,
            )
    }
}
