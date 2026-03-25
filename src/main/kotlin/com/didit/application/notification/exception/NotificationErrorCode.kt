package com.didit.application.notification.exception

import com.didit.application.common.exception.BaseErrorCode
import org.springframework.http.HttpStatus

enum class NotificationErrorCode(
    override val status: HttpStatus,
    override val detail: String,
) : BaseErrorCode {
    NOTIFICATION_SETTING_NOT_FOUND(HttpStatus.NOT_FOUND, "알림 설정을 찾을 수 없습니다."),
    NOTIFICATION_HISTORY_NOT_FOUND(HttpStatus.NOT_FOUND, "알림을 찾을 수 없습니다."),
}
