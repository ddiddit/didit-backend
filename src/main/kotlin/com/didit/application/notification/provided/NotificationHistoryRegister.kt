package com.didit.application.notification.provided

import com.didit.domain.notification.NotificationHistory
import com.didit.domain.notification.NotificationHistoryCreateRequest
import java.util.UUID

interface NotificationHistoryRegister {
    fun save(request: NotificationHistoryCreateRequest): NotificationHistory

    fun readAll(userId: UUID)

    fun read(
        notificationId: UUID,
        userId: UUID,
    )
}
