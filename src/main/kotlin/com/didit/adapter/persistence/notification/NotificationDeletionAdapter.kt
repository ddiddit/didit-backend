package com.didit.adapter.persistence.notification

import com.didit.application.notification.provided.NotificationDeletionPort
import com.didit.application.notification.required.NotificationHistoryRepository
import com.didit.application.notification.required.NotificationSettingRepository
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class NotificationDeletionAdapter(
    private val notificationHistoryRepository: NotificationHistoryRepository,
    private val notificationSettingRepository: NotificationSettingRepository,
) : NotificationDeletionPort {
    override fun deleteByUserId(userId: UUID) {
        notificationHistoryRepository.deleteAllByUserId(userId)
        notificationSettingRepository.deleteByUserId(userId)
    }
}
