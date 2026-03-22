package com.didit.application.notification

import com.didit.application.notification.provided.DeviceTokenFinder
import com.didit.application.notification.required.DeviceTokenRepository
import com.didit.domain.notification.DeviceToken
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Transactional(readOnly = true)
@Service
class DeviceTokenQueryService(
    private val deviceTokenRepository: DeviceTokenRepository,
) : DeviceTokenFinder {
    override fun findAllByUserId(userId: UUID): List<DeviceToken> = deviceTokenRepository.findAllByUserId(userId)
}
