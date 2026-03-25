package com.didit.application.auth.exception

import com.didit.application.common.exception.BusinessException

class InvalidRefreshTokenException : BusinessException(AuthErrorCode.INVALID_REFRESH_TOKEN)
