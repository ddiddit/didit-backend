package com.didit.application.notice.exception

import com.didit.application.common.exception.BaseErrorCode
import org.springframework.http.HttpStatus

enum class NoticeErrorCode(
    override val status: HttpStatus,
    override val detail: String,
) : BaseErrorCode {
    NOTICE_NOT_FOUND(HttpStatus.NOT_FOUND, "공지사항을 찾을 수 없습니다."),
}
