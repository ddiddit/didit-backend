package com.didit.domain.notification

import java.util.UUID

data class DeviceTokenRegisterRequest(
    val userId: UUID,
    val token: String,
    val deviceType: DeviceType,
) {
    init {
        require(token.isNotBlank()) { "토큰은 비어있을 수 없습니다." }
    }
}
