package com.didit.application.notification.provided

import com.didit.domain.notification.NotificationSetting
import java.time.LocalTime
import java.util.UUID

interface NotificationSettingFinder {
    fun findByUserId(userId: UUID): NotificationSetting

    fun findAllByReminderTime(reminderTime: LocalTime): List<NotificationSetting>
}
