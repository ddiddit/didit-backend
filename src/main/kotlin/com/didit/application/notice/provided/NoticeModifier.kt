package com.didit.application.notice.provided

import com.didit.domain.notice.Notice
import com.didit.domain.notice.NoticeRegisterRequest
import java.util.UUID

interface NoticeModifier {
    fun modify(
        request: NoticeRegisterRequest,
        noticeId: UUID,
        adminId: UUID,
    ): Notice

    fun delete(
        noticeId: UUID,
        adminId: UUID,
    )
}
