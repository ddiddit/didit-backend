package com.didit.application.notice.exception

import com.didit.application.common.exception.BusinessException

class NoticeForbiddenException :
    BusinessException(
        NoticeErrorCode.NOTICE_FORBIDDEN,
    )
