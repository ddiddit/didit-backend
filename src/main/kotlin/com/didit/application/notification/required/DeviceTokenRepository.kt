package com.didit.application.notification.required

import com.didit.domain.notification.DeviceToken
import com.didit.domain.notification.DeviceType
import org.springframework.data.repository.Repository
import java.util.UUID

interface DeviceTokenRepository : Repository<DeviceToken, UUID> {
    fun save(deviceToken: DeviceToken): DeviceToken

    fun findByUserIdAndDeviceType(
        userId: UUID,
        deviceType: DeviceType,
    ): DeviceToken?

    fun deleteByUserIdAndDeviceType(
        userId: UUID,
        deviceType: DeviceType,
    )

    fun findAllByUserId(userId: UUID): List<DeviceToken>
}
