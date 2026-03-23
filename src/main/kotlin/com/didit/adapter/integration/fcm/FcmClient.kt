package com.didit.adapter.integration.fcm

import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.Message
import com.google.firebase.messaging.Notification
import org.springframework.stereotype.Component

@Component
class FcmClient {
    fun sendMessage(
        token: String,
        title: String,
        body: String,
    ) {
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

        FirebaseMessaging.getInstance().send(message)
    }
}
