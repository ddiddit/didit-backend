package com.didit.adapter.integration.fcm

import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingException
import com.google.firebase.messaging.Message
import com.google.firebase.messaging.MessagingErrorCode
import com.google.firebase.messaging.Notification
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class FcmClient {
    companion object {
        private val logger = LoggerFactory.getLogger(FcmClient::class.java)
    }

    fun sendMessage(
        token: String,
        title: String,
        body: String,
    ): Boolean {
        val message =
            Message
                .builder()
                .setToken(token)
                .setNotification(
                    Notification
                        .builder()
                        .setTitle(title)
                        .setBody(body)
                        .build(),
                ).build()

        return try {
            FirebaseMessaging.getInstance().send(message)
            false
        } catch (e: FirebaseMessagingException) {
            when (e.messagingErrorCode) {
                MessagingErrorCode.UNREGISTERED -> {
                    logger.warn("만료된 FCM 토큰 - token: ${token.take(20)}...")
                    true
                }
                else -> {
                    logger.error("FCM 전송 실패 - errorCode: ${e.messagingErrorCode}, message: ${e.message}", e)
                    throw e
                }
            }
        }
    }
}
