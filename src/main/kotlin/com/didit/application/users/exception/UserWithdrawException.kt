package com.didit.application.users.exception

import com.didit.application.common.exception.BusinessException

class UserWithdrawException :
    BusinessException(
        UserErrorCode.USER_WITHDRAWN,
    )
