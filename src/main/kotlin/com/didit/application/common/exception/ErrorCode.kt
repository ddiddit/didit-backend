package com.didit.application.common.exception

import org.springframework.http.HttpStatus

enum class ErrorCode(
    val status: HttpStatus,
    val detail: String,
) {
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "잘못된 요청입니다."),

    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다."),
}
