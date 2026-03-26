package com.didit.adapter.webapi.notice.dto

import com.didit.domain.notice.Notice
import java.util.UUID

data class NoticeListResponse(
    val id: UUID,
    val title: String,
) {
    companion object {
        fun from(notice: Notice): NoticeListResponse =
            NoticeListResponse(
                id = notice.id,
                title = notice.title,
            )
    }
}
