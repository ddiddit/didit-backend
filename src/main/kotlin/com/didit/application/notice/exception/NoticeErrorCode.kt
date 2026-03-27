package com.didit.application.notice.exception

import com.didit.application.common.exception.BaseErrorCode
import org.springframework.http.HttpStatus

enum class NoticeErrorCode(
    override val status: HttpStatus,
    override val detail: String,
) : BaseErrorCode {
    NOTICE_NOT_FOUND(HttpStatus.NOT_FOUND, "공지사항을 찾을 수 없습니다."),
    NOTICE_STATUS_INVALID(HttpStatus.BAD_REQUEST, "공지사항 상태가 올바르지 않습니다."),
    NOTICE_FORBIDDEN(HttpStatus.FORBIDDEN, "공지사항에 접근할 수 없습니다."),
}
