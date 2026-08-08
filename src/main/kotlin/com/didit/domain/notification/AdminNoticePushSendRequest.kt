package com.didit.domain.notification

import java.util.UUID

data class AdminNoticePushSendRequest(
    val adminId: UUID,
    val targetType: AdminNoticePushTargetType,
    val userIds: List<UUID>,
    val title: String,
    val body: String,
    val link: String,
) {
    init {
        require(title.isNotBlank()) { "제목은 비어 있을 수 없습니다." }
        require(body.isNotBlank()) { "본문은 비어 있을 수 없습니다." }
        require(link.isNotBlank()) { "링크는 비어 있을 수 없습니다." }
        when (targetType) {
            AdminNoticePushTargetType.ALL ->
                require(userIds.isEmpty()) { "전체 발송에서 사용자 ID 목록을 지정할 수 없습니다." }

            AdminNoticePushTargetType.SELECTED_USERS ->
                require(userIds.isNotEmpty()) { "선택 발송에서는 사용자 ID 목록이 필요합니다." }
        }
    }
}
