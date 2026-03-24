package com.didit.application.users.exception

import com.didit.application.common.exception.BusinessException
import java.util.UUID

class UserNotFoundException(
    userId: UUID,
) : BusinessException(
        UserErrorCode.USER_NOT_FOUND,
        "userId=$userId",
    )
