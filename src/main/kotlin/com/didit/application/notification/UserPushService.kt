package com.didit.application.notification

import com.didit.application.notification.provided.DeviceTokenFinder
import com.didit.application.notification.provided.UserPushSender
import com.didit.application.notification.required.DeviceTokenRepository
import com.didit.application.notification.required.PushMessageSender
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class UserPushService(
    private val deviceTokenFinder: DeviceTokenFinder,
    private val deviceTokenRepository: DeviceTokenRepository,
    private val pushMessageSender: PushMessageSender,
) : UserPushSender {
    companion object {
        private val logger = LoggerFactory.getLogger(UserPushService::class.java)
    }

    @Transactional
    override fun sendToUser(
        userId: UUID,
        title: String,
        body: String,
        link: String,
    ) {
        val tokens = deviceTokenFinder.findAllByUserId(userId)
        if (tokens.isEmpty()) return

        val successCount =
            tokens.count { deviceToken ->
                val isExpired =
                    pushMessageSender.sendMessage(
                        token = deviceToken.token,
                        title = title,
                        body = body,
                        deviceType = deviceToken.deviceType,
                        link = link,
                    )

                if (isExpired) {
                    deviceTokenRepository.deleteByToken(deviceToken.token)
                    logger.warn("만료된 FCM 토큰 삭제 - userId: $userId, token: ${deviceToken.token.take(20)}...")
                }

                !isExpired
            }

        if (successCount > 0) {
            logger.info("푸시 전송 성공 - userId: $userId, 성공 토큰 수: $successCount")
        }
    }
}
