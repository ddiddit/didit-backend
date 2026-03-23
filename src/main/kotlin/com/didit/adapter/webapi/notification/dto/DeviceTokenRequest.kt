package com.didit.adapter.webapi.notification.dto

import com.didit.domain.notification.DeviceType

data class DeviceTokenRequest(
    val token: String,
    val deviceType: DeviceType,
)
