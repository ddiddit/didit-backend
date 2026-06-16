package com.didit.adapter.persistence.audit

import com.didit.application.audit.ActorType
import com.didit.application.audit.AdminAuditLogEntry
import com.didit.application.audit.AuditAction
import com.didit.application.audit.AuditEntry
import com.didit.application.audit.AuditPageResult
import com.didit.application.audit.AuditReader
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
        val resolvedActorType = resolveActorType(actorType)
        val pageable = PageRequest.of(page, size)
        val result = auditLogRepository.findFiltered(action, resolvedActorType, pageable)
        return AuditPageResult(
            content = result.content.map { it.toEntry() },
            totalElements = result.totalElements,
            totalPages = result.totalPages,
            page = page,
        )
    }

    private fun resolveActorType(actorType: String?): ActorType? =
        actorType?.let {
            ActorType.entries.find { a -> a.name == it }
                ?: throw IllegalArgumentException("유효하지 않은 actorType 값: $it")
        }

    private fun AuditLog.toEntry() =
        AdminAuditLogEntry(
            action = action,
            actorId = actorId,
            actorType = actorType.name,
            targetId = targetId,
            targetType = targetType,
            payload = payload,
            createdAt = createdAt,
        )
}
