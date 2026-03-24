package com.didit.application.users.exception

import com.didit.application.common.exception.BaseErrorCode
import org.springframework.http.HttpStatus

enum class UserErrorCode(
    override val status: HttpStatus,
    override val detail: String,
) : BaseErrorCode {
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 유저를 찾을 수 없습니다."),
    USER_WITHDRAWN(HttpStatus.FORBIDDEN, "해당 회원은 탈퇴 회원입니다."),
}
