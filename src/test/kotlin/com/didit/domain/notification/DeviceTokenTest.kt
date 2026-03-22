package com.didit.domain.notification

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.util.UUID

class DeviceTokenTest {

    @Test
    fun `register`() {
        val request = DeviceTokenRegisterRequest(
            userId = UUID.randomUUID(),
            token = "test-token",
            deviceType = DeviceType.IOS,
        )
        val deviceToken = DeviceToken.register(request)

        assertThat(deviceToken.token).isEqualTo("test-token")
        assertThat(deviceToken.deviceType).isEqualTo(DeviceType.IOS)
    }

    @Test
    fun `register with blank token`() {
        assertThatThrownBy {
            DeviceTokenRegisterRequest(
                userId = UUID.randomUUID(),
                token = "",
                deviceType = DeviceType.IOS,
            )
        }.isInstanceOf(IllegalArgumentException::class.java)
            .hasMessage("토큰은 비어있을 수 없습니다.")
    }

    @Test
    fun `update`() {
        val deviceToken = DeviceToken.register(
            DeviceTokenRegisterRequest(
                userId = UUID.randomUUID(),
                token = "old-token",
                deviceType = DeviceType.IOS,
            )
        )

        deviceToken.update("new-token")

        assertThat(deviceToken.token).isEqualTo("new-token")
    }

    @Test
    fun `update with blank token`() {
        val deviceToken = DeviceToken.register(
            DeviceTokenRegisterRequest(
                userId = UUID.randomUUID(),
                token = "test-token",
                deviceType = DeviceType.IOS,
            )
        )

        assertThatThrownBy {
            deviceToken.update("")
        }.isInstanceOf(IllegalArgumentException::class.java)
            .hasMessage("토큰은 비어있을 수 없습니다.")
    }
}