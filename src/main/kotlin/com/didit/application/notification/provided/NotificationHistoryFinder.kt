package com.didit.application.notification.provided

import com.didit.domain.notification.NotificationHistory
import java.util.UUID

interface NotificationHistoryFinder {
    fun findAllByUserId(userId: UUID): List<NotificationHistory>
}
