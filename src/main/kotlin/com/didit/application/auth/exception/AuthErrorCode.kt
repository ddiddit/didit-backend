package com.didit.application.auth.exception

import com.didit.application.common.exception.BaseErrorCode
import org.springframework.http.HttpStatus

enum class AuthErrorCode(
    override val status: HttpStatus,
    override val detail: String,
) : BaseErrorCode {
    INVALID_ID_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 ID 토큰입니다."),
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 리프레시 토큰입니다."),
    EXPIRED_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "리프레시 토큰이 만료되었습니다."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."),

    KAKAO_TOKEN_REQUEST_FAILED(HttpStatus.BAD_GATEWAY, "카카오 토큰 요청에 실패했습니다."),
    KAKAO_ID_TOKEN_NOT_FOUND(HttpStatus.BAD_REQUEST, "id_token이 없습니다. scope=openid를 확인하세요."),
    KAKAO_INVALID_ID_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 카카오 ID 토큰입니다."),
}
