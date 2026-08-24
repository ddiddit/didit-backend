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
    INVALID_SOCIAL_CREDENTIAL_TYPE(HttpStatus.BAD_REQUEST, "소셜 로그인 인증 방식이 올바르지 않습니다."),
    ACCOUNT_VERIFICATION_REQUIRED(HttpStatus.CONFLICT, "계정 보호를 위해 이메일 인증이 필요합니다."),
    SOCIAL_LOGIN_SESSION_INVALID(HttpStatus.UNAUTHORIZED, "소셜 로그인 세션이 유효하지 않습니다."),
    SOCIAL_LOGIN_SESSION_EXPIRED(HttpStatus.UNAUTHORIZED, "소셜 로그인 세션이 만료되었습니다."),
    EMAIL_VERIFICATION_INVALID(HttpStatus.UNAUTHORIZED, "이메일 인증번호가 올바르지 않습니다."),
    EMAIL_VERIFICATION_EXPIRED(HttpStatus.UNAUTHORIZED, "이메일 인증번호가 만료되었습니다."),
    EMAIL_VERIFICATION_ATTEMPTS_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS, "이메일 인증 시도 횟수를 초과했습니다."),
    EMAIL_VERIFICATION_RESEND_TOO_SOON(HttpStatus.TOO_MANY_REQUESTS, "잠시 후 인증번호를 다시 요청해주세요."),
    EMAIL_REQUIRED(HttpStatus.BAD_REQUEST, "이메일 주소가 필요합니다."),

    DUPLICATE_NICKNAME(HttpStatus.CONFLICT, "이미 사용 중인 닉네임입니다."),

    OAUTH_USER_INFO_FAILED(HttpStatus.BAD_GATEWAY, "소셜 로그인 사용자 정보 조회에 실패했습니다."),
}
