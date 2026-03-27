package com.didit.application.notice.provided

import com.didit.domain.notice.Notice
import com.didit.domain.notice.NoticeRegisterRequest
import java.util.UUID

interface NoticeRegister {
    fun register(
        request: NoticeRegisterRequest,
        adminId: UUID,
    ): Notice
}
