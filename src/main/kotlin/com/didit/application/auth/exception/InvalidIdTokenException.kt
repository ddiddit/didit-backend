package com.didit.application.auth.exception

import com.didit.application.common.exception.BusinessException

class InvalidIdTokenException : BusinessException(AuthErrorCode.INVALID_ID_TOKEN)
