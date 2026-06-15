package com.didit.adapter.webapi.admin.dto

import com.didit.application.admin.provided.UserDetailResult
import com.didit.application.audit.AuditAction
import com.didit.application.audit.AuditEntry
import java.time.LocalDateTime

data class UserDetailResponse(
    val profile: UserListResponse,
    val timeline: List<ActivityLogResponse>,
) {
    companion object {
        fun from(result: UserDetailResult) =
            UserDetailResponse(
                profile = UserListResponse.from(result.profile),
                timeline = result.timeline.map { ActivityLogResponse.from(it) },
            )
    }
}

data class ActivityLogResponse(
    val action: AuditAction,
    val payload: Map<String, Any>?,
    val createdAt: LocalDateTime,
) {
    companion object {
        fun from(entry: AuditEntry) =
            ActivityLogResponse(
                action = entry.action,
                payload = entry.payload,
                createdAt = entry.createdAt,
            )
    }
}
