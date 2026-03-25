package com.didit.application.auth.exception

import com.didit.application.common.exception.BusinessException

class OAuthUserInfoFailedException : BusinessException(AuthErrorCode.OAUTH_USER_INFO_FAILED)
