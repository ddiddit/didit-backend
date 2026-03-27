package com.didit.domain.notice

import java.util.UUID

data class NoticeModifyRequest(
    val noticeId: UUID,
    val title: String,
    val content: String,
    val status: NoticeStatus,
    val sendPush: Boolean,
)
