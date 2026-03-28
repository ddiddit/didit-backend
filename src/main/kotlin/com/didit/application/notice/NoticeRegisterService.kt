package com.didit.application.notice

import com.didit.application.notice.provided.NoticeRegister
import com.didit.application.notice.required.NoticeRepository
import com.didit.domain.notice.Notice
import com.didit.domain.notice.NoticeRegisterRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Transactional(readOnly = true)
@Service
class NoticeRegisterService(
    private val noticeRepository: NoticeRepository,
) : NoticeRegister {
    @Transactional
    override fun register(
        request: NoticeRegisterRequest,
        adminId: UUID,
    ): Notice {
        val notice =
            Notice.register(
                request,
                adminId = adminId,
            )
        return noticeRepository.save(notice)
    }
}
