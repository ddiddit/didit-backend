package com.didit.application.auth.exception

import com.didit.application.common.exception.BusinessException
import java.util.UUID

class UserConsentNotFoundException(
    userId: UUID,
) : BusinessException(
        AuthErrorCode.USER_CONSENT_NOT_FOUND,
        "userId: $userId",
    )
