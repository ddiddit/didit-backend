package com.didit.adapter.webapi.notification.dto

import com.didit.domain.notification.AdminNoticePushTargetType
import java.util.UUID

data class AdminNoticePushSendApiRequest(
    val targetType: AdminNoticePushTargetType,
    val userIds: List<UUID>,
    val title: String,
    val body: String,
    val link: String,
)
