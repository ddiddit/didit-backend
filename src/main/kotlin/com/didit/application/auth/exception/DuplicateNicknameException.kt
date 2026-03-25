package com.didit.application.auth.exception

import com.didit.application.common.exception.BusinessException

class DuplicateNicknameException : BusinessException(AuthErrorCode.DUPLICATE_NICKNAME)
