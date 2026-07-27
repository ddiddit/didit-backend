package com.didit.application.notification.provided

import com.didit.domain.notification.AdminNoticePushSendRequest

interface AdminNoticePushSender {
    fun send(request: AdminNoticePushSendRequest)
}
