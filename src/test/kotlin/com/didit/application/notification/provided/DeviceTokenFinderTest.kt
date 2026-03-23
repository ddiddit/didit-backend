package com.didit.application.notification.provided

import com.didit.domain.notification.DeviceToken
import com.didit.domain.notification.DeviceTokenRegisterRequest
import com.didit.domain.notification.DeviceType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class DeviceTokenFinderTest {
    @Mock
    lateinit var deviceTokenFinder: DeviceTokenFinder

    @Test
    fun `findAllByUserId`() {
        val userId = UUID.randomUUID()
        val tokens =
            listOf(
                DeviceToken.register(
                    DeviceTokenRegisterRequest(
                        userId = userId,
                        token = "test-token-ios",
                        deviceType = DeviceType.IOS,
                    ),
                ),
                DeviceToken.register(
                    DeviceTokenRegisterRequest(
                        userId = userId,
                        token = "test-token-android",
                        deviceType = DeviceType.ANDROID,
                    ),
                ),
            )
        whenever(deviceTokenFinder.findAllByUserId(userId)).thenReturn(tokens)

        val found = deviceTokenFinder.findAllByUserId(userId)

        verify(deviceTokenFinder).findAllByUserId(userId)
        assertThat(found).hasSize(2)
    }
}
