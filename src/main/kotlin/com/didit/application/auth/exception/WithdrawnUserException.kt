package com.didit.application.auth.exception

import com.didit.application.common.exception.BusinessException

class WithdrawnUserException : BusinessException(AuthErrorCode.WITHDRAWN_USER)
