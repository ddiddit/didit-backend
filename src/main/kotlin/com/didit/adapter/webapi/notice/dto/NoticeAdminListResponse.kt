package com.didit.adapter.webapi.notice.dto

import com.didit.domain.notice.Notice
import com.didit.domain.notice.NoticeStatus
import java.time.LocalDateTime
import java.util.UUID

data class NoticeAdminListResponse(
    val id: UUID,
    val title: String,
    val status: NoticeStatus,
    val sendPush: Boolean,
    val createdAt: LocalDateTime,
) {
    companion object {
        fun from(notice: Notice): NoticeAdminListResponse =
            NoticeAdminListResponse(
                id = notice.id,
                title = notice.title,
                status = notice.status,
                sendPush = notice.sendPush,
                createdAt = notice.createdAt!!,
            )
    }
}
