package com.didit.application.notification.provided

import java.util.UUID

interface UserPushSender {
    fun sendToUser(
        userId: UUID,
        title: String,
        body: String,
        link: String,
    )
}
