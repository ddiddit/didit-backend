package com.didit.application.auth.exception

import com.didit.application.common.exception.BusinessException

class KakaoIdTokenNotFound :
    BusinessException(
        AuthErrorCode.KAKAO_ID_TOKEN_NOT_FOUND,
    )
