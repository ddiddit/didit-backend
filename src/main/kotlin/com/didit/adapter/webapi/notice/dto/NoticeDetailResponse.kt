package com.didit.adapter.webapi.notice.dto

import com.didit.domain.notice.Notice
import java.time.LocalDateTime
import java.util.UUID

data class NoticeDetailResponse(
    val id: UUID,
    val title: String,
    val content: String,
    val createdAt: LocalDateTime,
) {
    companion object {
        fun of(notice: Notice): NoticeDetailResponse =
            NoticeDetailResponse(
                id = notice.id,
                title = notice.title,
                content = notice.content,
                createdAt = notice.createdAt!!,
            )
    }
}
