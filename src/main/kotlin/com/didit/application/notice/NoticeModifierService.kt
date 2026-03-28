package com.didit.application.notice

import com.didit.application.notice.exception.NoticeForbiddenException
import com.didit.application.notice.exception.NoticeNotFoundException
import com.didit.application.notice.provided.NoticeModifier
import com.didit.application.notice.required.NoticeRepository
import com.didit.domain.notice.Notice
import com.didit.domain.notice.NoticeRegisterRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Transactional(readOnly = true)
@Service
class NoticeModifierService(
    private val noticeRepository: NoticeRepository,
) : NoticeModifier {
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
    }

    private fun validateAdmin(
        notice: Notice,
        adminId: UUID,
    ) {
        if (notice.adminId != adminId) {
            throw NoticeForbiddenException()
        }
    }
}
