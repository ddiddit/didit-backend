package com.didit.application.auth.exception

import com.didit.application.common.exception.BusinessException

class KakaoTokenRequestFailedException :
    BusinessException(
        AuthErrorCode.KAKAO_TOKEN_REQUEST_FAILED,
    )
