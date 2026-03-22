package com.didit.application.notification.required

import com.didit.adapter.config.JpaAuditingConfig
import com.didit.domain.notification.DeviceToken
import com.didit.domain.notification.DeviceTokenRegisterRequest
import com.didit.domain.notification.DeviceType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.context.annotation.Import
import java.util.UUID

@DataJpaTest
@Import(JpaAuditingConfig::class)
class DeviceTokenRepositoryTest {

    @Autowired
    lateinit var deviceTokenRepository: DeviceTokenRepository

    @Test
    fun `save`() {
        val deviceToken = DeviceToken.register(
            DeviceTokenRegisterRequest(
                userId = UUID.randomUUID(),
                token = "test-token",
                deviceType = DeviceType.IOS,
            )
        )

        val saved = deviceTokenRepository.save(deviceToken)

        assertThat(saved.token).isEqualTo("test-token")
    }

    @Test
    fun `findByUserIdAndDeviceType`() {
        val userId = UUID.randomUUID()
        deviceTokenRepository.save(
            DeviceToken.register(
                DeviceTokenRegisterRequest(
                    userId = userId,
                    token = "test-token",
                    deviceType = DeviceType.IOS,
                )
            )
        )

        val found = deviceTokenRepository.findByUserIdAndDeviceType(userId, DeviceType.IOS)

        assertThat(found).isNotNull
        assertThat(found?.token).isEqualTo("test-token")
    }

    @Test
    fun `deleteByUserIdAndDeviceType`() {
        val userId = UUID.randomUUID()
        deviceTokenRepository.save(
            DeviceToken.register(
                DeviceTokenRegisterRequest(
                    userId = userId,
                    token = "test-token",
                    deviceType = DeviceType.IOS,
                )
            )
        )

        deviceTokenRepository.deleteByUserIdAndDeviceType(userId, DeviceType.IOS)

        val found = deviceTokenRepository.findByUserIdAndDeviceType(userId, DeviceType.IOS)
        assertThat(found).isNull()
    }
}