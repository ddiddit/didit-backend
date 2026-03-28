package com.didit.adapter.webapi.notice.dto

import com.didit.domain.notice.Notice
import com.didit.domain.notice.NoticeStatus
import java.time.LocalDateTime
import java.util.UUID

data class NoticeAdminDetailResponse(
    val id: UUID,
    val title: String,
    val content: String,
    val status: NoticeStatus,
    val sendPush: Boolean,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime?,
) {
    companion object {
        fun of(notice: Notice): NoticeAdminDetailResponse =
            NoticeAdminDetailResponse(
                id = notice.id,
                title = notice.title,
                content = notice.content,
                status = notice.status,
                sendPush = notice.sendPush,
                createdAt = notice.createdAt!!,
                updatedAt = notice.updatedAt,
            )
    }
}
