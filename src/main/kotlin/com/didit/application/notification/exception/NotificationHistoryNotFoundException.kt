package com.didit.application.notification.exception

import com.didit.application.common.exception.BusinessException
import java.util.UUID

class NotificationHistoryNotFoundException(
    id: UUID,
) : BusinessException(NotificationErrorCode.NOTIFICATION_HISTORY_NOT_FOUND, "id: $id")
