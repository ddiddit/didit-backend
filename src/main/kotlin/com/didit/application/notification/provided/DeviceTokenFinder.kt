package com.didit.application.notification.provided

import com.didit.domain.notification.DeviceToken
import java.util.UUID

interface DeviceTokenFinder {
    fun findAllByUserId(userId: UUID): List<DeviceToken>
}
