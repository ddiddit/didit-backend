package com.didit.adapter.persistence.audit

import com.didit.application.audit.AdminAuditLogEntry
import com.didit.application.audit.AuditAction
import com.didit.application.audit.AuditEntry
import com.didit.application.audit.AuditPageResult
import com.didit.application.audit.AuditReader
import com.didit.application.audit.ActorType
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import java.util.UUID

@Component
class AuditReaderImpl(
    private val auditLogRepository: AuditLogRepository,
) : AuditReader {
    override fun findLastLogin(actorId: UUID): LocalDateTime? =
        auditLogRepository
            .findTopByActorIdAndActionOrderByCreatedAtDesc(actorId, AuditAction.USER_LOGGED_IN)
            ?.createdAt

    override fun findTimeline(
        actorId: UUID,
        actions: List<AuditAction>,
        limit: Int,
    ): List<AuditEntry> =
        auditLogRepository
            .findTop20ByActorIdAndActionInOrderByCreatedAtDesc(actorId, actions)
            .map { AuditEntry(action = it.action, payload = it.payload, createdAt = it.createdAt) }

    override fun findLastLoginsByUserIds(userIds: List<UUID>): Map<UUID, LocalDateTime> {
        if (userIds.isEmpty()) return emptyMap()
        return auditLogRepository
            .findLastLoginByUserIds(userIds)
            .associate { it.userId to it.lastLoginAt }
    }

    override fun countDau(since: LocalDateTime): Long =
        auditLogRepository.countDistinctActorSince(
            action = AuditAction.USER_LOGGED_IN,
            since = since,
            actorType = ActorType.USER,
        )

    override fun findAuditLogs(
        action: AuditAction?,
        actorType: String?,
        page: Int,
        size: Int,
    ): AuditPageResult {
        val resolvedActorType = actorType?.let { runCatching { ActorType.valueOf(it) }.getOrNull() }
        val pageable = PageRequest.of(page, size)
        val result = auditLogRepository.findFiltered(action, resolvedActorType, pageable)
        return AuditPageResult(
            content = result.content.map {
                AdminAuditLogEntry(
                    action = it.action,
                    actorId = it.actorId,
                    actorType = it.actorType.name,
                    targetId = it.targetId,
                    targetType = it.targetType,
                    payload = it.payload,
                    createdAt = it.createdAt,
                )
            },
            totalElements = result.totalElements,
            totalPages = result.totalPages,
            page = page,
        )
    }
}
