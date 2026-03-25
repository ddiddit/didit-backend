package com.didit.application.notification.exception

import com.didit.application.common.exception.BusinessException
import java.util.UUID

class NotificationHistoryNotFoundException(
    notificationId: UUID,
    userId: UUID,
) : BusinessException(
        NotificationErrorCode.NOTIFICATION_HISTORY_NOT_FOUND,
        "notificationId: $notificationId, userId: $userId",
    )
