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
