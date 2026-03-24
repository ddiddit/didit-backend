package com.didit.application.auth.exception

import com.didit.application.common.exception.BusinessException

class ExpiredRefreshTokenException :
    BusinessException(
        AuthErrorCode.EXPIRED_REFRESH_TOKEN,
    )
