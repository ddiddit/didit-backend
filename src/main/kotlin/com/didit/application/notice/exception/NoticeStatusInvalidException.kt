package com.didit.application.notice.exception

import com.didit.application.common.exception.BusinessException
import java.util.UUID

class NoticeStatusInvalidException(
    noticeId: UUID,
) : BusinessException(
        NoticeErrorCode.NOTICE_STATUS_INVALID,
        "noticeId: $noticeId",
    )
