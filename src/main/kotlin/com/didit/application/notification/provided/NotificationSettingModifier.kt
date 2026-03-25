package com.didit.application.notification.provided

import java.time.LocalTime
import java.util.UUID

interface NotificationSettingModifier {
    fun updateSetting(
        userId: UUID,
        enabled: Boolean,
        reminderTime: LocalTime,
    )

    fun updateNightPushConsent(
        userId: UUID,
        consent: Boolean,
    )
}
