package com.didit.adapter.webapi.notification

import com.didit.adapter.webapi.notification.dto.DeviceTokenRequest
import com.didit.application.notification.provided.DeviceTokenRegister
import com.didit.domain.notification.DeviceTokenRegisterRequest
import com.didit.domain.notification.DeviceType
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/v1/device-tokens")
class DeviceTokenApi(
    private val deviceTokenRegister: DeviceTokenRegister,
) {
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    fun register(
        @RequestHeader("X-User-Id") userId: UUID,
        @RequestBody request: DeviceTokenRequest,
    ) {
        deviceTokenRegister.register(
            DeviceTokenRegisterRequest(
                userId = userId,
                token = request.token,
                deviceType = request.deviceType,
            ),
        )
    }

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping
    fun delete(
        @RequestHeader("X-User-Id") userId: UUID,
        @RequestParam deviceType: DeviceType,
    ) {
        deviceTokenRegister.delete(userId, deviceType)
    }
}
