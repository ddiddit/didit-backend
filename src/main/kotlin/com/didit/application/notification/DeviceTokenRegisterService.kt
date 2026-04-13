package com.didit.application.notification

import com.didit.application.notification.provided.DeviceTokenRegister
import com.didit.application.notification.required.DeviceTokenRepository
import com.didit.domain.notification.DeviceToken
import com.didit.domain.notification.DeviceTokenRegisterRequest
import com.didit.domain.notification.DeviceType
import org.slf4j.LoggerFactory
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Transactional(readOnly = true)
@Service
class DeviceTokenRegisterService(
    private val deviceTokenRepository: DeviceTokenRepository,
) : DeviceTokenRegister {
    companion object {
        private val logger = LoggerFactory.getLogger(DeviceTokenRegisterService::class.java)
    }

    @Transactional
    override fun register(request: DeviceTokenRegisterRequest) {
        try {
            deviceTokenRepository
                .findByUserIdAndDeviceType(request.userId, request.deviceType)
                ?.update(request.token)
                ?: deviceTokenRepository.save(DeviceToken.register(request))

            logger.info("기기 토큰 등록 - userId: ${request.userId}, deviceType: ${request.deviceType}")
        } catch (e: DataIntegrityViolationException) {
            logger.warn("기기 토큰 중복 삽입 무시 - userId: ${request.userId}, deviceType: ${request.deviceType}")
        }
    }

    @Transactional
    override fun delete(
        userId: UUID,
        deviceType: DeviceType,
    ) {
        deviceTokenRepository.deleteByUserIdAndDeviceType(userId, deviceType)

        logger.info("기기 토큰 삭제 - userId: $userId, deviceType: $deviceType")
    }
}
