package com.didit.adapter.webapi.notification.dto

import com.didit.domain.notification.AdminNoticeEmailTargetType
import java.util.UUID

data class AdminNoticeEmailSendApiRequest(
    val targetType: AdminNoticeEmailTargetType,
    val userIds: List<UUID>,
    val subject: String,
    val body: String,
)
