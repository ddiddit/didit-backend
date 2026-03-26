package com.didit.application.notice.exception

import com.didit.application.common.exception.BusinessException
import java.util.UUID

class NoticeNotFoundException(
    noticeId: UUID,
) : BusinessException(
        NoticeErrorCode.NOTICE_NOT_FOUND,
        "noticeId: $noticeId",
    )
