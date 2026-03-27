package com.didit.application.notice.provided

import com.didit.domain.notice.Notice
import java.util.UUID

interface NoticeFinder {
    fun findAll(): List<Notice>

    fun findById(noticeId: UUID): Notice

    fun findAllForAdmin(): List<Notice>

    fun findByIdForAdmin(noticeId: UUID): Notice
}
