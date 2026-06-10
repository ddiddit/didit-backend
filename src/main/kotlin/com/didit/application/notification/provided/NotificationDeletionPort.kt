package com.didit.application.notification.provided

import java.util.UUID

interface NotificationDeletionPort {
    fun deleteByUserId(userId: UUID)
}
