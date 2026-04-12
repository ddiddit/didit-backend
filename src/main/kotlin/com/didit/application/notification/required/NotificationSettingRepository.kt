package com.didit.application.notification.required

import com.didit.domain.notification.NotificationSetting
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.Repository
import org.springframework.data.repository.query.Param
import java.time.LocalTime
import java.util.UUID

interface NotificationSettingRepository : Repository<NotificationSetting, UUID> {
    fun save(notificationSetting: NotificationSetting): NotificationSetting

    fun findByUserId(userId: UUID): NotificationSetting?

    @Query(
        """
        SELECT n FROM NotificationSetting n
        WHERE n.enabled = true
        AND n.reminderTime = :reminderTime
        AND (:isNightTime = false OR n.nightPushConsent = true)
    """,
    )
    fun findAllByReminderTime(
        @Param("reminderTime") reminderTime: LocalTime,
        @Param("isNightTime") isNightTime: Boolean,
    ): List<NotificationSetting>

    fun deleteByUserId(userId: UUID)
}
