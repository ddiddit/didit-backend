package com.didit.adapter.webapi.notification.dto

import com.didit.domain.notification.NotificationSetting
import java.time.LocalTime

data class NotificationSettingResponse(
    val enabled: Boolean,
    val reminderTime: LocalTime,
    val nightPushConsent: Boolean,
) {
    companion object {
        fun from(setting: NotificationSetting): NotificationSettingResponse =
            NotificationSettingResponse(
                enabled = setting.enabled,
                reminderTime = setting.reminderTime,
                nightPushConsent = setting.nightPushConsent,
            )
    }
}
