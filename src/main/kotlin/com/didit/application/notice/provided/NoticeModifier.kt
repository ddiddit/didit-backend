package com.didit.application.notice.provided

import com.didit.domain.notice.Notice
import com.didit.domain.notice.NoticeModifyRequest
import java.util.UUID

interface NoticeModifier {
    fun modify(
        request: NoticeModifyRequest,
        adminId: UUID,
    ): Notice

    fun delete(
        noticeId: UUID,
        adminId: UUID,
    )
}
