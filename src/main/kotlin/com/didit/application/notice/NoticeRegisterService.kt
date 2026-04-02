package com.didit.application.notice

import com.didit.application.notice.provided.NoticeRegister
import com.didit.application.notice.required.NoticeRepository
import com.didit.domain.notice.Notice
import com.didit.domain.notice.NoticeRegisterRequest
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Transactional(readOnly = true)
@Service
class NoticeRegisterService(
    private val noticeRepository: NoticeRepository,
) : NoticeRegister {
    companion object {
        private val logger = LoggerFactory.getLogger(NoticeRegisterService::class.java)
    }

    @Transactional
    override fun register(
        request: NoticeRegisterRequest,
        adminId: UUID,
    ): Notice {
        val notice = Notice.register(request, adminId = adminId)

        val saved = noticeRepository.save(notice)

        logger.info("공지사항 등록 - noticeId: ${saved.id}, adminId: $adminId, title: ${request.title}")

        return saved
    }
}
