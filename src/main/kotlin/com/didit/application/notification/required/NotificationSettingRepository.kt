package com.didit.application.notification.required

import com.didit.domain.notification.NotificationSetting
import org.springframework.data.repository.Repository
import java.time.LocalTime
import java.util.UUID

interface NotificationSettingRepository : Repository<NotificationSetting, UUID> {
    fun save(notificationSetting: NotificationSetting): NotificationSetting

    fun findByUserId(userId: UUID): NotificationSetting?

    fun findAllByEnabledTrueAndReminderTime(reminderTime: LocalTime): List<NotificationSetting>
}
