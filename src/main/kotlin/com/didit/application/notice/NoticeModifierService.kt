package com.didit.application.notice

import com.didit.application.notice.exception.NoticeForbiddenException
import com.didit.application.notice.exception.NoticeNotFoundException
import com.didit.application.notice.provided.NoticeModifier
import com.didit.application.notice.required.NoticeRepository
import com.didit.domain.notice.Notice
import com.didit.domain.notice.NoticeRegisterRequest
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Transactional(readOnly = true)
@Service
class NoticeModifierService(
    private val noticeRepository: NoticeRepository,
) : NoticeModifier {
    companion object {
        private val logger = LoggerFactory.getLogger(NoticeModifierService::class.java)
    }

    @Transactional
    override fun modify(
        request: NoticeRegisterRequest,
        noticeId: UUID,
        adminId: UUID,
    ): Notice {
        val notice =
            noticeRepository.findByIdAndDeletedAtIsNull(noticeId)
                ?: throw NoticeNotFoundException(noticeId)

        validateAdmin(notice, adminId)

        notice.update(request)

        logger.info("공지사항 수정 - noticeId: $noticeId, adminId: $adminId")

        return notice
    }

    @Transactional
    override fun delete(
        noticeId: UUID,
        adminId: UUID,
    ) {
        val notice =
            noticeRepository.findByIdAndDeletedAtIsNull(noticeId)
                ?: throw NoticeNotFoundException(noticeId)

        validateAdmin(notice, adminId)

        notice.delete()

        logger.info("공지사항 삭제 - noticeId: $noticeId, adminId: $adminId")
    }

    private fun validateAdmin(
        notice: Notice,
        adminId: UUID,
    ) {
        if (notice.adminId != adminId) throw NoticeForbiddenException()
    }
}
