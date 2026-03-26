package com.didit.application.admin.exception

import com.didit.application.common.exception.BaseErrorCode
import org.springframework.http.HttpStatus

enum class AdminErrorCode(
    override val status: HttpStatus,
    override val detail: String,
) : BaseErrorCode {
    ADMIN_NOT_FOUND(HttpStatus.NOT_FOUND, "관리자를 찾을 수 없습니다."),

    ADMIN_NOT_ACTIVE(HttpStatus.FORBIDDEN, "활성화되지 않은 관리자입니다."),

    ADMIN_INVALID_PASSWORD(HttpStatus.UNAUTHORIZED, "비밀번호가 올바르지 않습니다."),

    INVALID_ADMIN_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 리프레시 토큰입니다."),
    EXPIRED_ADMIN_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "만료된 리프레시 토큰입니다."),

    INVALID_ADMIN_INVITE_TOKEN(HttpStatus.BAD_REQUEST, "유효하지 않은 초대 토큰입니다."),

    DUPLICATE_ADMIN_INVITE(HttpStatus.CONFLICT, "이미 초대된 이메일입니다."),
    DUPLICATE_ADMIN_EMAIL(HttpStatus.CONFLICT, "이미 사용 중인 이메일입니다."),
}
