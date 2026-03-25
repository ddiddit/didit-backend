package com.didit.application.auth.exception

import com.didit.application.common.exception.BaseErrorCode
import org.springframework.http.HttpStatus

enum class AuthErrorCode(
    override val status: HttpStatus,
    override val detail: String,
) : BaseErrorCode {
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."),
    USER_CONSENT_NOT_FOUND(HttpStatus.NOT_FOUND, "약관 동의 정보를 찾을 수 없습니다."),

    WITHDRAWN_USER(HttpStatus.FORBIDDEN, "탈퇴한 회원입니다."),

    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 리프레시 토큰입니다."),
    EXPIRED_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "만료된 리프레시 토큰입니다."),

    UNSUPPORTED_OAUTH_PROVIDER(HttpStatus.BAD_REQUEST, "지원하지 않는 소셜 로그인입니다."),

    DUPLICATE_NICKNAME(HttpStatus.CONFLICT, "이미 사용 중인 닉네임입니다."),

    OAUTH_USER_INFO_FAILED(HttpStatus.BAD_GATEWAY, "소셜 로그인 사용자 정보 조회에 실패했습니다."),
}
