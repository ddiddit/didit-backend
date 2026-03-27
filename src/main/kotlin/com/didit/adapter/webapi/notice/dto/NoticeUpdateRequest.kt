package com.didit.adapter.webapi.notice.dto

import com.didit.domain.notice.NoticeStatus

data class NoticeUpdateRequest(
    val title: String,
    val content: String,
    val status: NoticeStatus,
    val sendPush: Boolean,
)
