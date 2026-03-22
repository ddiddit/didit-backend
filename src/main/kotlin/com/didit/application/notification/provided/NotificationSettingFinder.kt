package com.didit.application.notification.provided

import com.didit.domain.notification.NotificationSetting
import java.util.UUID

interface NotificationSettingFinder {
    fun findByUserId(userId: UUID): NotificationSetting
}
