package com.didit.application.notification

import com.didit.application.notification.provided.DeviceTokenFinder
import com.didit.application.notification.required.DeviceTokenRepository
import com.didit.application.notification.required.PushMessageSender
import com.didit.domain.notification.DeviceToken
import com.didit.domain.notification.DeviceTokenRegisterRequest
import com.didit.domain.notification.DeviceType
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class UserPushServiceTest {
    @Mock lateinit var deviceTokenFinder: DeviceTokenFinder

    @Mock lateinit var deviceTokenRepository: DeviceTokenRepository

    @Mock lateinit var pushMessageSender: PushMessageSender

    @InjectMocks
    lateinit var userPushService: UserPushService

    private fun token(
        userId: UUID,
        value: String,
    ): DeviceToken =
        DeviceToken.register(
            DeviceTokenRegisterRequest(
                userId = userId,
                token = value,
                deviceType = DeviceType.WEB,
            ),
        )

    @Test
    fun `토큰이 있으면 모든 토큰으로 발송한다`() {
        val userId = UUID.randomUUID()
        whenever(deviceTokenFinder.findAllByUserId(userId)).thenReturn(listOf(token(userId, "t1")))
        whenever(pushMessageSender.sendMessage(any(), any(), any(), any(), any())).thenReturn(false)

        userPushService.sendToUser(userId, "제목", "본문", "/")

        verify(pushMessageSender).sendMessage("t1", "제목", "본문", DeviceType.WEB, "/")
        verify(deviceTokenRepository, never()).deleteByToken(any())
    }

    @Test
    fun `토큰이 없으면 발송하지 않는다`() {
        val userId = UUID.randomUUID()
        whenever(deviceTokenFinder.findAllByUserId(userId)).thenReturn(emptyList())

        userPushService.sendToUser(userId, "제목", "본문", "/")

        verify(pushMessageSender, never()).sendMessage(any(), any(), any(), any(), any())
    }

    @Test
    fun `만료된 토큰은 삭제한다`() {
        val userId = UUID.randomUUID()
        whenever(deviceTokenFinder.findAllByUserId(userId)).thenReturn(listOf(token(userId, "expired")))
        whenever(pushMessageSender.sendMessage(any(), any(), any(), any(), any())).thenReturn(true)

        userPushService.sendToUser(userId, "제목", "본문", "/")

        verify(deviceTokenRepository).deleteByToken("expired")
    }
}
