package com.didit.application.admin.provided

import java.time.LocalDateTime
import java.util.UUID

data class AdminAuditLogsResult(
    val content: List<AdminAuditLogItem>,
    val totalElements: Long,
    val totalPages: Int,
    val page: Int,
)

data class AdminAuditLogItem(
    val action: String,
    val actionLabel: String,
    val actorId: UUID?,
    val actorType: String?,
    val targetId: UUID?,
    val targetType: String?,
    val payload: Map<String, Any>?,
    val createdAt: LocalDateTime,
)

data class AdminAuditActionItem(
    val action: String,
    val label: String,
    val actorType: String,
)
