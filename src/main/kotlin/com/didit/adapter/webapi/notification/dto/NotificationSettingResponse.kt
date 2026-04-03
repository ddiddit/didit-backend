package com.didit.adapter.webapi.notification.dto

import com.didit.domain.notification.NotificationSetting
import java.time.LocalTime

data class NotificationSettingResponse(
    val marketingAgreed: Boolean,
    val nightPushConsent: Boolean,
    val enabled: Boolean,
    val reminderTime: LocalTime,
) {
    companion object {
        fun of(
            setting: NotificationSetting,
            marketingAgreed: Boolean,
        ) = NotificationSettingResponse(
            marketingAgreed = marketingAgreed,
            nightPushConsent = setting.nightPushConsent,
            enabled = setting.enabled,
            reminderTime = setting.reminderTime,
        )
    }
}
