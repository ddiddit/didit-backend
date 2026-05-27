package com.didit.domain.notification

import java.util.UUID

data class AdminNoticeEmailSendRequest(
    val adminId: UUID,
    val targetType: AdminNoticeEmailTargetType,
    val userIds: List<UUID>,
    val subject: String,
    val body: String,
) {
    init {
        require(subject.isNotBlank()) { "제목은 비어 있을 수 없습니다." }
        require(body.isNotBlank()) { "본문은 비어 있을 수 없습니다." }
        when (targetType) {
            AdminNoticeEmailTargetType.ALL ->
                require(userIds.isEmpty()) { "전체 발송에서 사용자 ID 목록을 지정할 수 없습니다." }

            AdminNoticeEmailTargetType.SELECTED_USERS ->
                require(userIds.isNotEmpty()) { "선택 발송에서는 사용자 ID 목록이 필요합니다." }
        }
    }
}
