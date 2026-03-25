package com.didit.application.auth.exception

import com.didit.application.common.exception.BusinessException

class UnsupportedOAuthProviderException : BusinessException(AuthErrorCode.UNSUPPORTED_OAUTH_PROVIDER)
