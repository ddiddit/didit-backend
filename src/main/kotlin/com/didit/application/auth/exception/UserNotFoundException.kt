package com.didit.application.auth.exception

import com.didit.application.common.exception.BusinessException
import java.util.UUID

class UserNotFoundException(
    userId: UUID,
) : BusinessException(
        AuthErrorCode.USER_NOT_FOUND,
        "userId: $userId",
    )
