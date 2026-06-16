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

    fun countDau(since: LocalDateTime): Long

    fun findAuditLogs(
        action: AuditAction?,
        actorType: String?,
        page: Int,
        size: Int,
    ): AuditPageResult
}

data class AuditEntry(
    val action: AuditAction,
    val payload: Map<String, Any>?,
    val createdAt: LocalDateTime,
)

data class AdminAuditLogEntry(
    val action: AuditAction,
    val actorId: UUID?,
    val actorType: String?,
    val targetId: UUID?,
    val targetType: String?,
    val payload: Map<String, Any>?,
    val createdAt: LocalDateTime,
)

data class AuditPageResult(
    val content: List<AdminAuditLogEntry>,
    val totalElements: Long,
    val totalPages: Int,
    val page: Int,
)
