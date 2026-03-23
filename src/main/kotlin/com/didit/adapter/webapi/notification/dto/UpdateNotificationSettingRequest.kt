package com.didit.adapter.webapi.notification.dto

import java.time.LocalTime

data class UpdateNotificationSettingRequest(
    val enabled: Boolean,
    val reminderTime: LocalTime,
)
