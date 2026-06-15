package com.didit.application.audit

import java.time.LocalDateTime
import java.util.UUID

interface AuditReader {
    fun findLastLogin(actorId: UUID): LocalDateTime?

    fun findTimeline(
        actorId: UUID,
        actions: List<AuditAction>,
        limit: Int,
    ): List<AuditEntry>

    fun findLastLoginsByUserIds(userIds: List<UUID>): Map<UUID, LocalDateTime>
}

data class AuditEntry(
    val action: AuditAction,
    val payload: Map<String, Any>?,
    val createdAt: LocalDateTime,
)
