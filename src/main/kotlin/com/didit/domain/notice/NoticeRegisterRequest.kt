package com.didit.domain.notice

data class NoticeRegisterRequest(
    val title: String,
    val content: String,
    val status: NoticeStatus,
    val sendPush: Boolean,
)
