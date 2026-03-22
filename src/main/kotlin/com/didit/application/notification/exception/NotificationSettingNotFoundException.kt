package com.didit.application.notification.exception

import com.didit.application.common.exception.BusinessException
import java.util.UUID

class NotificationSettingNotFoundException(userId: UUID) :
    BusinessException(NotificationErrorCode.NOTIFICATION_SETTING_NOT_FOUND)