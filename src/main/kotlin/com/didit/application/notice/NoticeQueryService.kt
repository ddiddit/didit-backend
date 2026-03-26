package com.didit.application.notice

import com.didit.application.notice.exception.NoticeNotFoundException
import com.didit.application.notice.provided.NoticeFinder
import com.didit.application.notice.required.NoticeRepository
import com.didit.domain.notice.Notice
import com.didit.domain.notice.NoticeStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Transactional(readOnly = true)
@Service
class NoticeQueryService(
    private val noticeRepository: NoticeRepository,
) : NoticeFinder {
    override fun findAll(): List<Notice> = noticeRepository.findAllByStatusAndDeletedAtIsNullOrderByCreatedAtDesc(NoticeStatus.PUBLISHED)

    override fun findById(noticeId: UUID): Notice {
        val notice =
            noticeRepository.findByIdAndStatusAndDeletedAtIsNull(
                noticeId,
                NoticeStatus.PUBLISHED,
            ) ?: throw NoticeNotFoundException(noticeId)

        return notice
    }
}
