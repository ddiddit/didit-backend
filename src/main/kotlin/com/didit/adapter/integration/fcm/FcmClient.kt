package com.didit.adapter.integration.fcm

import com.didit.application.notification.required.PushMessageSender
import com.didit.domain.notification.DeviceType
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingException
import com.google.firebase.messaging.Message
import com.google.firebase.messaging.MessagingErrorCode
import com.google.firebase.messaging.Notification
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class FcmClient : PushMessageSender {
    companion object {
        private val logger = LoggerFactory.getLogger(FcmClient::class.java)
    }

    override fun sendMessage(
        token: String,
        title: String,
        body: String,
        deviceType: DeviceType,
        link: String,
    ): Boolean {
        val message = buildMessage(token, title, body, deviceType, link)

        return try {
            FirebaseMessaging.getInstance().send(message)
            false
        } catch (e: FirebaseMessagingException) {
            when (e.messagingErrorCode) {
                MessagingErrorCode.UNREGISTERED -> {
                    logger.warn("만료된 FCM 토큰 삭제 대상 - token: ${token.take(20)}...")
                    true
                }
                MessagingErrorCode.UNAVAILABLE,
                MessagingErrorCode.QUOTA_EXCEEDED,
                MessagingErrorCode.INTERNAL,
                -> {
                    logger.warn("FCM 일시적 오류 - 스킵 - errorCode: ${e.messagingErrorCode}, message: ${e.message}")
                    false
                }
                else -> {
                    logger.error("FCM 전송 실패 - 스킵 - errorCode: ${e.messagingErrorCode}, message: ${e.message}", e)
                    false
                }
            }
        }
    }

    private fun buildMessage(
        token: String,
        title: String,
        body: String,
        deviceType: DeviceType,
        link: String,
    ): Message {
        val builder =
            Message
                .builder()
                .setToken(token)
                .putData("link", link)

        return when (deviceType) {
            DeviceType.WEB ->
                builder
                    .putData("title", title)
                    .putData("body", body)
                    .build()
            else ->
                builder
                    .setNotification(
                        Notification
                            .builder()
                            .setTitle(title)
                            .setBody(body)
                            .build(),
                    ).build()
        }
    }
}
