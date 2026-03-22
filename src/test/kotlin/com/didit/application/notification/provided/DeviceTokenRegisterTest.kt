package com.didit.application.notification.provided

import com.didit.domain.notification.DeviceTokenRegisterRequest
import com.didit.domain.notification.DeviceType
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class DeviceTokenRegisterTest {

    @Mock
    lateinit var deviceTokenRegister: DeviceTokenRegister

    @Test
    fun `register`() {
        val request = DeviceTokenRegisterRequest(
            userId = UUID.randomUUID(),
            token = "test-token",
            deviceType = DeviceType.IOS,
        )

        deviceTokenRegister.register(request)

        verify(deviceTokenRegister).register(request)
    }

    @Test
    fun `delete`() {
        val userId = UUID.randomUUID()

        deviceTokenRegister.delete(userId, DeviceType.IOS)

        verify(deviceTokenRegister).delete(userId, DeviceType.IOS)
    }
}