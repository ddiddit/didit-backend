package com.didit.application.auth.exception

import com.didit.application.common.exception.BusinessException
import java.util.UUID

class UserNotFoundException(
    userId: UUID,
) : BusinessException(
        AuthErrorCode.USER_NOT_FOUND,
        "userId: $userId",
    )

class UserConsentNotFoundException(
    userId: UUID,
) : BusinessException(
        AuthErrorCode.USER_CONSENT_NOT_FOUND,
        "userId: $userId",
    )

class WithdrawnUserException : BusinessException(AuthErrorCode.WITHDRAWN_USER)

class InvalidRefreshTokenException : BusinessException(AuthErrorCode.INVALID_REFRESH_TOKEN)

class ExpiredRefreshTokenException : BusinessException(AuthErrorCode.EXPIRED_REFRESH_TOKEN)

class UnsupportedOAuthProviderException : BusinessException(AuthErrorCode.UNSUPPORTED_OAUTH_PROVIDER)

class DuplicateNicknameException : BusinessException(AuthErrorCode.DUPLICATE_NICKNAME)

class OAuthUserInfoFailedException : BusinessException(AuthErrorCode.OAUTH_USER_INFO_FAILED)

class InvalidSocialCredentialTypeException : BusinessException(AuthErrorCode.INVALID_SOCIAL_CREDENTIAL_TYPE)

class AccountVerificationRequiredException : BusinessException(AuthErrorCode.ACCOUNT_VERIFICATION_REQUIRED)

class InvalidSocialLoginSessionException : BusinessException(AuthErrorCode.SOCIAL_LOGIN_SESSION_INVALID)

class ExpiredSocialLoginSessionException : BusinessException(AuthErrorCode.SOCIAL_LOGIN_SESSION_EXPIRED)

class InvalidEmailVerificationException : BusinessException(AuthErrorCode.EMAIL_VERIFICATION_INVALID)

class ExpiredEmailVerificationException : BusinessException(AuthErrorCode.EMAIL_VERIFICATION_EXPIRED)

class EmailVerificationAttemptsExceededException : BusinessException(AuthErrorCode.EMAIL_VERIFICATION_ATTEMPTS_EXCEEDED)

class EmailVerificationResendTooSoonException : BusinessException(AuthErrorCode.EMAIL_VERIFICATION_RESEND_TOO_SOON)

class EmailRequiredException : BusinessException(AuthErrorCode.EMAIL_REQUIRED)
