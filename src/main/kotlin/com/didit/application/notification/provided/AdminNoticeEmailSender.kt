package com.didit.application.notification.provided

import com.didit.domain.notification.AdminNoticeEmailSendRequest

interface AdminNoticeEmailSender {
    fun send(request: AdminNoticeEmailSendRequest)
}
