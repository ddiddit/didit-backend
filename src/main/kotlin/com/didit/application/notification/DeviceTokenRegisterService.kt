package com.didit.application.notification

import com.didit.application.notification.provided.DeviceTokenRegister
import com.didit.application.notification.required.DeviceTokenRepository
import com.didit.domain.notification.DeviceToken
import com.didit.domain.notification.DeviceTokenRegisterRequest
import com.didit.domain.notification.DeviceType
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Transactional(readOnly = true)
@Service
class DeviceTokenRegisterService(
    private val deviceTokenRepository: DeviceTokenRepository,
) : DeviceTokenRegister {
    @Transactional
    override fun register(request: DeviceTokenRegisterRequest) {
        deviceTokenRepository
            .findByUserIdAndDeviceType(request.userId, request.deviceType)
            ?.update(request.token)
            ?: deviceTokenRepository.save(DeviceToken.register(request))
    }

    @Transactional
    override fun delete(
        userId: UUID,
        deviceType: DeviceType,
    ) {
        deviceTokenRepository.deleteByUserIdAndDeviceType(userId, deviceType)
    }
}
