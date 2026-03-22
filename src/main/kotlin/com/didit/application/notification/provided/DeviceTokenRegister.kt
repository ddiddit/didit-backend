package com.didit.application.notification.provided

import com.didit.domain.notification.DeviceTokenRegisterRequest
import com.didit.domain.notification.DeviceType
import java.util.UUID

interface DeviceTokenRegister {
    fun register(request: DeviceTokenRegisterRequest)

    fun delete(userId: UUID, deviceType: DeviceType)
}