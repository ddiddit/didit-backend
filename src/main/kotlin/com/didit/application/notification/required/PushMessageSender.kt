package com.didit.application.notification.required

import com.didit.domain.notification.DeviceType

interface PushMessageSender {
    fun sendMessage(
        token: String,
        title: String,
        body: String,
        deviceType: DeviceType,
        link: String,
    ): Boolean
}
