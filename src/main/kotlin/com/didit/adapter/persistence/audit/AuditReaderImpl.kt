package com.didit.adapter.persistence.audit

import com.didit.application.audit.AuditAction
import com.didit.application.audit.AuditEntry
import com.didit.application.audit.AuditReader
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
}
