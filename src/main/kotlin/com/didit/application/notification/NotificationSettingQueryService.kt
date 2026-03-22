package com.didit.application.notification

import com.didit.application.notification.exception.NotificationSettingNotFoundException
import com.didit.application.notification.provided.NotificationSettingFinder
import com.didit.application.notification.required.NotificationSettingRepository
import com.didit.domain.notification.NotificationSetting
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Transactional(readOnly = true)
@Service
class NotificationSettingQueryService(
    private val notificationSettingRepository: NotificationSettingRepository,
) : NotificationSettingFinder {

    override fun findByUserId(userId: UUID): NotificationSetting =
        notificationSettingRepository.findByUserId(userId)
            ?: throw NotificationSettingNotFoundException(userId)
}