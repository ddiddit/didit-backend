package com.didit.application.auth.exception

import com.didit.application.common.exception.BusinessException

class KakaoInvalidIdTokenException :
    BusinessException(
        AuthErrorCode.KAKAO_INVALID_ID_TOKEN,
    )
