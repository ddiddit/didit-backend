package com.didit.application.notice.required

import com.didit.domain.notice.Notice
import com.didit.domain.notice.NoticeStatus
import org.springframework.data.repository.Repository
import java.util.UUID

interface NoticeRepository : Repository<Notice, UUID> {
    fun findAllByStatusAndDeletedAtIsNullOrderByCreatedAtDesc(status: NoticeStatus): List<Notice>

    fun findByIdAndStatusAndDeletedAtIsNull(
        noticeId: UUID,
        status: NoticeStatus,
    ): Notice?
}
